package org.example.sonar;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Assert;
import org.junit.Test;

public class SonarHealthClientTest {
    @Test
    public void detectsHealthyStatus() throws Exception {
        SonarHealthClient client = new SonarHealthClient();
        String body = readResource("sonar/health_up.json");
        Assert.assertTrue(client.isStatusUp(body));
    }

    @Test
    public void detectsUnhealthyStatus() throws Exception {
        SonarHealthClient client = new SonarHealthClient();
        String body = readResource("sonar/health_red.json");
        Assert.assertFalse(client.isStatusUp(body));
    }

    private String readResource(String name) throws IOException {
        Path path = Path.of("src/test/resources").resolve(name);
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
