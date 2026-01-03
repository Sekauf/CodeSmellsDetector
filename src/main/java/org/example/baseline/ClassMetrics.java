package org.example.baseline;

public class ClassMetrics {
    private final String fullyQualifiedName;
    private final int methodCount;
    private final int fieldCount;
    private final int dependencyTypeCount;

    public ClassMetrics(String fullyQualifiedName, int methodCount, int fieldCount, int dependencyTypeCount) {
        this.fullyQualifiedName = fullyQualifiedName;
        this.methodCount = methodCount;
        this.fieldCount = fieldCount;
        this.dependencyTypeCount = dependencyTypeCount;
    }

    public String getFullyQualifiedName() {
        return fullyQualifiedName;
    }

    public int getMethodCount() {
        return methodCount;
    }

    public int getFieldCount() {
        return fieldCount;
    }

    public int getDependencyTypeCount() {
        return dependencyTypeCount;
    }
}
