package org.example.jdeodorant;

import java.util.Objects;

public class ProjectConfig {
    private final String projectPath;
    private final String jdeodorantCsvPath;
    private final boolean headlessEnabled;

    public ProjectConfig(String projectPath, String jdeodorantCsvPath, boolean headlessEnabled) {
        this.projectPath = projectPath;
        this.jdeodorantCsvPath = jdeodorantCsvPath;
        this.headlessEnabled = headlessEnabled;
    }

    public static ProjectConfig forJdeodorantCsv(String jdeodorantCsvPath) {
        return new ProjectConfig(null, Objects.requireNonNull(jdeodorantCsvPath, "jdeodorantCsvPath"), false);
    }

    public String getProjectPath() {
        return projectPath;
    }

    public String getJdeodorantCsvPath() {
        return jdeodorantCsvPath;
    }

    public boolean isHeadlessEnabled() {
        return headlessEnabled;
    }
}
