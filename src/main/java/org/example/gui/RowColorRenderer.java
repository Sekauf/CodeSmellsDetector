package org.example.gui;

import java.awt.Color;
import org.example.baseline.CandidateDTO;

/**
 * Provides row background colors for the candidate results table.
 *
 * <ul>
 *   <li>≥ 2 tool flags → {@link #TWO_TOOLS_COLOR} (orange tint)</li>
 *   <li>baseline flag only (1 total flag) → {@link #GOD_CLASS_COLOR} (red tint)</li>
 *   <li>otherwise → {@code null} (use the table's default background)</li>
 * </ul>
 */
public final class RowColorRenderer {

    /** Light red used for rows where only the baseline detected the class. */
    static final Color GOD_CLASS_COLOR = new Color(255, 200, 200);

    /** Light orange used for rows detected by two or more tools. */
    static final Color TWO_TOOLS_COLOR = new Color(255, 220, 160);

    private RowColorRenderer() { }

    /**
     * Returns the background color for the given candidate, or {@code null} for the default.
     *
     * @param dto the candidate to evaluate
     * @return color, or {@code null} if no special coloring applies
     */
    public static Color backgroundFor(CandidateDTO dto) {
        int flags = countFlags(dto);
        if (flags >= 2) {
            return TWO_TOOLS_COLOR;
        }
        if (dto.isBaselineFlag()) {
            return GOD_CLASS_COLOR;
        }
        return null;
    }

    private static int countFlags(CandidateDTO dto) {
        int count = 0;
        if (dto.isBaselineFlag())   { count++; }
        if (dto.isSonarFlag())      { count++; }
        if (dto.isJdeodorantFlag()) { count++; }
        return count;
    }
}
