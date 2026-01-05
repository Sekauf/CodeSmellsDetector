package org.example.sonar;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Assert;
import org.junit.Test;

public class CeTaskClientTest {
    @Test
    public void waitsUntilSuccess() throws Exception {
        CeTaskClient client = new SequenceClient(
                "sonar/ce_task_in_progress.json",
                "sonar/ce_task_success.json"
        );

        String status = client.waitForCompletion("http://localhost:9000", null, "abc", 5, 0);
        Assert.assertEquals("SUCCESS", status);
        Assert.assertEquals(2, ((SequenceClient) client).getCalls());
    }

    @Test
    public void timesOutAfterMaxAttempts() throws Exception {
        CeTaskClient client = new SequenceClient(
                "sonar/ce_task_in_progress.json",
                "sonar/ce_task_in_progress.json",
                "sonar/ce_task_in_progress.json"
        );

        try {
            client.waitForCompletion("http://localhost:9000", null, "abc", 2, 0);
            Assert.fail("Expected timeout.");
        } catch (IOException ex) {
            Assert.assertTrue(ex.getMessage().contains("did not complete"));
        }
    }

    private static class SequenceClient extends CeTaskClient {
        private final String[] fixtures;
        private int index;
        private int calls;

        private SequenceClient(String... fixtures) {
            this.fixtures = fixtures;
        }

        @Override
        String httpGet(String url, String token) throws IOException, InterruptedException {
            calls++;
            int current = Math.min(index, fixtures.length - 1);
            String name = fixtures[current];
            index++;
            Path path = Path.of("src/test/resources").resolve(name);
            return Files.readString(path, StandardCharsets.UTF_8);
        }

        @Override
        void sleep(long millis) {
            // No-op for tests.
        }

        private int getCalls() {
            return calls;
        }
    }
}
