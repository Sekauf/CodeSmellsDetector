package org.example.sonar;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class SonarDockerManager {
    private static final Logger LOGGER = Logger.getLogger(SonarDockerManager.class.getName());
    // Uses docker-compose from the working directory (or SONAR_COMPOSE_DIR) to start SonarQube.

    public boolean ensureRunning(SonarConfig config) throws IOException, InterruptedException {
        Objects.requireNonNull(config, "config");
        if (!config.isDockerEnabled()) {
            return true;
        }

        if (isHealthy(config.getHostUrl(), config.getHealthcheckTimeout())) {
            LOGGER.info("SonarQube is already healthy; skipping Docker start.");
            return true;
        }

        Path composeDir = resolveComposeDir();
        checkComposeAvailable(composeDir);
        startCompose(composeDir);

        return waitForHealthy(config.getHostUrl(), config.getHealthcheckTimeout());
    }

    public boolean waitForHealthy(String hostUrl, Duration timeout) throws IOException, InterruptedException {
        Instant start = Instant.now();
        Duration checkTimeout = timeout == null ? Duration.ofMinutes(2) : timeout;
        while (Duration.between(start, Instant.now()).compareTo(checkTimeout) < 0) {
            if (isHealthy(hostUrl, Duration.ofSeconds(5))) {
                LOGGER.info("SonarQube health check passed.");
                return true;
            }
            Thread.sleep(2000);
        }
        LOGGER.warning("SonarQube health check timed out.");
        return false;
    }

    public boolean isHealthy(String hostUrl, Duration timeout) throws IOException, InterruptedException {
        if (hostUrl == null || hostUrl.isBlank()) {
            return false;
        }
        String url = hostUrl.endsWith("/") ? hostUrl + "api/system/status" : hostUrl + "/api/system/status";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(timeout == null ? Duration.ofSeconds(5) : timeout)
                .GET()
                .build();
        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            return false;
        }
        String body = response.body().toUpperCase(Locale.ROOT);
        return body.contains("\"STATUS\":\"UP\"") || body.contains("\"STATUS\":\"GREEN\"");
    }

    protected void checkComposeAvailable(Path composeDir) throws IOException, InterruptedException {
        try {
            runCommand(List.of("docker-compose", "version"), composeDir, 20);
        } catch (IOException ex) {
            throw new IOException("Docker Compose is required but not available. "
                    + "Install Docker Desktop or ensure docker-compose is on PATH.", ex);
        }
    }

    protected void startCompose(Path composeDir) throws IOException, InterruptedException {
        LOGGER.info("Starting SonarQube via docker-compose in " + composeDir);
        runCommand(List.of("docker-compose", "up", "-d", "sonarqube"), composeDir, 120);
    }

    private Path resolveComposeDir() {
        String override = System.getenv("SONAR_COMPOSE_DIR");
        if (override != null && !override.isBlank()) {
            return java.nio.file.Path.of(override);
        }
        return java.nio.file.Path.of(System.getProperty("user.dir"));
    }

    protected CommandResult runCommand(List<String> cmd, java.nio.file.Path workingDir, int timeoutSeconds)
            throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(cmd);
        if (workingDir != null) {
            builder.directory(workingDir.toFile());
        }
        builder.redirectErrorStream(true);
        Process process = builder.start();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        process.getInputStream().transferTo(buffer);
        boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new IOException("Command timed out: " + String.join(" ", cmd));
        }
        int exit = process.exitValue();
        String output = buffer.toString(java.nio.charset.StandardCharsets.UTF_8);
        List<String> lines = output.isEmpty()
                ? List.of()
                : List.of(output.split("\\R", -1));
        if (exit != 0) {
            throw new IOException("Command failed (" + exit + "): " + String.join(" ", cmd) + "\n" + output);
        }
        return new CommandResult(exit, lines);
    }

    private static class CommandResult {
        private final int exitCode;
        private final List<String> outputLines;

        private CommandResult(int exitCode, List<String> outputLines) {
            this.exitCode = exitCode;
            this.outputLines = outputLines;
        }
    }
}
