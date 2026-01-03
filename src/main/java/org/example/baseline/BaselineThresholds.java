package org.example.baseline;

public class BaselineThresholds {
    private final int methodPlusFieldThreshold;
    private final int dependencyTypeThreshold;

    public BaselineThresholds(int methodPlusFieldThreshold, int dependencyTypeThreshold) {
        this.methodPlusFieldThreshold = methodPlusFieldThreshold;
        this.dependencyTypeThreshold = dependencyTypeThreshold;
    }

    public int getMethodPlusFieldThreshold() {
        return methodPlusFieldThreshold;
    }

    public int getDependencyTypeThreshold() {
        return dependencyTypeThreshold;
    }
}
