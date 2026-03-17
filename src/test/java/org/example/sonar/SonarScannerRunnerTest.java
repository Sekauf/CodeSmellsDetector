package org.example.sonar;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class SonarScannerRunnerTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    @Test
    public void buildsPropertiesTextWithToken() {
        SonarScannerRunner runner = new SonarScannerRunner();
        Path projectRoot = Path.of("C:\\tmp\\demo-project");
        String text = runner.buildPropertiesText(
                projectRoot,
                "demo",
                "http://localhost:9000",
                "token-123",
                "**/test/**"
        );

        String expected = ""
                + "sonar.projectKey=demo\n"
                + "sonar.host.url=http://localhost:9000\n"
                + "sonar.projectBaseDir=" + projectRoot.toAbsolutePath() + "\n"
                + "sonar.sources=.\n"
                + "sonar.exclusions=**/test/**\n"
                + "sonar.sourceEncoding=UTF-8\n"
                + "sonar.token=token-123\n";
        Assert.assertEquals(expected, text);
    }

    @Test
    public void parsesCeTaskIdFromOutput() {
        SonarScannerRunner runner = new SonarScannerRunner();
        String output = ""
                + "INFO: More output\n"
                + "INFO: ceTaskUrl=http://localhost:9000/api/ce/task?id=abc123\n"
                + "INFO: Done\n";
        Assert.assertEquals("abc123", runner.parseCeTaskIdFromOutput(output));
    }

    @Test
    public void parsesCeTaskIdFromMavenReportFile() throws IOException {
        Path projectRoot = temporaryFolder.newFolder("sonar-maven").toPath();
        Path reportDir = projectRoot.resolve("target").resolve("sonar");
        Files.createDirectories(reportDir);
        Files.writeString(reportDir.resolve("report-task.txt"),
                "projectKey=demo\n"
                + "serverUrl=http://localhost:9000\n"
                + "ceTaskId=AY_test123\n"
                + "ceTaskUrl=http://localhost:9000/api/ce/task?id=AY_test123\n",
                StandardCharsets.UTF_8);

        SonarScannerRunner runner = new SonarScannerRunner();
        Assert.assertEquals("AY_test123", runner.parseCeTaskIdFromReportFile(projectRoot));
    }

    @Test
    public void parsesCeTaskIdFromScannerworkFile() throws IOException {
        Path projectRoot = temporaryFolder.newFolder("sonar-scannerwork").toPath();
        Path reportDir = projectRoot.resolve(".scannerwork");
        Files.createDirectories(reportDir);
        Files.writeString(reportDir.resolve("report-task.txt"),
                "ceTaskId=BZ_scanner456\n",
                StandardCharsets.UTF_8);

        SonarScannerRunner runner = new SonarScannerRunner();
        Assert.assertEquals("BZ_scanner456", runner.parseCeTaskIdFromReportFile(projectRoot));
    }

    @Test
    public void parsesCeTaskIdFromReportFile_noFileReturnsNull() throws IOException {
        Path projectRoot = temporaryFolder.newFolder("sonar-empty").toPath();

        SonarScannerRunner runner = new SonarScannerRunner();
        Assert.assertNull(runner.parseCeTaskIdFromReportFile(projectRoot));
    }

    @Test
    public void parsesCeTaskIdFromReportFile_noCeTaskIdLineReturnsNull() throws IOException {
        Path projectRoot = temporaryFolder.newFolder("sonar-no-id").toPath();
        Path reportDir = projectRoot.resolve("target").resolve("sonar");
        Files.createDirectories(reportDir);
        Files.writeString(reportDir.resolve("report-task.txt"),
                "projectKey=demo\nserverUrl=http://localhost:9000\n",
                StandardCharsets.UTF_8);

        SonarScannerRunner runner = new SonarScannerRunner();
        Assert.assertNull(runner.parseCeTaskIdFromReportFile(projectRoot));
    }

    @Test
    public void parsesCeTaskIdPrefersMavenOverScannerwork() throws IOException {
        Path projectRoot = temporaryFolder.newFolder("sonar-both").toPath();

        Path mavenDir = projectRoot.resolve("target").resolve("sonar");
        Files.createDirectories(mavenDir);
        Files.writeString(mavenDir.resolve("report-task.txt"), "ceTaskId=MAVEN_ID\n", StandardCharsets.UTF_8);

        Path scannerDir = projectRoot.resolve(".scannerwork");
        Files.createDirectories(scannerDir);
        Files.writeString(scannerDir.resolve("report-task.txt"), "ceTaskId=SCANNER_ID\n", StandardCharsets.UTF_8);

        SonarScannerRunner runner = new SonarScannerRunner();
        Assert.assertEquals("MAVEN_ID", runner.parseCeTaskIdFromReportFile(projectRoot));
    }

    @Test
    public void buildCommandMavenUsesBatchModeNotQuiet() {
        SonarScannerRunner runner = new SonarScannerRunner();
        Path props = Path.of("/tmp/sonar.properties");
        List<String> cmd = runner.buildCommand("demo", "http://localhost:9000", "token", props, true);

        Assert.assertTrue("Maven command should contain -B", cmd.contains("-B"));
        Assert.assertFalse("Maven command should not contain -q", cmd.contains("-q"));
    }
}
