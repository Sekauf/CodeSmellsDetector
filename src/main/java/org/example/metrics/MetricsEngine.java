package org.example.metrics;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

public final class MetricsEngine {
    private static final Logger LOGGER = Logger.getLogger(MetricsEngine.class.getName());

    private MetricsEngine() {
    }

    public static EvaluationMetrics computeMetrics(Set<String> predicted, Set<String> actual, int totalItems) {
        if (totalItems < 0) {
            throw new IllegalArgumentException("totalItems must be >= 0");
        }
        Set<String> predictedSet = normalizeSet(predicted);
        Set<String> actualSet = normalizeSet(actual);
        LOGGER.info(
                "MetricsEngine started. predicted=" + predictedSet.size()
                        + " actual=" + actualSet.size()
                        + " total=" + totalItems
        );

        int truePositives = 0;
        for (String value : predictedSet) {
            if (actualSet.contains(value)) {
                truePositives++;
            }
        }
        int falsePositives = predictedSet.size() - truePositives;

        int falseNegatives = 0;
        for (String value : actualSet) {
            if (!predictedSet.contains(value)) {
                falseNegatives++;
            }
        }

        int trueNegatives = totalItems - truePositives - falsePositives - falseNegatives;
        if (trueNegatives < 0) {
            throw new IllegalArgumentException("totalItems is smaller than tp+fp+fn.");
        }

        EvaluationMetrics metrics = computeMetrics(truePositives, falsePositives, falseNegatives, trueNegatives);
        LOGGER.info(
                "MetricsEngine finished. tp=" + truePositives
                        + " fp=" + falsePositives
                        + " fn=" + falseNegatives
                        + " tn=" + trueNegatives
        );
        return metrics;
    }

    public static EvaluationMetrics computeMetrics(
            int truePositives,
            int falsePositives,
            int falseNegatives,
            int trueNegatives
    ) {
        if (truePositives < 0 || falsePositives < 0 || falseNegatives < 0 || trueNegatives < 0) {
            throw new IllegalArgumentException("Confusion matrix values must be >= 0.");
        }

        double precision = safeDivide(truePositives, truePositives + falsePositives);
        double recall = safeDivide(truePositives, truePositives + falseNegatives);
        double f1Score = (precision + recall) == 0.0 ? 0.0 : (2.0 * precision * recall) / (precision + recall);
        double mcc = computeMcc(truePositives, falsePositives, falseNegatives, trueNegatives);
        double specificity = safeDivide(trueNegatives, trueNegatives + falsePositives);

        return new EvaluationMetrics(
                truePositives,
                falsePositives,
                falseNegatives,
                trueNegatives,
                precision,
                recall,
                f1Score,
                mcc,
                specificity
        );
    }

    private static Set<String> normalizeSet(Set<String> input) {
        if (input == null || input.isEmpty()) {
            return Set.of();
        }
        Set<String> normalized = new HashSet<>();
        for (String value : input) {
            if (value != null) {
                normalized.add(value);
            }
        }
        return normalized;
    }

    private static double safeDivide(double numerator, double denominator) {
        if (denominator == 0.0) {
            return 0.0;
        }
        return numerator / denominator;
    }

    private static double computeMcc(int tp, int fp, int fn, int tn) {
        double denominator = Math.sqrt(
                (tp + fp) * (double) (tp + fn) * (tn + fp) * (double) (tn + fn)
        );
        if (denominator == 0.0) {
            return 0.0;
        }
        double numerator = (tp * (double) tn) - (fp * (double) fn);
        return numerator / denominator;
    }
}
