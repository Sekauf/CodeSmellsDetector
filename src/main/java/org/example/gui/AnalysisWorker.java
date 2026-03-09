package org.example.gui;

import java.nio.file.Path;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import javax.swing.SwingWorker;
import org.example.baseline.BaselineThresholds;
import org.example.jdeodorant.ProjectConfig;
import org.example.logging.LoggingConfigurator;
import org.example.orchestrator.AnalysisOrchestrator;
import org.example.sonar.SonarConfig;

/**
 * Runs {@link AnalysisOrchestrator#run} on a background thread and streams
 * log output live to the progress card via {@link MainWindow#appendLog}.
 *
 * <p>The GUI handler is added <em>after</em> {@link LoggingConfigurator#configure}
 * because configure() removes all pre-existing handlers first.</p>
 */
public class AnalysisWorker extends SwingWorker<Void, String> {

    private final MainWindow owner;
    private final Path projectRoot;
    private final BaselineThresholds thresholds;
    private final SonarConfig sonarConfig;
    private final ProjectConfig jdeodorantConfig;
    private final Path outputDir;

    public AnalysisWorker(
            MainWindow owner,
            Path projectRoot,
            BaselineThresholds thresholds,
            SonarConfig sonarConfig,
            ProjectConfig jdeodorantConfig,
            Path outputDir) {
        this.owner = owner;
        this.projectRoot = projectRoot;
        this.thresholds = thresholds;
        this.sonarConfig = sonarConfig;
        this.jdeodorantConfig = jdeodorantConfig;
        this.outputDir = outputDir;
    }

    @Override
    protected Void doInBackground() throws Exception {
        // Configure file + console logging first (removes all existing handlers)
        LoggingConfigurator.configure(outputDir, true);

        // Add GUI handler after configure() so it is not removed
        Logger root = Logger.getLogger("");
        Handler guiHandler = new GuiLogHandler();
        guiHandler.setLevel(Level.INFO);
        root.addHandler(guiHandler);

        try {
            new AnalysisOrchestrator().run(projectRoot, thresholds, sonarConfig, jdeodorantConfig, outputDir);
        } finally {
            root.removeHandler(guiHandler);
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
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            owner.onAnalysisFailed(cause);
        }
    }

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
        public void flush() {
        }

        @Override
        public void close() {
        }
    }
}
