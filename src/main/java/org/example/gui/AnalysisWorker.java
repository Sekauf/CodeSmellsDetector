package org.example.gui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import org.example.baseline.BaselineThresholds;
import org.example.jdeodorant.ProjectConfig;
import org.example.logging.LoggingConfigurator;
import org.example.orchestrator.AnalysisOrchestrator;
import org.example.orchestrator.ProgressCallback;
import org.example.sonar.SonarConfig;
import org.example.sonar.SonarDockerManager;

/**
 * Runs {@link AnalysisOrchestrator#run} on a background thread and streams
 * log output live to the progress card via {@link MainWindow#appendLog}.
 *
 * <p>When {@code sonarConfig.isDockerEnabled()} is {@code true} the worker
 * checks Docker availability, starts the container, then stops it afterwards
 * if {@code autoStop} is set. The {@link DockerStatusIndicator} is updated
 * throughout so the user can follow the container lifecycle.</p>
 *
 * <p>Cancellation: calling {@code cancel(true)} interrupts the background thread.
 * Blocking calls that respect thread interruption ({@code Process.waitFor},
 * {@code Thread.sleep}) surface as {@link InterruptedException} and abort the
 * pipeline. Partial output files are deleted on cancellation.</p>
 */
public class AnalysisWorker extends SwingWorker<Void, String> {

    private static final int DOCKER_INFO_TIMEOUT_SEC = 10;
    private static final int DOCKER_STOP_TIMEOUT_SEC = 30;

    private final MainWindow             owner;
    private final Path                   projectRoot;
    private final BaselineThresholds     thresholds;
    private final SonarConfig            sonarConfig;
    private final ProjectConfig          jdeodorantConfig;
    private final Path                   outputDir;
    private final DockerStatusIndicator  dockerIndicator;
    private final boolean                autoStop;

    /** Full constructor including Docker status indicator and auto-stop flag. */
    public AnalysisWorker(
            MainWindow owner,
            Path projectRoot,
            BaselineThresholds thresholds,
            SonarConfig sonarConfig,
            ProjectConfig jdeodorantConfig,
            Path outputDir,
            DockerStatusIndicator dockerIndicator,
            boolean autoStop) {
        this.owner            = owner;
        this.projectRoot      = projectRoot;
        this.thresholds       = thresholds;
        this.sonarConfig      = sonarConfig;
        this.jdeodorantConfig = jdeodorantConfig;
        this.outputDir        = outputDir;
        this.dockerIndicator  = dockerIndicator;
        this.autoStop         = autoStop;
    }

    @Override
    protected Void doInBackground() throws Exception {
        if (sonarConfig != null && sonarConfig.isDockerEnabled()) {
            startDockerContainer();
        }

        if (isCancelled()) {
            return null;
        }

        LoggingConfigurator.configure(outputDir, true);
        Logger root = Logger.getLogger("");
        Handler guiHandler = new GuiLogHandler();
        guiHandler.setLevel(Level.INFO);
        root.addHandler(guiHandler);

        ProgressCallback callback = (label, percent) -> {
            if (!isCancelled()) {
                SwingUtilities.invokeLater(() -> owner.updateProgress(label, percent));
            }
        };
        try {
            new AnalysisOrchestrator().run(
                    projectRoot, thresholds, sonarConfig, jdeodorantConfig, outputDir, callback);
        } finally {
            root.removeHandler(guiHandler);
            if (!isCancelled() && sonarConfig != null && sonarConfig.isDockerEnabled() && autoStop) {
                stopDockerContainer();
            }
        }
        return null;
    }

    @Override
    protected void process(List<String> chunks) {
        for (String line : chunks) {
            owner.appendLog(line);
        }
    }

