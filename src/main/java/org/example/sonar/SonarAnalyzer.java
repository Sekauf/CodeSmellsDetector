package org.example.sonar;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import org.example.baseline.CandidateDTO;

public class SonarAnalyzer {
    private static final Logger LOGGER = Logger.getLogger(SonarAnalyzer.class.getName());
    private static final String RULE_KEY = "java:S6539";
    private static final int OUTPUT_TAIL_LINES = 30;

    /** Wait time when ceTaskId is unavailable. Package-private to allow override in tests. */
    long fallbackWaitMillis = 15_000L;

    private final SonarDockerManager dockerManager;
    private final SonarScannerRunner scannerRunner;
    private final SonarIssuesClient issuesClient;
    private final SonarS6539Mapper mapper;
    private final SonarHealthClient healthClient;
    private final CeTaskClient ceTaskClient;

    public SonarAnalyzer() {
        this(
                new SonarDockerManager(),
                new SonarScannerRunner(),
                new SonarIssuesClient(),
                new SonarS6539Mapper(),
                new SonarHealthClient(),
                new CeTaskClient()
        );
    }

    public SonarAnalyzer(
            SonarDockerManager dockerManager,
            SonarScannerRunner scannerRunner,
            SonarIssuesClient issuesClient,
            SonarS6539Mapper mapper,
            SonarHealthClient healthClient,
            CeTaskClient ceTaskClient
    ) {
        this.dockerManager = Objects.requireNonNull(dockerManager, "dockerManager");
        this.scannerRunner = Objects.requireNonNull(scannerRunner, "scannerRunner");
        this.issuesClient = Objects.requireNonNull(issuesClient, "issuesClient");
        this.mapper = Objects.requireNonNull(mapper, "mapper");
        this.healthClient = Objects.requireNonNull(healthClient, "healthClient");
        this.ceTaskClient = Objects.requireNonNull(ceTaskClient, "ceTaskClient");
    }

    public SonarAnalyzer(
            SonarScannerRunner scannerRunner,
            SonarIssuesClient issuesClient,
            SonarS6539Mapper mapper,
            SonarHealthClient healthClient
    ) {
        this(new SonarDockerManager(), scannerRunner, issuesClient, mapper, healthClient, new CeTaskClient());
    }

    public List<CandidateDTO> runSonarAndFetchResults(String projectPath) throws IOException, InterruptedException {
        if (projectPath == null || projectPath.isBlank()) {
            throw new IllegalArgumentException("projectPath must be set");
        }
        Path projectRoot = Path.of(projectPath);
        String projectKey = deriveProjectKey(projectRoot);
        SonarConfig config = SonarConfig.fromEnv(projectKey);
        Instant start = Instant.now();
        LOGGER.info("SonarQube run started. projectKey=" + projectKey);

        if (config.isDockerEnabled()) {
            boolean started = dockerManager.ensureRunning(config);
            if (!started) {
                throw new IOException("SonarQube did not become healthy within timeout.");
            }
        } else {
            boolean healthy = healthClient.isHealthy(
                    config.getHostUrl(),
                    config.getToken(),
                    config.getHealthcheckTimeout()
            );
            if (!healthy) {
                throw new IOException("SonarQube is unreachable at " + config.getHostUrl()
                        + ". Start SonarQube or update SONAR_HOST_URL.");
            }
        }

        SonarScanResult scanResult = scannerRunner.runScan(
                projectRoot,
                config.getProjectKey(),
                config.getHostUrl(),
                config.getToken()
        );
        if (scanResult.getExitCode() != 0) {
            throw new IOException("SonarQube scan failed with exit code " + scanResult.getExitCode()
                    + ". Output (tail):\n" + tailLines(scanResult.getOutput(), OUTPUT_TAIL_LINES));
        }
        if (scanResult.getCeTaskIdNullable() != null && !scanResult.getCeTaskIdNullable().isBlank()) {
            ceTaskClient.waitForCompletion(
                    config.getHostUrl(),
                    config.getToken(),
                    scanResult.getCeTaskIdNullable(),
                    60,
                    2000
            );
        } else {
            LOGGER.warning("SonarQube scan output did not include ceTaskId; waiting "
                    + fallbackWaitMillis + "ms for server-side analysis.");
            if (fallbackWaitMillis > 0) {
                Thread.sleep(fallbackWaitMillis);
            }
        }

        List<SonarIssue> issues;
        try {
            issues = issuesClient.searchIssues(
                    config.getHostUrl(),
                    config.getToken(),
                    config.getProjectKey(),
                    RULE_KEY,
                    config.getIssuesPageSize()
            );
        } catch (IOException ex) {
            throw new IOException("SonarQube API call failed for rule " + RULE_KEY + ": " + ex.getMessage(), ex);
        }

        List<CandidateDTO> candidates = mapper.mapIssues(projectRoot, issues, config.getProjectKey());
        Instant end = Instant.now();
        LOGGER.info(String.format(
                "SonarQube run finished in %d ms. Issues=%d Candidates=%d",
                Duration.between(start, end).toMillis(),
                issues.size(),
                candidates.size()
        ));
        return candidates;
    }

