package org.example.metrics;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ReliabilityEvaluatorTest {
    @Test
    public void perfectAgreement_returnsOne() {
        List<Boolean> rater1 = Arrays.asList(true, true, false, false, true);
        List<Boolean> rater2 = Arrays.asList(true, true, false, false, true);

        ReliabilityMetrics metrics = ReliabilityEvaluator.computeReliability(rater1, rater2);

        assertEquals(1.0, metrics.getKappa(), 0.0001);
        assertEquals(1.0, metrics.getAc1(), 0.0001);
        assertEquals(1.0, metrics.getObservedAgreement(), 0.0001);
    }

    @Test
    public void noAgreement_returnsNegativeOne() {
        List<Boolean> rater1 = Arrays.asList(true, true, false, false);
        List<Boolean> rater2 = Arrays.asList(false, false, true, true);

        ReliabilityMetrics metrics = ReliabilityEvaluator.computeReliability(rater1, rater2);

        assertEquals(-1.0, metrics.getKappa(), 0.0001);
        assertEquals(-1.0, metrics.getAc1(), 0.0001);
        assertEquals(0.0, metrics.getObservedAgreement(), 0.0001);
    }

    @Test
    public void partialAgreement_matchesExpectedKappa() {
        List<Boolean> rater1 = Arrays.asList(true, true, true, true, true, false, false, false, false, false);
        List<Boolean> rater2 = Arrays.asList(true, true, true, false, false, true, true, false, false, false);

        ReliabilityMetrics metrics = ReliabilityEvaluator.computeReliability(rater1, rater2);

        assertEquals(0.2, metrics.getKappa(), 0.0001);
        assertEquals(0.2, metrics.getAc1(), 0.0001);
        assertEquals(0.6, metrics.getObservedAgreement(), 0.0001);
    }

    @Test
    public void emptyLists_throw() {
        try {
            ReliabilityEvaluator.computeReliability(List.of(), List.of());
        } catch (IllegalArgumentException ex) {
            assertEquals(true, ex.getMessage().contains("must not be empty"));
            return;
        }
        throw new AssertionError("Expected IllegalArgumentException for empty lists.");
    }

    @Test
    public void sizeMismatch_throw() {
        List<Boolean> rater1 = Arrays.asList(true, false);
        List<Boolean> rater2 = Arrays.asList(true);
        try {
            ReliabilityEvaluator.computeReliability(rater1, rater2);
        } catch (IllegalArgumentException ex) {
            assertEquals(true, ex.getMessage().contains("same size"));
            return;
        }
        throw new AssertionError("Expected IllegalArgumentException for size mismatch.");
    }
}
