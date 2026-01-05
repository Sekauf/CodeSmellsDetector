package org.example.sampling;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.example.baseline.CandidateDTO;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SamplingEngineTest {

    @Test
    public void sameSeedProducesSameOutput() {
        List<CandidateDTO> candidates = mixedCandidates(6, 6);

        List<String> first = names(SamplingEngine.sampleBalanced(candidates, 6, 123L));
        List<String> second = names(SamplingEngine.sampleBalanced(candidates, 6, 123L));

        assertEquals(first, second);
    }

    @Test
    public void inputOrderDoesNotAffectOutput() {
        List<CandidateDTO> base = mixedCandidates(4, 4);
        List<CandidateDTO> reversed = new ArrayList<>(base);
        java.util.Collections.reverse(reversed);

        List<String> first = names(SamplingEngine.sampleBalanced(base, 6, 123L));
        List<String> second = names(SamplingEngine.sampleBalanced(reversed, 6, 123L));

        assertEquals(first, second);
    }

    @Test
    public void balancesWhenPossible() {
        List<CandidateDTO> candidates = mixedCandidates(8, 2);

        List<CandidateDTO> result = SamplingEngine.sampleBalanced(candidates, 4, 7L);
        long positives = result.stream().filter(CandidateDTO::isSonarFlag).count();

        assertEquals(4, result.size());
        assertEquals(2, positives);
    }

    @Test
    public void handlesMinorityShortage() {
        List<CandidateDTO> candidates = mixedCandidates(1, 9);

        List<CandidateDTO> result = SamplingEngine.sampleBalanced(candidates, 6, 7L);
        long positives = result.stream().filter(CandidateDTO::isSonarFlag).count();

        assertEquals(6, result.size());
        assertEquals(1, positives);
    }

    @Test
    public void countZeroReturnsEmpty() {
        List<CandidateDTO> candidates = mixedCandidates(2, 2);

        assertTrue(SamplingEngine.sampleBalanced(candidates, 0, 1L).isEmpty());
    }

    @Test
    public void countNegativeReturnsEmpty() {
        List<CandidateDTO> candidates = mixedCandidates(2, 2);

        assertTrue(SamplingEngine.sampleBalanced(candidates, -5, 1L).isEmpty());
    }

    @Test
    public void nullInputReturnsEmpty() {
        assertTrue(SamplingEngine.sampleBalanced(null, 5, 1L).isEmpty());
    }

    @Test
    public void nullElementsAreIgnored() {
        List<CandidateDTO> candidates = new ArrayList<>();
        candidates.add(null);
        candidates.add(candidate("p0", true));
        candidates.add(null);

        List<CandidateDTO> result = SamplingEngine.sampleBalanced(candidates, 5, 1L);

        assertEquals(1, result.size());
        assertEquals("p0", result.get(0).getFullyQualifiedClassName());
    }

    @Test
    public void countGreaterThanInputReturnsAllAvailable() {
        List<CandidateDTO> candidates = mixedCandidates(3, 2);

        List<CandidateDTO> result = SamplingEngine.sampleBalanced(candidates, 99, 11L);

        assertEquals(5, result.size());
        assertEquals(names(candidates).stream().sorted().collect(Collectors.toList()),
                names(result).stream().sorted().collect(Collectors.toList()));
    }

    @Test
    public void goldenOutputWhenAllSelected() {
        List<CandidateDTO> candidates = mixedCandidates(2, 1);

        List<CandidateDTO> result = SamplingEngine.sampleBalanced(candidates, 10, 99L);

        assertEquals(Arrays.asList("n0", "p0", "p1"), names(result));
    }

    @Test
    public void fractionUsesRoundOnInputSize() {
        List<CandidateDTO> candidates = mixedCandidates(5, 5);

        List<CandidateDTO> result = SamplingEngine.sampleBalanced(
                candidates,
                0.2,
                101L,
                SamplingEngineTest::isPositive
        );

        assertEquals(2, result.size());
    }

    @Test
    public void fractionDeterminismWithSeed() {
        List<CandidateDTO> candidates = mixedCandidates(5, 5);

        List<String> first = names(SamplingEngine.sampleBalanced(
                candidates,
                0.3,
                202L,
                SamplingEngineTest::isPositive
        ));
        List<String> second = names(SamplingEngine.sampleBalanced(
                candidates,
                0.3,
                202L,
                SamplingEngineTest::isPositive
        ));

        assertEquals(first, second);
    }

    @Test
    public void secondReviewUsesBalancedStratifiedSample() {
        List<CandidateDTO> candidates = mixedCandidates(8, 2);

        List<CandidateDTO> result = SamplingEngine.sampleSecondReview(candidates, 0.2, 303L);
        long positives = result.stream().filter(CandidateDTO::isSonarFlag).count();

        assertEquals(2, result.size());
        assertEquals(1, positives);
    }

    @Test
    public void blindNegativesFromTopPercentileOnly() {
        List<CandidateDTO> candidates = new ArrayList<>();
        candidates.add(detectedCandidate("d0", 100));
        candidates.add(undetectedCandidate("u0", 100));
        candidates.add(undetectedCandidate("u1", 90));
        candidates.add(undetectedCandidate("u2", 80));
        candidates.add(undetectedCandidate("u3", 70));
        candidates.add(undetectedCandidate("u4", 60));
        candidates.add(undetectedCandidate("u5", 50));

        List<CandidateDTO> result = SamplingEngine.sampleBlindNegativesTopPercentile(
                candidates,
                2,
                0.4,
                42L,
                SamplingEngineTest::isDetected,
                SamplingEngineTest::sizeMetric
        );

        assertEquals(2, result.size());
        assertTrue(result.stream().noneMatch(SamplingEngineTest::isDetected));
        List<String> names = names(result);
        assertTrue(names.contains("u0"));
        assertTrue(names.contains("u1"));
    }

    @Test
    public void blindNegativesAreDeterministicWithSeed() {
        List<CandidateDTO> candidates = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            candidates.add(undetectedCandidate("u" + i, 100 - i));
        }

        List<String> first = names(SamplingEngine.sampleBlindNegativesTopPercentile(
                candidates,
                3,
                0.5,
                7L,
                SamplingEngineTest::isDetected,
                SamplingEngineTest::sizeMetric
        ));
        List<String> second = names(SamplingEngine.sampleBlindNegativesTopPercentile(
                candidates,
                3,
                0.5,
                7L,
                SamplingEngineTest::isDetected,
                SamplingEngineTest::sizeMetric
        ));

        assertEquals(first, second);
    }

    private static CandidateDTO candidate(String name, boolean positive) {
        return CandidateDTO.builder(name).sonarFlag(positive).build();
    }

    private static List<CandidateDTO> mixedCandidates(int positives, int negatives) {
        List<CandidateDTO> candidates = new ArrayList<>();
        for (int i = 0; i < positives; i++) {
            candidates.add(candidate("p" + i, true));
        }
        for (int i = 0; i < negatives; i++) {
            candidates.add(candidate("n" + i, false));
        }
        return candidates;
    }

    private static List<String> names(List<CandidateDTO> candidates) {
        return candidates.stream()
                .map(CandidateDTO::getFullyQualifiedClassName)
                .collect(Collectors.toList());
    }

    private static boolean isPositive(CandidateDTO candidate) {
        return candidate.isSonarFlag();
    }

    private static CandidateDTO undetectedCandidate(String name, int wmc) {
        return CandidateDTO.builder(name).wmc(wmc).build();
    }

    private static CandidateDTO detectedCandidate(String name, int wmc) {
        return CandidateDTO.builder(name).wmc(wmc).sonarFlag(true).build();
    }

    private static boolean isDetected(CandidateDTO candidate) {
        return candidate.isBaselineFlag()
                || candidate.isSonarFlag()
                || candidate.isJdeodorantFlag();
    }

    private static int sizeMetric(CandidateDTO candidate) {
        return candidate.getWmc();
    }
}
