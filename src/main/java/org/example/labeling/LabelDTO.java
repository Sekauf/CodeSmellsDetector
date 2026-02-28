package org.example.labeling;

/**
 * Data transfer object representing a manual labeling decision for a candidate class.
 * Holds four binary criteria (k1–k4), an optional comment, and the derived final label.
 */
public class LabelDTO {

    /** Three-tier classification outcome derived from the four criteria votes. */
    public enum FinalLabel {
        GOD_CLASS,
        UNCERTAIN,
        NO
    }

    private static final int THRESHOLD_GOD_CLASS = 3;
    private static final int THRESHOLD_UNCERTAIN = 2;

    private final String fullyQualifiedClassName;
    private final Boolean k1;
    private final Boolean k2;
    private final Boolean k3;
    private final Boolean k4;
    private final String comment;
    private final FinalLabel finalLabel;

    /**
     * Creates a fully populated LabelDTO.
     *
     * @param fullyQualifiedClassName primary key identifying the class
     * @param k1 criterion 1 vote (null treated as false)
     * @param k2 criterion 2 vote (null treated as false)
     * @param k3 criterion 3 vote (null treated as false)
     * @param k4 criterion 4 vote (null treated as false)
     * @param comment optional free-text remark
     * @param finalLabel explicitly supplied label (may differ from derived label)
     */
    public LabelDTO(
            String fullyQualifiedClassName,
            Boolean k1,
            Boolean k2,
            Boolean k3,
            Boolean k4,
            String comment,
            FinalLabel finalLabel) {
        this.fullyQualifiedClassName = fullyQualifiedClassName;
        this.k1 = k1;
        this.k2 = k2;
        this.k3 = k3;
        this.k4 = k4;
        this.comment = comment;
        this.finalLabel = finalLabel;
    }

    /**
     * Derives the {@link FinalLabel} from four binary criteria votes.
     * Null values count as {@code false}.
     * <ul>
     *   <li>≥3 true → {@code GOD_CLASS}</li>
     *   <li>2 true → {@code UNCERTAIN}</li>
     *   <li>≤1 true → {@code NO}</li>
     * </ul>
     *
     * @param k1 criterion 1
     * @param k2 criterion 2
     * @param k3 criterion 3
     * @param k4 criterion 4
     * @return derived final label
     */
    public static FinalLabel deriveLabel(Boolean k1, Boolean k2, Boolean k3, Boolean k4) {
        int trueCount = countTrue(k1) + countTrue(k2) + countTrue(k3) + countTrue(k4);
        if (trueCount >= THRESHOLD_GOD_CLASS) {
            return FinalLabel.GOD_CLASS;
        }
        if (trueCount == THRESHOLD_UNCERTAIN) {
            return FinalLabel.UNCERTAIN;
        }
        return FinalLabel.NO;
    }

    private static int countTrue(Boolean value) {
        return Boolean.TRUE.equals(value) ? 1 : 0;
    }

    /** @return fully qualified class name (primary key) */
    public String getFullyQualifiedClassName() {
        return fullyQualifiedClassName;
    }

    /** @return criterion 1 vote, may be null (unanswered) */
    public Boolean getK1() {
        return k1;
    }

    /** @return criterion 2 vote, may be null (unanswered) */
    public Boolean getK2() {
        return k2;
    }

    /** @return criterion 3 vote, may be null (unanswered) */
    public Boolean getK3() {
        return k3;
    }

    /** @return criterion 4 vote, may be null (unanswered) */
    public Boolean getK4() {
        return k4;
    }

    /** @return optional comment, may be null */
    public String getComment() {
        return comment;
    }

    /** @return final label, may be null when not yet assigned */
    public FinalLabel getFinalLabel() {
        return finalLabel;
    }
}
