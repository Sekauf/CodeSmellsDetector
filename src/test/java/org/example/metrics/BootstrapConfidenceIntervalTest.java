package org.example.metrics;

import org.example.metrics.BootstrapConfidenceInterval.BootstrapResult;
import org.example.metrics.BootstrapConfidenceInterval.ConfidenceInterval;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link BootstrapConfidenceInterval}.
 */
class BootstrapConfidenceIntervalTest {

    private static final double TOLERANCE = 0.0001;

    @Test
    void perfectAgreement_ciShouldBeOneToOne() {
        // When totalItems == predicted.size() == actual.size(), every item is TP.
        // Every bootstrap sample draws only TPs, so P/R/F1 are always 1.0.
        Set<String> items = Set.of("A", "B", "C");
        BootstrapResult result = BootstrapConfidenceInterval.computeWithCI(items, items, 3, 1000, 42L);

        assertEquals(1.0, result.precisionCI().lower(), TOLERANCE);
        assertEquals(1.0, result.precisionCI().upper(), TOLERANCE);
        assertEquals(1.0, result.recallCI().lower(), TOLERANCE);
        assertEquals(1.0, result.recallCI().upper(), TOLERANCE);
        assertEquals(1.0, result.f1CI().lower(), TOLERANCE);
        assertEquals(1.0, result.f1CI().upper(), TOLERANCE);
    }

    @Test
    void totalItemsZero_returnsEmptyDefaults_noCrash() {
        BootstrapResult result = BootstrapConfidenceInterval.computeWithCI(Set.of(), Set.of(), 0, 1000, 42L);

        assertNotNull(result);
        assertNotNull(result.pointEstimates());
        assertEquals(0.0, result.precisionCI().lower(), TOLERANCE);
        assertEquals(0.0, result.precisionCI().upper(), TOLERANCE);
        assertEquals(0.0, result.recallCI().lower(), TOLERANCE);
        assertEquals(0.0, result.recallCI().upper(), TOLERANCE);
        assertEquals(0.0, result.f1CI().lower(), TOLERANCE);
        assertEquals(0.0, result.f1CI().upper(), TOLERANCE);
        assertEquals(0.0, result.mccCI().lower(), TOLERANCE);
        assertEquals(0.0, result.mccCI().upper(), TOLERANCE);
    }

    @Test
    void deterministicWithSameSeed() {
        Set<String> predicted = Set.of("A", "B", "C");
        Set<String> actual = Set.of("A", "D", "E");

        BootstrapResult r1 = BootstrapConfidenceInterval.computeWithCI(predicted, actual, 20, 5000, 42L);
        BootstrapResult r2 = BootstrapConfidenceInterval.computeWithCI(predicted, actual, 20, 5000, 42L);

        assertEquals(r1.precisionCI().lower(), r2.precisionCI().lower(), TOLERANCE);
        assertEquals(r1.precisionCI().upper(), r2.precisionCI().upper(), TOLERANCE);
        assertEquals(r1.recallCI().lower(), r2.recallCI().lower(), TOLERANCE);
        assertEquals(r1.recallCI().upper(), r2.recallCI().upper(), TOLERANCE);
        assertEquals(r1.f1CI().lower(), r2.f1CI().lower(), TOLERANCE);
        assertEquals(r1.f1CI().upper(), r2.f1CI().upper(), TOLERANCE);
        assertEquals(r1.mccCI().lower(), r2.mccCI().lower(), TOLERANCE);
        assertEquals(r1.mccCI().upper(), r2.mccCI().upper(), TOLERANCE);
    }

