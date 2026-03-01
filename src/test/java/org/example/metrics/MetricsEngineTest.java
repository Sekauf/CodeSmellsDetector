package org.example.metrics;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class MetricsEngineTest {
    @Test
    public void computeMetrics_basicCase() {
        Set<String> actual = new HashSet<>(Arrays.asList("A", "B"));
        Set<String> predicted = new HashSet<>(Arrays.asList("A", "C"));

        EvaluationMetrics metrics = MetricsEngine.computeMetrics(predicted, actual, 5);

        assertEquals(1, metrics.getTruePositives());
        assertEquals(1, metrics.getFalsePositives());
        assertEquals(1, metrics.getFalseNegatives());
        assertEquals(2, metrics.getTrueNegatives());
        assertEquals(0.5, metrics.getPrecision(), 0.0001);
        assertEquals(0.5, metrics.getRecall(), 0.0001);
        assertEquals(0.5, metrics.getF1Score(), 0.0001);
        assertEquals(1.0 / 6.0, metrics.getMcc(), 0.0001);
    }

    @Test
    public void computeMetrics_perfectAgreement() {
        Set<String> actual = new HashSet<>(Arrays.asList("A", "B"));
        Set<String> predicted = new HashSet<>(Arrays.asList("A", "B"));

        EvaluationMetrics metrics = MetricsEngine.computeMetrics(predicted, actual, 4);

        assertEquals(2, metrics.getTruePositives());
        assertEquals(0, metrics.getFalsePositives());
        assertEquals(0, metrics.getFalseNegatives());
        assertEquals(2, metrics.getTrueNegatives());
        assertEquals(1.0, metrics.getPrecision(), 0.0001);
        assertEquals(1.0, metrics.getRecall(), 0.0001);
        assertEquals(1.0, metrics.getF1Score(), 0.0001);
        assertEquals(1.0, metrics.getMcc(), 0.0001);
    }

    @Test
    public void computeMetrics_allWrong() {
        Set<String> actual = new HashSet<>(Arrays.asList("A", "B"));
        Set<String> predicted = new HashSet<>(Arrays.asList("C", "D"));

        EvaluationMetrics metrics = MetricsEngine.computeMetrics(predicted, actual, 4);

        assertEquals(0, metrics.getTruePositives());
        assertEquals(2, metrics.getFalsePositives());
        assertEquals(2, metrics.getFalseNegatives());
        assertEquals(0, metrics.getTrueNegatives());
        assertEquals(0.0, metrics.getPrecision(), 0.0001);
        assertEquals(0.0, metrics.getRecall(), 0.0001);
        assertEquals(0.0, metrics.getF1Score(), 0.0001);
        assertEquals(-1.0, metrics.getMcc(), 0.0001);
    }

    @Test
    public void computeMetrics_emptySets_doNotDivideByZero() {
        EvaluationMetrics metrics = MetricsEngine.computeMetrics(Set.of(), Set.of(), 0);

        assertEquals(0, metrics.getTruePositives());
        assertEquals(0, metrics.getFalsePositives());
        assertEquals(0, metrics.getFalseNegatives());
        assertEquals(0, metrics.getTrueNegatives());
        assertEquals(0.0, metrics.getPrecision(), 0.0001);
        assertEquals(0.0, metrics.getRecall(), 0.0001);
        assertEquals(0.0, metrics.getF1Score(), 0.0001);
        assertEquals(0.0, metrics.getMcc(), 0.0001);
    }

    @Test
    public void computeMetrics_specificity() {
        // tp=1, fp=1, fn=1, tn=2 → specificity = 2/(2+1) = 2/3
        Set<String> actual = new HashSet<>(Arrays.asList("A", "B"));
        Set<String> predicted = new HashSet<>(Arrays.asList("A", "C"));

        EvaluationMetrics metrics = MetricsEngine.computeMetrics(predicted, actual, 5);

        assertEquals(2.0 / 3.0, metrics.getSpecificity(), 0.0001);
    }

    @Test
    public void computeMetrics_specificity_divByZero_returnsZero() {
        // All items are positive (tp+fp = total, tn=0, fp=0) → TN+FP = 0 → specificity = 0
        Set<String> actual = new HashSet<>(Arrays.asList("A", "B"));
        Set<String> predicted = new HashSet<>(Arrays.asList("A", "B"));

        EvaluationMetrics metrics = MetricsEngine.computeMetrics(predicted, actual, 2);

        assertEquals(0, metrics.getTrueNegatives());
        assertEquals(0, metrics.getFalsePositives());
        assertEquals(0.0, metrics.getSpecificity(), 0.0001);
    }

    @Test
    public void computeMetrics_invalidTotalItems_throws() {
        Set<String> actual = new HashSet<>(Arrays.asList("A", "B"));
        Set<String> predicted = new HashSet<>(Arrays.asList("C"));

        try {
            MetricsEngine.computeMetrics(predicted, actual, 1);
        } catch (IllegalArgumentException ex) {
            assertEquals(true, ex.getMessage().contains("totalItems"));
            return;
        }
        throw new AssertionError("Expected IllegalArgumentException for invalid totalItems.");
    }
}
