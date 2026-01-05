package org.example.sonar;

import java.nio.file.Path;
import org.junit.Assert;
import org.junit.Test;

public class SonarScannerRunnerTest {
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
}
