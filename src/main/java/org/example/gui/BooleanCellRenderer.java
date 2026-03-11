package org.example.gui;

import java.awt.Color;
import java.awt.Component;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Renders boolean table cells as a Unicode check mark (✓) or cross (✗).
 * True → green "✓", false → gray "✗". Cell is center-aligned.
 */
public class BooleanCellRenderer extends DefaultTableCellRenderer {

    private static final Color TRUE_COLOR  = new Color(0, 153, 0);
    private static final Color FALSE_COLOR = Color.LIGHT_GRAY;
    private static final String TICK  = "\u2713";
    private static final String CROSS = "\u2717";

    /** Creates a center-aligned boolean renderer. */
    public BooleanCellRenderer() {
        setHorizontalAlignment(JLabel.CENTER);
    }

    @Override
    public Component getTableCellRendererComponent(
            JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int col) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
        boolean flag = Boolean.TRUE.equals(value);
        setText(flag ? TICK : CROSS);
        setForeground(flag ? TRUE_COLOR : FALSE_COLOR);
        return this;
    }
}
