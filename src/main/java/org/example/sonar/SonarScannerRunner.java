package org.example.sonar;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SonarScannerRunner {
    private static final Logger LOGGER = Logger.getLogger(SonarScannerRunner.class.getName());
    private static final String DEFAULT_EXCLUSIONS = "**/test/**,**/generated/**,**/target/**,**/build/**";
    private static final Pattern CE_TASK_ID_PATTERN = Pattern.compile("ceTaskId=([A-Za-z0-9_-]+)");
    private static final Pattern CE_TASK_URL_PATTERN =
            Pattern.compile("ceTaskUrl=([^\\s]+)", Pattern.CASE_INSENSITIVE);
    private static final String DEFAULT_MAVEN_GOAL =
            "org.sonarsource.scanner.maven:sonar-maven-plugin:3.11.0.3922:sonar";

    public SonarScanResult runScan(Path projectRoot, String projectKey, String sonarUrl, String token)
            throws IOException, InterruptedException {
        Objects.requireNonNull(projectRoot, "projectRoot");
        if (projectKey == null || projectKey.isBlank()) {
            throw new IllegalArgumentException("projectKey must be set");
        }
        if (sonarUrl == null || sonarUrl.isBlank()) {
            throw new IllegalArgumentException("sonarUrl must be set");
        }

        Path propertiesFile = writeTempProperties(projectRoot, projectKey, sonarUrl, token, DEFAULT_EXCLUSIONS);
        boolean hasPom = Files.exists(projectRoot.resolve("pom.xml"));
        List<String> cmd = buildCommand(projectKey, sonarUrl, token, propertiesFile, hasPom);
        String mode = hasPom ? "MAVEN" : "SONAR_SCANNER";
        LOGGER.info("Starting SonarQube scan using " + mode + " in " + projectRoot);
        CommandResult result = runCommand(cmd, projectRoot, 1800);
        String ceTaskId = parseCeTaskIdFromOutput(result.output);
        LOGGER.info("SonarQube scan finished. exitCode=" + result.exitCode);
        return new SonarScanResult(result.exitCode, result.output, ceTaskId);
    }

    List<String> buildCommand(
            String projectKey,
            String sonarUrl,
            String token,
            Path propertiesFile,
            boolean hasPom
    ) {
        List<String> cmd = new ArrayList<>();
        if (!hasPom) {
            cmd.add("sonar-scanner");
            cmd.add("-Dproject.settings=" + propertiesFile.toAbsolutePath());
        } else {
            cmd.add(resolveMavenCommand());
            cmd.add("-q");
            cmd.add("-DskipTests");
            cmd.add(resolveMavenGoal());
            cmd.add("-Dsonar.projectKey=" + projectKey);
            cmd.add("-Dsonar.host.url=" + sonarUrl);
            if (token != null && !token.isBlank()) {
                cmd.add("-Dsonar.token=" + token);
            }
        }
        return cmd;
    }

    private String resolveMavenCommand() {
        String override = System.getenv("SONAR_MVN_CMD");
        if (override != null && !override.isBlank()) {
            return override;
        }
        String os = System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT);
        if (os.contains("win")) {
            return "mvn.cmd";
        }
        return "mvn";
    }

    private String resolveMavenGoal() {
        String override = System.getenv("SONAR_MAVEN_GOAL");
        if (override != null && !override.isBlank()) {
            return override;
        }
        return DEFAULT_MAVEN_GOAL;
    }

    String buildPropertiesText(
            Path projectRoot,
            String projectKey,
            String sonarUrl,
            String token,
            String exclusions
    ) {
        StringBuilder builder = new StringBuilder();
        builder.append("sonar.projectKey=").append(projectKey).append("\n");
        builder.append("sonar.host.url=").append(sonarUrl).append("\n");
        builder.append("sonar.projectBaseDir=").append(projectRoot.toAbsolutePath()).append("\n");
        builder.append("sonar.sources=.").append("\n");
        builder.append("sonar.exclusions=").append(exclusions).append("\n");
        builder.append("sonar.sourceEncoding=UTF-8").append("\n");
        if (token != null && !token.isBlank()) {
            builder.append("sonar.token=").append(token).append("\n");
        }
        return builder.toString();
    }

    private Path writeTempProperties(
            Path projectRoot,
            String projectKey,
            String sonarUrl,
            String token,
            String exclusions
    ) throws IOException {
        Path tempDir = Files.createTempDirectory("sonar-props-");
        Path propertiesFile = tempDir.resolve("sonar-project.properties");
        String propertiesText = buildPropertiesText(projectRoot, projectKey, sonarUrl, token, exclusions);
        Files.writeString(propertiesFile, propertiesText, StandardCharsets.UTF_8);
        propertiesFile.toFile().deleteOnExit();
        tempDir.toFile().deleteOnExit();
        return propertiesFile;
    }

    private CommandResult runCommand(List<String> cmd, Path workingDir, int timeoutSeconds)
            throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(cmd);
        builder.directory(workingDir.toFile());
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
        if (exit != 0) {
            LOGGER.warning(buffer.toString(java.nio.charset.StandardCharsets.UTF_8));
        }
        return new CommandResult(exit, buffer.toString(StandardCharsets.UTF_8));
    }

    private static class CommandResult {
        private final int exitCode;
        private final String output;

        private CommandResult(int exitCode, String output) {
            this.exitCode = exitCode;
            this.output = output;
        }
    }

    String parseCeTaskIdFromOutput(String output) {
        if (output == null || output.isBlank()) {
            return null;
        }
        Matcher matcher = CE_TASK_ID_PATTERN.matcher(output);
        if (matcher.find()) {
            return matcher.group(1);
        }
        Matcher urlMatcher = CE_TASK_URL_PATTERN.matcher(output);
        if (urlMatcher.find()) {
            String url = urlMatcher.group(1);
            int idIndex = url.indexOf("id=");
            if (idIndex >= 0 && idIndex + 3 < url.length()) {
                String idPart = url.substring(idIndex + 3);
                int ampIndex = idPart.indexOf('&');
                if (ampIndex > 0) {
                    idPart = idPart.substring(0, ampIndex);
                }
                if (!idPart.isBlank()) {
                    return idPart;
                }
            }
        }
        return null;
    }
}
