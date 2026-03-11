package org.example.gui;

import java.util.List;
import org.example.baseline.CandidateDTO;

/**
 * Computes pairwise Jaccard agreement and Venn region counts from a candidate list.
 * All computation is side-effect-free; pass candidates and receive result records.
 */
public class AgreementCalculator {

    private static final String BASELINE   = "Baseline";
    private static final String SONAR_QUBE = "SonarQube";
    private static final String JDEODORANT = "JDeodorant";

    /** Per-pair statistics: both flags set, only A set, only B set. */
    public record PairStats(String toolA, String toolB, int both, int onlyA, int onlyB) {
        /** Jaccard index = both / (both + onlyA + onlyB). Returns 0 for empty sets. */
        public double jaccard() {
            int union = both + onlyA + onlyB;
            return union == 0 ? 0.0 : (double) both / union;
        }
    }

    /** Counts for the 7 exclusive regions of a 3-set Venn diagram. */
    public record VennCounts(
            int onlyBaseline, int onlySonar, int onlyJdeo,
            int baselineSonarOnly, int baselineJdeoOnly, int sonarJdeoOnly,
            int allThree) {
    }

    /**
     * Computes Jaccard statistics for all three tool pairs (B-S, B-JD, S-JD).
     *
     * @param candidates candidate list; may be null or empty
     * @return list of three PairStats
     */
    public List<PairStats> computePairs(List<CandidateDTO> candidates) {
        List<CandidateDTO> safe = candidates == null ? List.of() : candidates;
        return List.of(
                computePair(safe, BASELINE, SONAR_QUBE),
                computePair(safe, BASELINE, JDEODORANT),
                computePair(safe, SONAR_QUBE, JDEODORANT));
    }

    /**
     * Computes Venn region counts for all three tools.
     *
     * @param candidates candidate list; may be null or empty
     * @return VennCounts with all 7 exclusive region counts
     */
    public VennCounts computeVenn(List<CandidateDTO> candidates) {
        List<CandidateDTO> safe = candidates == null ? List.of() : candidates;
        int onlyB = 0, onlyS = 0, onlyJ = 0;
        int bsOnly = 0, bjOnly = 0, sjOnly = 0, all = 0;
        for (CandidateDTO c : safe) {
            boolean b = c.isBaselineFlag(), s = c.isSonarFlag(), j = c.isJdeodorantFlag();
            if      (b && s && j) { all++; }
            else if (b && s)      { bsOnly++; }
            else if (b && j)      { bjOnly++; }
            else if (s && j)      { sjOnly++; }
            else if (b)           { onlyB++; }
            else if (s)           { onlyS++; }
            else if (j)           { onlyJ++; }
        }
        return new VennCounts(onlyB, onlyS, onlyJ, bsOnly, bjOnly, sjOnly, all);
    }

    private PairStats computePair(List<CandidateDTO> candidates, String toolA, String toolB) {
        int both = 0, onlyA = 0, onlyB = 0;
        for (CandidateDTO c : candidates) {
            boolean a = flagFor(c, toolA), b = flagFor(c, toolB);
            if      (a && b) { both++; }
            else if (a)      { onlyA++; }
            else if (b)      { onlyB++; }
        }
        return new PairStats(toolA, toolB, both, onlyA, onlyB);
    }

    private boolean flagFor(CandidateDTO c, String tool) {
        return switch (tool) {
            case BASELINE   -> c.isBaselineFlag();
            case SONAR_QUBE -> c.isSonarFlag();
            case JDEODORANT -> c.isJdeodorantFlag();
            default         -> false;
        };
    }
}
