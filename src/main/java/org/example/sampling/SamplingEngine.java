package org.example.sampling;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;
import java.util.logging.Logger;
import org.example.baseline.CandidateDTO;

/**
 * Deterministic sampling helper for {@link CandidateDTO} lists.
 *
 * <p>Determinism guarantee: for the same elements and seed, results are identical even if input
 * order differs. This is achieved by a stable pre-sort using {@link #samplingKey(CandidateDTO)}
 * before any shuffling.
 *
 * <p>Stable key: uses fully qualified class name when present; otherwise falls back to a
 * combination of tool flags and {@code toString()} as a last resort.
 *
 * <p>Complexity: O(n log n) due to sorting; additional operations are linear in the input size.
 *
 * <p>Usage examples:
 * <pre>
 * List<CandidateDTO> sample = SamplingEngine.sampleBalanced(candidates, 50, 42L);
 * List<CandidateDTO> sample = SamplingEngine.sampleBalanced(candidates, 0.2, 42L, predicate);
 * List<CandidateDTO> secondReview = SamplingEngine.sampleSecondReview(candidates, 0.2, 42L);
 * </pre>
 *
 * <p>Contract:
 * - null input is treated as empty and returns an empty list.
 * - non-positive counts return an empty list.
 * - null elements inside the list are ignored.
 * - ambiguous label sources should use the predicate overload.
 */
public final class SamplingEngine {
    private static final Logger LOGGER = Logger.getLogger(SamplingEngine.class.getName());

    private SamplingEngine() {
    }

    /**
     * Returns a reproducible sample using a default positive predicate.
     *
     * <p>Determinism is achieved by sorting on a stable key before shuffling with the seed.
     */
    public static List<CandidateDTO> sampleBalanced(List<CandidateDTO> input, int count, long seed) {
        return sampleBalancedInternal(
                input,
                count,
                seed,
                SamplingEngine::defaultPositivePredicate,
                "sampleBalanced(count)",
                null
        );
    }

    /**
     * Returns a reproducible sample using a caller-supplied positive predicate.
     *
     * <p>Determinism is achieved by sorting on a stable key before shuffling with the seed.
     */
    public static List<CandidateDTO> sampleBalanced(
            List<CandidateDTO> input,
            int count,
            long seed,
            Predicate<CandidateDTO> isPositive
    ) {
        return sampleBalancedInternal(input, count, seed, isPositive, "sampleBalanced(count)", null);
    }

    private static List<CandidateDTO> sampleBalancedInternal(
            List<CandidateDTO> input,
            int count,
            long seed,
            Predicate<CandidateDTO> isPositive,
            String mode,
            Double fractionForLog
    ) {
        if (input == null || input.isEmpty() || count <= 0) {
            logBalanced(mode, seed, input == null ? 0 : input.size(), 0, 0, count, 0, fractionForLog);
            return Collections.emptyList();
        }
        if (isPositive == null) {
            throw new IllegalArgumentException("isPositive must not be null");
        }

        List<CandidateDTO> filtered = new ArrayList<>();
        for (CandidateDTO candidate : input) {
            if (candidate != null) {
                filtered.add(candidate);
            }
        }
        if (filtered.isEmpty()) {
            logBalanced(mode, seed, input.size(), 0, 0, count, 0, fractionForLog);
            return Collections.emptyList();
        }

        filtered.sort(Comparator.comparing(SamplingEngine::samplingKey));

        List<CandidateDTO> positives = new ArrayList<>();
        List<CandidateDTO> negatives = new ArrayList<>();
        for (CandidateDTO candidate : filtered) {
            if (isPositive.test(candidate)) {
                positives.add(candidate);
            } else {
                negatives.add(candidate);
            }
        }

        int totalAvailable = positives.size() + negatives.size();
        int finalTarget = Math.min(count, totalAvailable);
        if (finalTarget == 0) {
            logBalanced(mode, seed, input.size(), positives.size(), negatives.size(), count, 0, fractionForLog);
            return Collections.emptyList();
        }

        int targetPos = Math.min((finalTarget + 1) / 2, positives.size());
        int targetNeg = Math.min(finalTarget / 2, negatives.size());

        int remaining = finalTarget - (targetPos + targetNeg);
        if (remaining > 0) {
            int posRemaining = positives.size() - targetPos;
            int negRemaining = negatives.size() - targetNeg;
            int addPos = Math.min(remaining, posRemaining);
            targetPos += addPos;
            remaining -= addPos;
            if (remaining > 0) {
                int addNeg = Math.min(remaining, negRemaining);
                targetNeg += addNeg;
            }
        }

        List<CandidateDTO> posSample = takeRandom(positives, targetPos, seed);
        List<CandidateDTO> negSample = takeRandom(negatives, targetNeg, seed + 1);

        List<CandidateDTO> result = new ArrayList<>(posSample.size() + negSample.size());
        result.addAll(posSample);
        result.addAll(negSample);
        result.sort(Comparator.comparing(SamplingEngine::samplingKey));
        logBalanced(mode, seed, input.size(), positives.size(), negatives.size(), count, result.size(), fractionForLog);
        return result;
    }

    /**
     * Returns a reproducible sample using a fraction of the input size.
     *
     * <p>Rounding uses {@link Math#round(double)} on {@code input.size() * fraction}. Fractions
     * less than or equal to 0 return an empty list; fractions greater than 1 are treated as 1.
     */
    public static List<CandidateDTO> sampleBalanced(
            List<CandidateDTO> input,
            double fraction,
            long seed,
            Predicate<CandidateDTO> isPositive
    ) {
        int count = countFromFraction(input, fraction);
        return sampleBalancedInternal(input, count, seed, isPositive, "sampleBalanced(fraction)", fraction);
    }

