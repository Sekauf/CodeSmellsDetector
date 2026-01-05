package org.example.sonar;

public class SonarScanResult {
    private final int exitCode;
    private final String output;
    private final String ceTaskIdNullable;

    public SonarScanResult(int exitCode, String output, String ceTaskIdNullable) {
        this.exitCode = exitCode;
        this.output = output;
        this.ceTaskIdNullable = ceTaskIdNullable;
    }

    public int getExitCode() {
        return exitCode;
    }

    public String getOutput() {
        return output;
    }

    public String getCeTaskIdNullable() {
        return ceTaskIdNullable;
    }
}
