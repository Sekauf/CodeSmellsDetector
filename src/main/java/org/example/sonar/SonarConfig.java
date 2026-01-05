package org.example.sonar;

import java.time.Duration;

public class SonarConfig {
    public enum ScannerMode {
        MAVEN,
        SONAR_SCANNER
    }

    private final String hostUrl;
    private final String token;
    private final String projectKey;
    private final boolean dockerEnabled;
    private final String dockerImage;
    private final String dockerContainerName;
    private final int dockerPort;
    private final ScannerMode scannerMode;
    private final int issuesPageSize;
    private final Duration healthcheckTimeout;

    private SonarConfig(Builder builder) {
        this.hostUrl = builder.hostUrl;
        this.token = builder.token;
        this.projectKey = builder.projectKey;
        this.dockerEnabled = builder.dockerEnabled;
        this.dockerImage = builder.dockerImage;
        this.dockerContainerName = builder.dockerContainerName;
        this.dockerPort = builder.dockerPort;
        this.scannerMode = builder.scannerMode;
        this.issuesPageSize = builder.issuesPageSize;
        this.healthcheckTimeout = builder.healthcheckTimeout;
    }

    public String getHostUrl() {
        return hostUrl;
    }

    public String getToken() {
        return token;
    }

    public String getProjectKey() {
        return projectKey;
    }

    public boolean isDockerEnabled() {
        return dockerEnabled;
    }

    public String getDockerImage() {
        return dockerImage;
    }

    public String getDockerContainerName() {
        return dockerContainerName;
    }

    public int getDockerPort() {
        return dockerPort;
    }

    public ScannerMode getScannerMode() {
        return scannerMode;
    }

    public int getIssuesPageSize() {
        return issuesPageSize;
    }

    public Duration getHealthcheckTimeout() {
        return healthcheckTimeout;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static SonarConfig fromEnv(String projectKey) {
        String hostUrl = envOrDefault("SONAR_HOST_URL", "http://localhost:9000");
        String token = System.getenv("SONAR_TOKEN");
        boolean dockerEnabled = envFlag("SONAR_DOCKER_ENABLED", false);
        return SonarConfig.builder()
                .hostUrl(hostUrl)
                .token(token)
                .projectKey(projectKey)
                .dockerEnabled(dockerEnabled)
                .build();
    }

    private static String envOrDefault(String key, String fallback) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }

    private static boolean envFlag(String key, boolean fallback) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return "true".equalsIgnoreCase(value)
                || "1".equals(value)
                || "yes".equalsIgnoreCase(value)
                || "y".equalsIgnoreCase(value);
    }

    public static class Builder {
        private String hostUrl = "http://localhost:9000";
        private String token;
        private String projectKey;
        private boolean dockerEnabled;
        private String dockerImage = "sonarqube:2025.4-community";
        private String dockerContainerName = "sonarqube";
        private int dockerPort = 9000;
        private ScannerMode scannerMode = ScannerMode.MAVEN;
        private int issuesPageSize = 100;
        private Duration healthcheckTimeout = Duration.ofMinutes(2);

        public Builder hostUrl(String hostUrl) {
            this.hostUrl = hostUrl;
            return this;
        }

        public Builder token(String token) {
            this.token = token;
            return this;
        }

        public Builder projectKey(String projectKey) {
            this.projectKey = projectKey;
            return this;
        }

        public Builder dockerEnabled(boolean dockerEnabled) {
            this.dockerEnabled = dockerEnabled;
            return this;
        }

        public Builder dockerImage(String dockerImage) {
            this.dockerImage = dockerImage;
            return this;
        }

        public Builder dockerContainerName(String dockerContainerName) {
            this.dockerContainerName = dockerContainerName;
            return this;
        }

        public Builder dockerPort(int dockerPort) {
            this.dockerPort = dockerPort;
            return this;
        }

        public Builder scannerMode(ScannerMode scannerMode) {
            this.scannerMode = scannerMode;
            return this;
        }

        public Builder issuesPageSize(int issuesPageSize) {
            this.issuesPageSize = issuesPageSize;
            return this;
        }

        public Builder healthcheckTimeout(Duration healthcheckTimeout) {
            this.healthcheckTimeout = healthcheckTimeout;
            return this;
        }

        public SonarConfig build() {
            if (hostUrl == null || hostUrl.isBlank()) {
                throw new IllegalArgumentException("hostUrl must be set");
            }
            if (projectKey == null || projectKey.isBlank()) {
                throw new IllegalArgumentException("projectKey must be set");
            }
            return new SonarConfig(this);
        }
    }
}
