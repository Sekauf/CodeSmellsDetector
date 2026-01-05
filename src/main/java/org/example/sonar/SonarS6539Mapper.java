package org.example.sonar;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import org.example.baseline.CandidateDTO;

public class SonarS6539Mapper {
    private static final Logger LOGGER = Logger.getLogger(SonarS6539Mapper.class.getName());

    public List<CandidateDTO> mapIssues(Path projectRoot, List<SonarIssue> issues, String projectKey) {
        Objects.requireNonNull(projectRoot, "projectRoot");
        if (issues == null || issues.isEmpty()) {
            return List.of();
        }

        Map<String, CandidateDTO> candidates = new HashMap<>();
        int unresolved = 0;

        for (SonarIssue issue : issues) {
            if (issue == null) {
                continue;
            }
            String component = issue.getComponent();
            if (component == null || component.isBlank()) {
                continue;
            }
            String fqn = resolveFqn(component, projectKey);
            if (fqn == null || fqn.isBlank()) {
                unresolved++;
                continue;
            }
            candidates.putIfAbsent(fqn, CandidateDTO.builder(fqn).sonarFlag(true).build());
        }

        List<CandidateDTO> result = new ArrayList<>(candidates.values());
        result.sort(Comparator.comparing(CandidateDTO::getFullyQualifiedClassName));
        LOGGER.info("SonarS6539Mapper mapped=" + result.size()
                + " unresolved=" + unresolved
                + " totalIssues=" + issues.size());
        return result;
    }

    private String resolveFqn(String component, String projectKey) {
        String trimmed = stripProjectKey(component, projectKey);
        String normalized = trimmed.replace("\\", "/");
        int srcIndex = normalized.indexOf("src/main/java/");
        if (srcIndex < 0) {
            return null;
        }
        String pathPart = normalized.substring(srcIndex + "src/main/java/".length());
        if (!pathPart.endsWith(".java")) {
            return null;
        }
        String withoutExt = pathPart.substring(0, pathPart.length() - ".java".length());
        if (withoutExt.isBlank()) {
            return null;
        }
        return withoutExt.replace("/", ".");
    }

    private String stripProjectKey(String component, String projectKey) {
        if (projectKey != null && component.startsWith(projectKey + ":")) {
            return component.substring(projectKey.length() + 1);
        }
        int colonIndex = component.indexOf(':');
        if (colonIndex >= 0 && colonIndex < component.length() - 1) {
            return component.substring(colonIndex + 1);
        }
        return component;
    }

}
