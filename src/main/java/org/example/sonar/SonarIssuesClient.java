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
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

public class SonarIssuesClient {
    private static final Logger LOGGER = Logger.getLogger(SonarIssuesClient.class.getName());
    private static final String RULE_KEY = "java:S6539";
    private static final int DEFAULT_PAGE_SIZE = 100;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public List<SonarIssue> fetchIssues(SonarConfig config) throws IOException, InterruptedException {
        Objects.requireNonNull(config, "config");
        return searchIssues(config.getHostUrl(), config.getToken(), config.getProjectKey(), RULE_KEY,
                config.getIssuesPageSize());
    }

    public List<SonarIssue> searchIssues(String hostUrl, String token, String projectKey)
            throws IOException, InterruptedException {
        return searchIssues(hostUrl, token, projectKey, RULE_KEY, DEFAULT_PAGE_SIZE);
    }

    public List<SonarIssue> searchIssues(String hostUrl, String token, String projectKey, String ruleKey)
            throws IOException, InterruptedException {
        return searchIssues(hostUrl, token, projectKey, ruleKey, DEFAULT_PAGE_SIZE);
    }

    public List<SonarIssue> searchIssues(
            String hostUrl,
            String token,
            String projectKey,
            String ruleKey,
            int pageSize
    ) throws IOException, InterruptedException {
        if (hostUrl == null || hostUrl.isBlank()) {
            throw new IllegalArgumentException("hostUrl must be set");
        }
        if (projectKey == null || projectKey.isBlank()) {
            throw new IllegalArgumentException("projectKey must be set");
        }
        if (ruleKey == null || ruleKey.isBlank()) {
            throw new IllegalArgumentException("ruleKey must be set");
        }

        List<SonarIssue> result = new ArrayList<>();
        int page = 1;
        int resolvedPageSize = pageSize > 0 ? pageSize : DEFAULT_PAGE_SIZE;
        int total = Integer.MAX_VALUE;
        int fetched = 0;

        while (fetched < total) {
            String url = buildSearchUrl(hostUrl, projectKey, ruleKey, page, resolvedPageSize);
            String json = httpGet(url, token);
            List<SonarIssue> issues = parseIssues(json);
            total = parseTotal(json);
            for (SonarIssue issue : issues) {
                if (issue != null && ruleKey.equals(issue.getRule())) {
                    result.add(issue);
                }
            }
            fetched += issues.size();
            if (issues.isEmpty()) {
                break;
            }
            page++;
        }

        LOGGER.info("Fetched SonarQube issues for rule " + ruleKey + ": " + result.size());
        return result;
    }

    String buildSearchUrl(String hostUrl, String projectKey, String ruleKey, int page, int pageSize) {
        String base = hostUrl;
        if (!base.endsWith("/")) {
            base = base + "/";
        }
        String encodedProjectKey = URLEncoder.encode(projectKey, StandardCharsets.UTF_8);
        return base + "api/issues/search"
                + "?componentKeys=" + encodedProjectKey
                + "&rules=" + URLEncoder.encode(ruleKey, StandardCharsets.UTF_8)
                + "&ps=" + pageSize
                + "&p=" + page;
    }

    String httpGet(String url, String token) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .GET();
        if (token != null && !token.isBlank()) {
            builder.header("Authorization", "Basic " + encodeToken(token));
        }
        HttpRequest request = builder.build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            String snippet = response.body() == null ? "" : response.body().trim();
            if (snippet.length() > 200) {
                snippet = snippet.substring(0, 200) + "...";
            }
            throw new IOException("SonarQube issues API returned " + response.statusCode()
                    + " with body: " + snippet);
        }
        return response.body();
    }

    List<SonarIssue> parseIssues(String json) throws IOException {
        List<SonarIssue> issues = new ArrayList<>();
        JsonNode root = objectMapper.readTree(json);
        JsonNode issueNodes = root.path("issues");
        if (issueNodes.isMissingNode() || !issueNodes.isArray()) {
            return issues;
        }
        for (JsonNode issueNode : issueNodes) {
            String component = issueNode.path("component").asText(null);
            String rule = issueNode.path("rule").asText(null);
            String message = issueNode.path("message").asText(null);
            Integer line = null;
            JsonNode textRange = issueNode.path("textRange");
            if (textRange != null && textRange.isObject()) {
                int startLine = textRange.path("startLine").asInt(-1);
                if (startLine > 0) {
                    line = startLine;
                }
            }
            if (component != null && !component.isBlank()) {
                issues.add(new SonarIssue(component, rule, message, line));
            }
        }
        return issues;
    }

    int parseTotal(String json) throws IOException {
        JsonNode root = objectMapper.readTree(json);
        JsonNode paging = root.path("paging");
        return paging.path("total").asInt(0);
    }

    private String encodeToken(String token) {
        return Base64.getEncoder().encodeToString((token + ":").getBytes(StandardCharsets.UTF_8));
    }
}