    @Test
    void differentSeed_producesDifferentResults() {
        Set<String> predicted = new HashSet<>(Set.of("A", "B", "C", "D", "E", "F", "G"));
        Set<String> actual = new HashSet<>(Set.of("A", "B", "C", "H", "I", "J", "K", "L"));
        int totalItems = 50;

        BootstrapResult r1 = BootstrapConfidenceInterval.computeWithCI(predicted, actual, totalItems, 10000, 1L);
        BootstrapResult r2 = BootstrapConfidenceInterval.computeWithCI(predicted, actual, totalItems, 10000, 999L);

        boolean anyDifference =
                Math.abs(r1.precisionCI().lower() - r2.precisionCI().lower()) > TOLERANCE
                || Math.abs(r1.precisionCI().upper() - r2.precisionCI().upper()) > TOLERANCE
                || Math.abs(r1.recallCI().lower() - r2.recallCI().lower()) > TOLERANCE
                || Math.abs(r1.recallCI().upper() - r2.recallCI().upper()) > TOLERANCE
                || Math.abs(r1.f1CI().lower() - r2.f1CI().lower()) > TOLERANCE
                || Math.abs(r1.f1CI().upper() - r2.f1CI().upper()) > TOLERANCE
                || Math.abs(r1.mccCI().lower() - r2.mccCI().lower()) > TOLERANCE
                || Math.abs(r1.mccCI().upper() - r2.mccCI().upper()) > TOLERANCE;
        assertTrue(anyDifference, "Different seeds should produce at least one different CI bound");
    }

    @Test
    void knownSetup_tp5_fp2_fn3_tn40_ciWidthsPlausible() {
        Set<String> predicted = new HashSet<>(Set.of("A", "B", "C", "D", "E", "F", "G"));
        Set<String> actual = new HashSet<>(Set.of("A", "B", "C", "D", "E", "H", "I", "J"));
        int totalItems = 50;

        BootstrapResult result = BootstrapConfidenceInterval.computeWithCI(
                predicted, actual, totalItems, 10000, 42L);

        EvaluationMetrics point = result.pointEstimates();
        assertEquals(5, point.getTruePositives());
        assertEquals(2, point.getFalsePositives());
        assertEquals(3, point.getFalseNegatives());
        assertEquals(40, point.getTrueNegatives());
        assertEquals(5.0 / 7.0, point.getPrecision(), TOLERANCE);
        assertEquals(5.0 / 8.0, point.getRecall(), TOLERANCE);

        assertCIContainsPoint(result.precisionCI());
        assertCIContainsPoint(result.recallCI());
        assertCIContainsPoint(result.f1CI());
        assertCIContainsPoint(result.mccCI());

        assertPlausibleWidth(result.precisionCI(), "precision");
        assertPlausibleWidth(result.recallCI(), "recall");
        assertPlausibleWidth(result.f1CI(), "f1");
        assertPlausibleWidth(result.mccCI(), "mcc");
    }

    @Test
    void pointEstimateMatchesMetricsEngine() {
        Set<String> predicted = Set.of("A", "B");
        Set<String> actual = Set.of("B", "C");

        BootstrapResult result = BootstrapConfidenceInterval.computeWithCI(predicted, actual, 5, 100, 42L);
        EvaluationMetrics direct = MetricsEngine.computeMetrics(predicted, actual, 5);

        assertEquals(direct.getPrecision(), result.pointEstimates().getPrecision(), TOLERANCE);
        assertEquals(direct.getRecall(), result.pointEstimates().getRecall(), TOLERANCE);
        assertEquals(direct.getF1Score(), result.pointEstimates().getF1Score(), TOLERANCE);
        assertEquals(direct.getMcc(), result.pointEstimates().getMcc(), TOLERANCE);
    }

    @Test
    void defaultOverload_usesDefaultParams() {
        Set<String> predicted = Set.of("A", "B");
        Set<String> actual = Set.of("B", "C");

        BootstrapResult result = BootstrapConfidenceInterval.computeWithCI(predicted, actual, 5);
        assertNotNull(result);
        assertNotNull(result.precisionCI());
        assertNotNull(result.recallCI());
    }

    private static void assertCIContainsPoint(ConfidenceInterval ci) {
        assertTrue(ci.lower() <= ci.pointEstimate() + TOLERANCE,
                "CI lower (" + ci.lower() + ") should be <= point (" + ci.pointEstimate() + ")");
        assertTrue(ci.upper() >= ci.pointEstimate() - TOLERANCE,
                "CI upper (" + ci.upper() + ") should be >= point (" + ci.pointEstimate() + ")");
    }

    private static void assertPlausibleWidth(ConfidenceInterval ci, String name) {
        double width = ci.upper() - ci.lower();
        assertTrue(width > 0.0, name + " CI width should be > 0, was " + width);
        assertTrue(width < 1.0, name + " CI width should be < 1, was " + width);
    }
}
