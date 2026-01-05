package org.example.sonar;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;
import java.util.Objects;
import java.util.logging.Logger;

public class CeTaskClient {
    private static final Logger LOGGER = Logger.getLogger(CeTaskClient.class.getName());
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_CANCELED = "CANCELED";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String waitForCompletion(
            String hostUrl,
            String token,
            String taskId,
            int maxAttempts,
            long sleepMillis
    ) throws IOException, InterruptedException {
        Objects.requireNonNull(hostUrl, "hostUrl");
        if (taskId == null || taskId.isBlank()) {
            throw new IllegalArgumentException("taskId must be set");
        }
        int attempts = Math.max(1, maxAttempts);
        long delay = Math.max(0, sleepMillis);
        for (int attempt = 1; attempt <= attempts; attempt++) {
            String url = buildTaskUrl(hostUrl, taskId);
            String json = httpGet(url, token);
            String status = parseStatus(json);
            LOGGER.info("SonarQube CE task status: " + status + " (attempt " + attempt + "/" + attempts + ")");
            if (STATUS_SUCCESS.equals(status)) {
                return status;
            }
            if (STATUS_FAILED.equals(status) || STATUS_CANCELED.equals(status)) {
                throw new IOException("SonarQube CE task completed with status " + status + ".");
            }
            if (attempt < attempts && delay > 0) {
                sleep(delay);
            }
        }
        throw new IOException("SonarQube CE task did not complete after " + attempts + " attempts.");
    }

    String buildTaskUrl(String hostUrl, String taskId) {
        String base = hostUrl.endsWith("/") ? hostUrl.substring(0, hostUrl.length() - 1) : hostUrl;
        String encodedId = URLEncoder.encode(taskId, StandardCharsets.UTF_8);
        return base + "/api/ce/task?id=" + encodedId;
    }

    String parseStatus(String json) throws IOException {
        JsonNode root = objectMapper.readTree(json);
        JsonNode task = root.path("task");
        String status = task.path("status").asText("");
        return status.toUpperCase(Locale.ROOT);
    }

    String httpGet(String url, String token) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .GET();
        if (token != null && !token.isBlank()) {
            builder.header("Authorization", "Basic " + encodeToken(token));
        }
        HttpRequest request = builder.build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("SonarQube CE task API returned " + response.statusCode());
        }
        return response.body();
    }

    void sleep(long millis) throws InterruptedException {
        Thread.sleep(millis);
    }

    private String encodeToken(String token) {
        return Base64.getEncoder().encodeToString((token + ":").getBytes(StandardCharsets.UTF_8));
    }
}
