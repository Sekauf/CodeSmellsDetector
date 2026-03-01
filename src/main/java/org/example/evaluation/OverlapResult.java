package org.example.evaluation;

/**
 * Pairwise agreement result between two God-Class detection tools.
 * Holds the Jaccard index and raw overlap counts.
 */
public class OverlapResult {

    private final String toolA;
    private final String toolB;
    private final double jaccard;
    private final int both;
    private final int onlyA;
    private final int onlyB;

    /**
     * Creates an OverlapResult.
     *
     * @param toolA   name of the first tool
     * @param toolB   name of the second tool
     * @param jaccard Jaccard similarity index (|A∩B| / |A∪B|)
     * @param both    number of classes flagged by both tools (|A∩B|)
     * @param onlyA   number of classes flagged only by toolA
     * @param onlyB   number of classes flagged only by toolB
     */
    public OverlapResult(String toolA, String toolB, double jaccard, int both, int onlyA, int onlyB) {
        this.toolA = toolA;
        this.toolB = toolB;
        this.jaccard = jaccard;
        this.both = both;
        this.onlyA = onlyA;
        this.onlyB = onlyB;
    }

    /** @return name of the first tool */
    public String getToolA() {
        return toolA;
    }

    /** @return name of the second tool */
    public String getToolB() {
        return toolB;
    }

    /** @return Jaccard similarity index; 0.0 when both sets are empty */
    public double getJaccard() {
        return jaccard;
    }

    /** @return number of classes flagged by both tools */
    public int getBoth() {
        return both;
    }

    /** @return number of classes flagged only by toolA */
    public int getOnlyA() {
        return onlyA;
    }

    /** @return number of classes flagged only by toolB */
    public int getOnlyB() {
        return onlyB;
    }
}