    @Override
    protected void done() {
        try {
            get();
            owner.onAnalysisComplete(outputDir);
        } catch (CancellationException e) {
            cleanupOutputDir();
            owner.onAnalysisCancelled();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (isCancelled() || cause instanceof InterruptedException) {
                cleanupOutputDir();
                owner.onAnalysisCancelled();
            } else {
                owner.onAnalysisFailed(cause);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            cleanupOutputDir();
            owner.onAnalysisCancelled();
        }
    }

    /**
     * Deletes partial output files written during an aborted analysis.
     * Best-effort: errors are silently ignored.
     */
    private void cleanupOutputDir() {
        if (outputDir == null || !Files.exists(outputDir)) {
            return;
        }
        for (String name : List.of("results.csv", "results.json", "labeling_input.csv")) {
            try {
                Files.deleteIfExists(outputDir.resolve(name));
            } catch (IOException ex) {
                // best-effort — do not propagate
            }
        }
    }

    // -------------------------------------------------------------------------
    // Docker lifecycle helpers
    // -------------------------------------------------------------------------

    /**
     * Checks Docker availability, then starts the SonarQube container.
     *
     * @throws IOException          if Docker is not installed or the container does not become healthy
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    private void startDockerContainer() throws IOException, InterruptedException {
        setDockerState(DockerStatusIndicator.State.CHECKING);
        checkDockerPresence();

        if (isCancelled()) {
            return;
        }

        setDockerState(DockerStatusIndicator.State.STARTING);
        boolean ready = new SonarDockerManager().ensureRunning(sonarConfig);
        if (!ready) {
            setDockerState(DockerStatusIndicator.State.ERROR);
            throw new IOException(
                    "SonarQube wurde innerhalb von 120 s nicht bereit.\n"
                    + "Abbruch — bitte Docker Desktop prüfen.");
        }
        setDockerState(DockerStatusIndicator.State.READY);
    }

    /**
     * Executes {@code docker info} to verify Docker is installed and running.
     *
     * @throws IOException          if Docker is absent or not reachable
     * @throws InterruptedException if interrupted while waiting for the process
     */
    private void checkDockerPresence() throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("docker", "info");
        pb.redirectErrorStream(true);
        try {
            Process p = pb.start();
            boolean finished = p.waitFor(DOCKER_INFO_TIMEOUT_SEC, TimeUnit.SECONDS);
            if (!finished) {
                p.destroyForcibly();
                throw new IOException("docker info timed out.");
            }
            if (p.exitValue() != 0) {
                throw new IOException("Docker antwortet nicht (Exit " + p.exitValue() + ").");
            }
        } catch (IOException e) {
            setDockerState(DockerStatusIndicator.State.ERROR);
            throw new IOException(
                    "Docker ist nicht installiert oder nicht erreichbar.\n"
                    + "Bitte Docker Desktop starten und erneut versuchen.", e);
        }
    }

    /**
     * Stops the SonarQube Docker container via {@code docker-compose stop}.
     * Errors are logged as warnings and do not abort the result display.
     */
    private void stopDockerContainer() {
        setDockerState(DockerStatusIndicator.State.STOPPING);
        try {
            ProcessBuilder pb = new ProcessBuilder("docker-compose", "stop", "sonarqube");
            pb.redirectErrorStream(true);
            String composeDir = System.getenv("SONAR_COMPOSE_DIR");
            if (composeDir != null && !composeDir.isBlank()) {
                pb.directory(Path.of(composeDir).toFile());
            }
            Process p = pb.start();
            p.waitFor(DOCKER_STOP_TIMEOUT_SEC, TimeUnit.SECONDS);
        } catch (Exception e) {
            publish("WARNUNG: Docker-Stop fehlgeschlagen: " + e.getMessage());
        }
        setDockerState(DockerStatusIndicator.State.STOPPED);
    }

    /** Null-safe helper to update the indicator from any thread. */
    private void setDockerState(DockerStatusIndicator.State state) {
        if (dockerIndicator != null) {
            dockerIndicator.setState(state);
        }
    }

    // -------------------------------------------------------------------------
    // Log handler
    // -------------------------------------------------------------------------

    /** Forwards log records to {@link SwingWorker#publish} for live display. */
    private class GuiLogHandler extends Handler {
        @Override
        public void publish(LogRecord record) {
            if (isLoggable(record)) {
                AnalysisWorker.this.publish(
                        record.getLevel().getName() + " " + record.getLoggerName()
                                + " - " + record.getMessage());
            }
        }

        @Override
        public void flush() { }

        @Override
        public void close() { }
    }
}
