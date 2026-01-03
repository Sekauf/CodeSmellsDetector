package org.example.baseline;

import java.nio.file.Path;

public class ParsedType {
    private final String fullyQualifiedName;
    private final String kind;
    private final Path sourceFile;

    public ParsedType(String fullyQualifiedName, String kind, Path sourceFile) {
        this.fullyQualifiedName = fullyQualifiedName;
        this.kind = kind;
        this.sourceFile = sourceFile;
    }

    public String getFullyQualifiedName() {
        return fullyQualifiedName;
    }

    public String getKind() {
        return kind;
    }

    public Path getSourceFile() {
        return sourceFile;
    }
}
