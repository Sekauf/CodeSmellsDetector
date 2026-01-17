package org.example.metrics;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ReliabilityEvaluator {
    private ReliabilityEvaluator() {
    }

    public static ReliabilityMetrics computeReliability(List<Boolean> rater1, List<Boolean> rater2) {
        if (rater1 == null || rater2 == null) {
            throw new IllegalArgumentException("rater lists must not be null.");
        }
        if (rater1.size() != rater2.size()) {
            throw new IllegalArgumentException("rater lists must have the same size.");
        }
        if (rater1.isEmpty()) {
            throw new IllegalArgumentException("rater lists must not be empty.");
        }

        int bothPositive = 0;
        int rater1PositiveRater2Negative = 0;
        int rater1NegativeRater2Positive = 0;
        int bothNegative = 0;

        for (int i = 0; i < rater1.size(); i++) {
            Boolean first = rater1.get(i);
            Boolean second = rater2.get(i);
            if (first == null || second == null) {
                throw new IllegalArgumentException("rater lists must not contain null values.");
            }
            if (first && second) {
                bothPositive++;
            } else if (first) {
                rater1PositiveRater2Negative++;
            } else if (second) {
                rater1NegativeRater2Positive++;
            } else {
                bothNegative++;
            }
        }

        return computeFromCounts(
                bothPositive,
                rater1PositiveRater2Negative,
                rater1NegativeRater2Positive,
                bothNegative
        );
    }

    public static ReliabilityMetrics computeReliability(Set<String> rater1, Set<String> rater2, int totalItems) {
        if (totalItems < 0) {
            throw new IllegalArgumentException("totalItems must be >= 0");
        }
        Set<String> rater1Set = normalizeSet(rater1);
        Set<String> rater2Set = normalizeSet(rater2);

        int bothPositive = 0;
        for (String value : rater1Set) {
            if (rater2Set.contains(value)) {
                bothPositive++;
            }
        }
        int rater1PositiveRater2Negative = rater1Set.size() - bothPositive;
        int rater1NegativeRater2Positive = rater2Set.size() - bothPositive;
        int bothNegative = totalItems - bothPositive - rater1PositiveRater2Negative - rater1NegativeRater2Positive;
        if (bothNegative < 0) {
            throw new IllegalArgumentException("totalItems is smaller than positive union.");
        }

        return computeFromCounts(
                bothPositive,
                rater1PositiveRater2Negative,
                rater1NegativeRater2Positive,
                bothNegative
        );
    }

    private static ReliabilityMetrics computeFromCounts(
            int bothPositive,
            int rater1PositiveRater2Negative,
            int rater1NegativeRater2Positive,
            int bothNegative
    ) {
        if (bothPositive < 0
                || rater1PositiveRater2Negative < 0
                || rater1NegativeRater2Positive < 0
                || bothNegative < 0) {
            throw new IllegalArgumentException("counts must be >= 0.");
        }
        int total = bothPositive + rater1PositiveRater2Negative + rater1NegativeRater2Positive + bothNegative;
        if (total == 0) {
            throw new IllegalArgumentException("counts must not all be zero.");
        }

        double totalDouble = total;
        double observedAgreement = (bothPositive + bothNegative) / totalDouble;

        double p1 = (bothPositive + rater1PositiveRater2Negative) / totalDouble;
        double p2 = (bothPositive + rater1NegativeRater2Positive) / totalDouble;
        double expectedKappa = (p1 * p2) + ((1.0 - p1) * (1.0 - p2));
        double kappa = agreementAdjusted(observedAgreement, expectedKappa);

        double averagePositive = (p1 + p2) / 2.0;
        double expectedAc1 = 2.0 * averagePositive * (1.0 - averagePositive);
        double ac1 = agreementAdjusted(observedAgreement, expectedAc1);

        return new ReliabilityMetrics(
                bothPositive,
                rater1PositiveRater2Negative,
                rater1NegativeRater2Positive,
                bothNegative,
                observedAgreement,
                kappa,
                ac1
        );
    }

    private static double agreementAdjusted(double observed, double expected) {
        double denominator = 1.0 - expected;
        if (denominator == 0.0) {
            return observed == 1.0 ? 1.0 : 0.0;
        }
        return (observed - expected) / denominator;
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
}
