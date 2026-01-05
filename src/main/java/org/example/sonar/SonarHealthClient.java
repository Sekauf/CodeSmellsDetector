package org.example.sonar;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class SonarHealthClient {
    private static final Logger LOGGER = Logger.getLogger(SonarHealthClient.class.getName());
    private static final Pattern STATUS_PATTERN = Pattern.compile("\"status\"\\s*:\\s*\"(UP|GREEN)\"",
            Pattern.CASE_INSENSITIVE);

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public boolean isHealthy(String hostUrl, String token, Duration timeout) throws IOException, InterruptedException {
        Objects.requireNonNull(hostUrl, "hostUrl");
        String url = toStatusUrl(hostUrl);
        LOGGER.info("SonarQube health check started: " + url);
        HttpRequest request = buildRequest(url, token, timeout);
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            LOGGER.warning("SonarQube health check failed with status " + response.statusCode());
            return false;
        }
        boolean healthy = isStatusUp(response.body());
        LOGGER.info("SonarQube health check finished. healthy=" + healthy);
        return healthy;
    }

    public void assertHealthy(String hostUrl, String token, Duration timeout) throws IOException, InterruptedException {
        boolean healthy = isHealthy(hostUrl, token, timeout);
        if (!healthy) {
            throw new IOException("SonarQube health check failed. Verify the server is running at " + hostUrl
                    + " and the token is valid if authentication is enabled.");
        }
    }

    String toStatusUrl(String hostUrl) {
        String trimmed = hostUrl.endsWith("/") ? hostUrl.substring(0, hostUrl.length() - 1) : hostUrl;
        return trimmed + "/api/system/status";
    }

    boolean isStatusUp(String body) {
        if (body == null || body.isBlank()) {
            return false;
        }
        String normalized = body.trim().toUpperCase(Locale.ROOT);
        return STATUS_PATTERN.matcher(normalized).find();
    }

    private HttpRequest buildRequest(String url, String token, Duration timeout) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(timeout == null ? Duration.ofSeconds(5) : timeout)
                .GET();
        if (token != null && !token.isBlank()) {
            builder.header("Authorization", "Basic " + encodeToken(token));
        }
        return builder.build();
    }

    private String encodeToken(String token) {
        return Base64.getEncoder().encodeToString((token + ":").getBytes(StandardCharsets.UTF_8));
    }
}
