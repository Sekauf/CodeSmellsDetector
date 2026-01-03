package org.example.baseline;

public class AnalysisMetadata {
    private final String javaVersion;
    private final String javaParserVersion;
    private final String timestampIso;

    public AnalysisMetadata(String javaVersion, String javaParserVersion, String timestampIso) {
        this.javaVersion = javaVersion;
        this.javaParserVersion = javaParserVersion;
        this.timestampIso = timestampIso;
    }

    public String getJavaVersion() {
        return javaVersion;
    }

    public String getJavaParserVersion() {
        return javaParserVersion;
    }

    public String getTimestampIso() {
        return timestampIso;
    }
}
