package org.example.sonar;

import org.junit.Assert;
import org.junit.Test;

public class SonarDockerManagerTest {
    @Test
    public void reachableSkipsComposeStart() throws Exception {
        final boolean[] startCalled = new boolean[] { false };
        SonarDockerManager manager = new SonarDockerManager() {

            @Override
            public boolean isHealthy(String hostUrl, java.time.Duration timeout) {
                return true;
            }

            @Override
            protected void checkComposeAvailable(java.nio.file.Path composeDir) {
                Assert.fail("checkComposeAvailable should not be called when already healthy.");
            }

            @Override
            protected void startCompose(java.nio.file.Path composeDir) {
                startCalled[0] = true;
            }
        };

        SonarConfig config = SonarConfig.builder()
                .projectKey("demo")
                .dockerEnabled(true)
                .build();

        boolean started = manager.ensureRunning(config);
        Assert.assertTrue(started);
        Assert.assertFalse(startCalled[0]);
    }
}
