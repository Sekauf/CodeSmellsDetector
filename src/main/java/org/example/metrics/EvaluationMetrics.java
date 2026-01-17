package org.example.metrics;

public class EvaluationMetrics {
    private final int truePositives;
    private final int falsePositives;
    private final int falseNegatives;
    private final int trueNegatives;
    private final double precision;
    private final double recall;
    private final double f1Score;
    private final double mcc;

    public EvaluationMetrics(
            int truePositives,
            int falsePositives,
            int falseNegatives,
            int trueNegatives,
            double precision,
            double recall,
            double f1Score,
            double mcc
    ) {
        this.truePositives = truePositives;
        this.falsePositives = falsePositives;
        this.falseNegatives = falseNegatives;
        this.trueNegatives = trueNegatives;
        this.precision = precision;
        this.recall = recall;
        this.f1Score = f1Score;
        this.mcc = mcc;
    }

    public int getTruePositives() {
        return truePositives;
    }

    public int getFalsePositives() {
        return falsePositives;
    }

    public int getFalseNegatives() {
        return falseNegatives;
    }

    public int getTrueNegatives() {
        return trueNegatives;
    }

    public double getPrecision() {
        return precision;
    }

    public double getRecall() {
        return recall;
    }

    public double getF1Score() {
        return f1Score;
    }

    public double getMcc() {
        return mcc;
    }
}