    /**
     * Returns a reproducible, roughly balanced stratified sample for second review.
     *
     * <p>Rounding uses {@link Math#round(double)} on {@code candidates.size() * fraction}. This
     * method uses the default positive predicate (tool flags) because CandidateDTO has no
     * label status in this repository.
     */
    public static List<CandidateDTO> sampleSecondReview(
            List<CandidateDTO> candidates,
            double fraction,
            long seed
    ) {
        int count = countFromFraction(candidates, fraction);
        return sampleBalancedInternal(
                candidates,
                count,
                seed,
                SamplingEngine::defaultPositivePredicate,
                "sampleSecondReview",
                fraction
        );
    }

    /**
     * Returns a deterministic sample of undetected candidates from the top percentile by size.
     *
     * <p>Percentile is clamped to (0, 1]. Only candidates where {@code isDetectedByAnyTool}
     * returns false are eligible. Eligible candidates are sorted by {@code sizeMetric} descending,
     * the top percentile bucket is shuffled with the seed, and up to {@code count} are returned.
     */
    public static List<CandidateDTO> sampleBlindNegativesTopPercentile(
            List<CandidateDTO> all,
            int count,
            double percentile,
            long seed,
            Predicate<CandidateDTO> isDetectedByAnyTool,
            java.util.function.ToIntFunction<CandidateDTO> sizeMetric
    ) {
        if (all == null || all.isEmpty() || count <= 0) {
            logBlindNegatives(seed, all == null ? 0 : all.size(), 0, 0, count, 0, percentile, 0);
            return Collections.emptyList();
        }
        if (isDetectedByAnyTool == null) {
            throw new IllegalArgumentException("isDetectedByAnyTool must not be null");
        }
        if (sizeMetric == null) {
            throw new IllegalArgumentException("sizeMetric must not be null");
        }

        double clamped = Math.min(1.0, Math.max(0.0, percentile));
        if (clamped <= 0.0) {
            logBlindNegatives(seed, all.size(), 0, 0, count, 0, percentile, 0);
            return Collections.emptyList();
        }

        List<CandidateDTO> eligible = new ArrayList<>();
        int detectedCount = 0;
        for (CandidateDTO candidate : all) {
            if (candidate != null) {
                if (isDetectedByAnyTool.test(candidate)) {
                    detectedCount++;
                } else {
                    eligible.add(candidate);
                }
            }
        }
        if (eligible.isEmpty()) {
            logBlindNegatives(seed, all.size(), detectedCount, 0, count, 0, percentile, 0);
            return Collections.emptyList();
        }

        eligible.sort(Comparator.comparingInt(sizeMetric).reversed()
                .thenComparing(SamplingEngine::samplingKey));

        int bucketSize = (int) Math.ceil(eligible.size() * clamped);
        if (bucketSize <= 0) {
            logBlindNegatives(seed, all.size(), detectedCount, eligible.size(), count, 0, percentile, 0);
            return Collections.emptyList();
        }
        List<CandidateDTO> bucket = new ArrayList<>(eligible.subList(0, Math.min(bucketSize, eligible.size())));
        Collections.shuffle(bucket, new Random(seed));

        int target = Math.min(count, bucket.size());
        List<CandidateDTO> result = new ArrayList<>(bucket.subList(0, target));
        logBlindNegatives(seed, all.size(), detectedCount, eligible.size(), count, result.size(), percentile, bucketSize);
        return result;
    }

    private static String samplingKey(CandidateDTO candidate) {
        String name = candidate.getFullyQualifiedClassName();
        if (name != null) {
            return name;
        }
        return (candidate.isBaselineFlag() ? "1" : "0")
                + (candidate.isSonarFlag() ? "1" : "0")
                + (candidate.isJdeodorantFlag() ? "1" : "0")
                + "|" + candidate.toString();
    }

    private static boolean defaultPositivePredicate(CandidateDTO candidate) {
        return candidate.isBaselineFlag()
                || candidate.isSonarFlag()
                || candidate.isJdeodorantFlag();
    }

    private static int countFromFraction(List<CandidateDTO> input, double fraction) {
        int inputSize = input == null ? 0 : input.size();
        double clamped = Math.min(1.0, Math.max(0.0, fraction));
        return (int) Math.round(inputSize * clamped);
    }

    private static List<CandidateDTO> takeRandom(List<CandidateDTO> input, int count, long seed) {
        if (input.isEmpty() || count <= 0) {
            return Collections.emptyList();
        }
        List<CandidateDTO> copy = new ArrayList<>(input);
        Collections.shuffle(copy, new Random(seed));
        if (count >= copy.size()) {
            return copy;
        }
        return new ArrayList<>(copy.subList(0, count));
    }

    private static void logBalanced(
            String mode,
            long seed,
            int inputSize,
            int positives,
            int negatives,
            int requested,
            int returned,
            Double fraction
    ) {
        String fractionPart = fraction == null ? "" : " fraction=" + fraction;
        LOGGER.info(
                "SamplingEngine." + mode
                        + " seed=" + seed
                        + " input=" + inputSize
                        + " pos=" + positives
                        + " neg=" + negatives
                        + " requested=" + requested
                        + " returned=" + returned
                        + fractionPart
        );
    }

    private static void logBlindNegatives(
            long seed,
            int inputSize,
            int detected,
            int undetected,
            int requested,
            int returned,
            double percentile,
            int bucketSize
    ) {
        LOGGER.info(
                "SamplingEngine.sampleBlindNegativesTopPercentile"
                        + " seed=" + seed
                        + " input=" + inputSize
                        + " detected=" + detected
                        + " undetected=" + undetected
                        + " requested=" + requested
                        + " returned=" + returned
                        + " percentile=" + percentile
                        + " bucket=" + bucketSize
        );
    }
}