    public List<CandidateDTO> analyze(Path projectRoot, SonarConfig config) throws IOException, InterruptedException {
        Objects.requireNonNull(projectRoot, "projectRoot");
        Objects.requireNonNull(config, "config");
        Instant start = Instant.now();
        LOGGER.info("SonarQube analysis started.");

        if (config.isDockerEnabled()) {
            boolean started = dockerManager.ensureRunning(config);
            if (!started) {
                throw new IOException("SonarQube did not become healthy within timeout.");
            }
        } else if (!dockerManager.isHealthy(config.getHostUrl(), config.getHealthcheckTimeout())) {
            LOGGER.warning("SonarQube health check failed; proceeding with scan attempt.");
        }

        SonarScanResult scanResult = scannerRunner.runScan(
                projectRoot,
                config.getProjectKey(),
                config.getHostUrl(),
                config.getToken()
        );
        if (scanResult.getExitCode() != 0) {
            throw new IOException("SonarQube scan failed with exit code " + scanResult.getExitCode()
                    + ". Output:\n" + scanResult.getOutput());
        }
        if (scanResult.getCeTaskIdNullable() != null && !scanResult.getCeTaskIdNullable().isBlank()) {
            ceTaskClient.waitForCompletion(
                    config.getHostUrl(),
                    config.getToken(),
                    scanResult.getCeTaskIdNullable(),
                    60,
                    2000
            );
        } else {
            LOGGER.warning("SonarQube scan output did not include ceTaskId; waiting "
                    + fallbackWaitMillis + "ms for server-side analysis.");
            if (fallbackWaitMillis > 0) {
                Thread.sleep(fallbackWaitMillis);
            }
        }
        List<SonarIssue> issues = issuesClient.fetchIssues(config);
        List<CandidateDTO> candidates = mapper.mapIssues(projectRoot, issues, config.getProjectKey());

        Instant end = Instant.now();
        LOGGER.info(String.format(
                "SonarQube analysis finished in %d ms. Issues=%d Candidates=%d",
                Duration.between(start, end).toMillis(),
                issues.size(),
                candidates.size()
        ));
        return candidates;
    }

    private String deriveProjectKey(Path projectRoot) {
        String name = projectRoot.getFileName() == null ? "project" : projectRoot.getFileName().toString();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '-' || c == '_' || c == '.') {
                builder.append(c);
            } else {
                builder.append('_');
            }
        }
        String key = builder.toString();
        if (key.isBlank()) {
            return "project";
        }
        return key;
    }

    private String tailLines(String output, int maxLines) {
        if (output == null || output.isBlank()) {
            return "";
        }
        String[] lines = output.split("\\R");
        int start = Math.max(0, lines.length - Math.max(1, maxLines));
        StringBuilder builder = new StringBuilder();
        for (int i = start; i < lines.length; i++) {
            builder.append(lines[i]).append("\n");
        }
        return builder.toString().trim();
    }
}
