package org.example.metrics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Computes bootstrap confidence intervals for evaluation metrics (Precision, Recall, F1, MCC).
 *
 * <p>Uses the percentile method with resampling to estimate 95% CIs around point estimates.</p>
 */
public final class BootstrapConfidenceInterval {

    private static final Logger LOGGER = Logger.getLogger(BootstrapConfidenceInterval.class.getName());

    private static final int DEFAULT_N_BOOTSTRAP = 10_000;
    private static final long DEFAULT_SEED = 42L;
    private static final double LOWER_PERCENTILE = 0.025;
    private static final double UPPER_PERCENTILE = 0.975;

    private BootstrapConfidenceInterval() {
    }

    /**
     * A confidence interval with lower bound, upper bound, and point estimate.
     */
    public record ConfidenceInterval(double lower, double upper, double pointEstimate) {
    }

    /**
     * Aggregated bootstrap result containing point estimates and CIs for each metric.
     */
    public record BootstrapResult(
            EvaluationMetrics pointEstimates,
            ConfidenceInterval precisionCI,
            ConfidenceInterval recallCI,
            ConfidenceInterval f1CI,
            ConfidenceInterval mccCI
    ) {
    }

    /**
     * Computes point estimates and bootstrap 95% confidence intervals using default parameters.
     *
     * @param predicted  set of predicted positive class names
     * @param actual     set of actual positive class names
     * @param totalItems total number of items in the population
     * @return bootstrap result with point estimates and CIs
     */
    public static BootstrapResult computeWithCI(Set<String> predicted, Set<String> actual, int totalItems) {
        return computeWithCI(predicted, actual, totalItems, DEFAULT_N_BOOTSTRAP, DEFAULT_SEED);
    }

    /**
     * Computes point estimates and bootstrap 95% confidence intervals.
     *
     * @param predicted  set of predicted positive class names
     * @param actual     set of actual positive class names
     * @param totalItems total number of items in the population
     * @param nBootstrap number of bootstrap iterations (e.g. 10000)
     * @param seed       random seed for reproducibility
     * @return bootstrap result with point estimates and CIs
     */
    public static BootstrapResult computeWithCI(
            Set<String> predicted, Set<String> actual, int totalItems, int nBootstrap, long seed) {

        EvaluationMetrics point = MetricsEngine.computeMetrics(predicted, actual, totalItems);
        LOGGER.info("Bootstrap started: totalItems=" + totalItems + " nBootstrap=" + nBootstrap);

        if (totalItems == 0 || nBootstrap <= 0) {
            return buildEmptyResult(point);
        }

        int[] labels = buildLabelArray(predicted, actual, totalItems, point);
        double[][] samples = runBootstrap(labels, nBootstrap, seed);

        ConfidenceInterval precisionCI = buildCI(samples[0], nBootstrap, point.getPrecision());
        ConfidenceInterval recallCI = buildCI(samples[1], nBootstrap, point.getRecall());
        ConfidenceInterval f1CI = buildCI(samples[2], nBootstrap, point.getF1Score());
        ConfidenceInterval mccCI = buildCI(samples[3], nBootstrap, point.getMcc());

        LOGGER.info("Bootstrap finished. Precision CI=[" + precisionCI.lower() + ", " + precisionCI.upper() + "]");
        return new BootstrapResult(point, precisionCI, recallCI, f1CI, mccCI);
    }

    /**
     * Builds an integer array encoding the confusion-matrix category for each item.
     * 0=TP, 1=FP, 2=FN, 3=TN.
     */
    private static int[] buildLabelArray(
            Set<String> predicted, Set<String> actual, int totalItems, EvaluationMetrics point) {

        int[] labels = new int[totalItems];
        int idx = 0;
        idx = fillLabel(labels, idx, point.getTruePositives(), 0);
        idx = fillLabel(labels, idx, point.getFalsePositives(), 1);
        idx = fillLabel(labels, idx, point.getFalseNegatives(), 2);
        fillLabel(labels, idx, point.getTrueNegatives(), 3);
        return labels;
    }

    private static int fillLabel(int[] labels, int startIdx, int count, int label) {
        for (int i = 0; i < count; i++) {
            labels[startIdx + i] = label;
        }
        return startIdx + count;
    }

    /**
     * Runs the bootstrap resampling loop and returns sampled metric values.
     *
     * @return double[4][nBootstrap] — rows: precision, recall, f1, mcc
     */
    private static double[][] runBootstrap(int[] labels, int nBootstrap, long seed) {
        Random rng = new Random(seed);
        int n = labels.length;
        double[] precisions = new double[nBootstrap];
        double[] recalls = new double[nBootstrap];
        double[] f1s = new double[nBootstrap];
        double[] mccs = new double[nBootstrap];

        for (int i = 0; i < nBootstrap; i++) {
            int tp = 0, fp = 0, fn = 0, tn = 0;
            for (int j = 0; j < n; j++) {
                switch (labels[rng.nextInt(n)]) {
                    case 0 -> tp++;
                    case 1 -> fp++;
                    case 2 -> fn++;
                    default -> tn++;
                }
            }
            EvaluationMetrics m = MetricsEngine.computeMetrics(tp, fp, fn, tn);
            precisions[i] = m.getPrecision();
            recalls[i] = m.getRecall();
            f1s[i] = m.getF1Score();
            mccs[i] = m.getMcc();
        }
        return new double[][]{precisions, recalls, f1s, mccs};
    }

    private static ConfidenceInterval buildCI(double[] values, int n, double pointEstimate) {
        Arrays.sort(values);
        double lower = values[percentileIndex(n, LOWER_PERCENTILE)];
        double upper = values[percentileIndex(n, UPPER_PERCENTILE)];
        return new ConfidenceInterval(lower, upper, pointEstimate);
    }

    private static int percentileIndex(int n, double percentile) {
        int idx = (int) (percentile * n);
        return Math.max(0, Math.min(idx, n - 1));
    }

    private static BootstrapResult buildEmptyResult(EvaluationMetrics point) {
        ConfidenceInterval zeroCi = new ConfidenceInterval(0.0, 0.0, 0.0);
        return new BootstrapResult(point, zeroCi, zeroCi, zeroCi, zeroCi);
    }
}
