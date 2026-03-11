package org.example.gui;

import javax.swing.RowFilter;
import org.example.baseline.CandidateDTO;

/**
 * Composite row filter for the candidate results table.
 * Combines a FQCN text search, required tool flags, and a quick-filter mode.
 * All active criteria are combined with logical AND.
 */
public class CandidateRowFilter extends RowFilter<CandidateTableModel, Integer> {

    /** Predefined quick-filter modes that restrict rows by tool-flag count. */
    public enum QuickMode {
        /** No quick filter — show all candidates regardless of flag count. */
        NONE,
        /** All three tools agree: exactly three flags are true. */
        ALL_AGREE,
        /** Exactly one tool flagged the class. */
        ONE_TOOL,
        /** Exactly two of three tools flagged the class ("disagreement"). */
        DISAGREEMENT
    }

    private final String    text;
    private final boolean   requireBaseline;
    private final boolean   requireSonar;
    private final boolean   requireJdeo;
    private final QuickMode quickMode;

    /**
     * @param text            substring to match in FQCN (case-insensitive; empty = no restriction)
     * @param requireBaseline when {@code true} the row must have {@code baselineFlag=true}
     * @param requireSonar    when {@code true} the row must have {@code sonarFlag=true}
     * @param requireJdeo     when {@code true} the row must have {@code jdeodorantFlag=true}
     * @param quickMode       predefined quick-filter mode ({@code null} treated as {@link QuickMode#NONE})
     */
    public CandidateRowFilter(
            String text,
            boolean requireBaseline,
            boolean requireSonar,
            boolean requireJdeo,
            QuickMode quickMode) {
        this.text            = text == null ? "" : text.trim().toLowerCase();
        this.requireBaseline = requireBaseline;
        this.requireSonar    = requireSonar;
        this.requireJdeo     = requireJdeo;
        this.quickMode       = quickMode == null ? QuickMode.NONE : quickMode;
    }

    @Override
    public boolean include(Entry<? extends CandidateTableModel, ? extends Integer> entry) {
        CandidateDTO dto = entry.getModel().getRow(entry.getIdentifier());
        return matchesText(dto) && matchesFlags(dto) && matchesQuick(dto);
    }

    private boolean matchesText(CandidateDTO dto) {
        if (text.isEmpty()) { return true; }
        String fqcn = dto.getFullyQualifiedClassName();
        return fqcn != null && fqcn.toLowerCase().contains(text);
    }

    private boolean matchesFlags(CandidateDTO dto) {
        if (requireBaseline && !dto.isBaselineFlag())   { return false; }
        if (requireSonar    && !dto.isSonarFlag())      { return false; }
        if (requireJdeo     && !dto.isJdeodorantFlag()) { return false; }
        return true;
    }

    private boolean matchesQuick(CandidateDTO dto) {
        int flags = flagCount(dto);
        return switch (quickMode) {
            case NONE         -> true;
            case ALL_AGREE    -> flags == 3;
            case ONE_TOOL     -> flags == 1;
            case DISAGREEMENT -> flags == 2;
        };
    }

    private static int flagCount(CandidateDTO dto) {
        int n = 0;
        if (dto.isBaselineFlag())   { n++; }
        if (dto.isSonarFlag())      { n++; }
        if (dto.isJdeodorantFlag()) { n++; }
        return n;
    }

    /**
     * Returns {@code true} when this filter imposes at least one restriction.
     * A filter that is not active would accept every row.
     */
    public boolean isActive() {
        return !text.isEmpty() || requireBaseline || requireSonar || requireJdeo
                || quickMode != QuickMode.NONE;
    }
}
