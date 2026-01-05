package org.example.sonar;

public class SonarIssue {
    private final String component;
    private final String rule;
    private final String message;
    private final Integer lineNullable;

    public SonarIssue(String component, String rule, String message, Integer lineNullable) {
        this.component = component;
        this.rule = rule;
        this.message = message;
        this.lineNullable = lineNullable;
    }

    public String getComponent() {
        return component;
    }

    public String getRule() {
        return rule;
    }

    public String getMessage() {
        return message;
    }

    public Integer getLineNullable() {
        return lineNullable;
    }
}
