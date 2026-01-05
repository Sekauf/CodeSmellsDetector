package org.example.sonar;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import org.example.baseline.CandidateDTO;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

public class SonarIntegrationTest {
    @Test(timeout = 480000)
    public void runsSonarAgainstGoldenProject() throws Exception {
        boolean enabled = "true".equalsIgnoreCase(System.getenv("SONAR_IT_ENABLED"));
        if (!enabled) {
            System.out.println("Skipping Sonar integration test. Set SONAR_IT_ENABLED=true to enable.");
        }
        Assume.assumeTrue(enabled);

        String hostUrl = envOrDefault("SONAR_HOST_URL", "http://localhost:9000");
        String token = System.getenv("SONAR_TOKEN");
        if (token == null || token.isBlank()) {
            System.out.println("Skipping Sonar integration test. Set SONAR_TOKEN for authenticated scans.");
        }
        Assume.assumeTrue(token != null && !token.isBlank());
        boolean useDocker = !"false".equalsIgnoreCase(envOrDefault("SONAR_IT_USE_DOCKER", "true"));

        Path projectRoot = Path.of("src/test/resources/golden/mini-project");
        String projectKey = "golden-mini-project";

        SonarConfig config = SonarConfig.builder()
                .hostUrl(hostUrl)
                .token(token)
                .projectKey(projectKey)
                .dockerEnabled(useDocker)
                .healthcheckTimeout(Duration.ofMinutes(3))
                .build();

        SonarDockerManager dockerManager = new SonarDockerManager();
        if (useDocker) {
            boolean started = dockerManager.ensureRunning(config);
            Assert.assertTrue("SonarQube did not become healthy.", started);
        } else {
            SonarHealthClient healthClient = new SonarHealthClient();
            boolean healthy = healthClient.isHealthy(hostUrl, token, Duration.ofSeconds(30));
            Assert.assertTrue("SonarQube is not reachable at " + hostUrl, healthy);
        }

        SonarScannerRunner runner = new SonarScannerRunner();
        SonarScanResult scanResult = runner.runScan(projectRoot, projectKey, hostUrl, token);
        Assert.assertEquals("Sonar scan failed. Output:\n" + scanResult.getOutput(), 0, scanResult.getExitCode());

        if (scanResult.getCeTaskIdNullable() != null && !scanResult.getCeTaskIdNullable().isBlank()) {
            CeTaskClient ceTaskClient = new CeTaskClient();
            ceTaskClient.waitForCompletion(hostUrl, token, scanResult.getCeTaskIdNullable(), 60, 2000);
        } else {
            System.out.println("No ceTaskId detected; proceeding to fetch issues without wait.");
        }

        SonarIssuesClient issuesClient = new SonarIssuesClient();
        List<SonarIssue> issues = issuesClient.searchIssues(hostUrl, token, projectKey, "java:S6539", 100);
        SonarS6539Mapper mapper = new SonarS6539Mapper();
        List<CandidateDTO> candidates = mapper.mapIssues(projectRoot, issues, projectKey);

        boolean containsExpected = containsCandidate(candidates, "com.example.golden.one.BigGod")
                || containsCandidate(candidates, "com.example.golden.two.CoupledSmall");
        Assert.assertTrue("Expected at least one known class in Sonar results.", containsExpected);
    }

    private boolean containsCandidate(List<CandidateDTO> candidates, String fqn) {
        for (CandidateDTO candidate : candidates) {
            if (candidate != null && fqn.equals(candidate.getFullyQualifiedClassName())) {
                return true;
            }
        }
        return false;
    }

    private String envOrDefault(String key, String fallback) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }
}
