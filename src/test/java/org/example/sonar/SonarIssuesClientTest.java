package org.example.sonar;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class SonarIssuesClientTest {
    @Test
    public void parsesIssuesAndTotal() throws Exception {
        SonarIssuesClient client = new FixtureClient(
                "sonar/issues_s6539_page1.json",
                "sonar/issues_s6539_page2.json"
        );

        List<SonarIssue> issues = client.searchIssues(
                "http://localhost:9000",
                null,
                "demo",
                "java:S6539",
                1
        );

        Assert.assertEquals(1, issues.size());
        Assert.assertEquals("demo:src/main/java/com/example/mini/one/BigGod.java", issues.get(0).getComponent());
        Assert.assertEquals("java:S6539", issues.get(0).getRule());
        Assert.assertEquals("Monster class detected.", issues.get(0).getMessage());
        Assert.assertEquals(2, ((FixtureClient) client).getCallCount());
    }

    @Test
    public void returnsEmptyForNoIssues() throws Exception {
        SonarIssuesClient client = new FixtureClient(
                "sonar/issues_s6539_page1.json",
                "sonar/issues_s6539_page2.json"
        ) {
            @Override
            String httpGet(String url, String token) throws IOException, InterruptedException {
                return "{\"paging\":{\"total\":0},\"issues\":[]}";
            }
        };

        List<SonarIssue> issues = client.searchIssues(
                "http://localhost:9000",
                null,
                "demo",
                "java:S6539",
                50
        );

        Assert.assertTrue(issues.isEmpty());
    }

    private String readResource(String name) throws IOException {
        Path path = Path.of("src/test/resources").resolve(name);
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static class FixtureClient extends SonarIssuesClient {
        private final String page1;
        private final String page2;
        private int callCount;

        private FixtureClient(String page1, String page2) {
            this.page1 = page1;
            this.page2 = page2;
        }

        @Override
        String httpGet(String url, String token) throws IOException, InterruptedException {
            callCount++;
            if (url.contains("p=1")) {
                return readFixture(page1);
            }
            return readFixture(page2);
        }

        private int getCallCount() {
            return callCount;
        }

        private String readFixture(String name) throws IOException {
            Path path = Path.of("src/test/resources").resolve(name);
            return Files.readString(path, StandardCharsets.UTF_8);
        }
    }
}
