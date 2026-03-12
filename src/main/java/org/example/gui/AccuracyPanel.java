package org.example.gui;

import java.awt.BorderLayout;
import java.util.Map;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import org.example.metrics.EvaluationMetrics;

/**
 * Displays a summary table of evaluation metrics (P/R/F1/Specificity/MCC/TP/FP/FN/TN)
 * per tool after evaluation has been run.
 */
public class AccuracyPanel extends JPanel {

    private static final String[] COLS = {
        "Tool", "Precision", "Recall", "F1", "Specificity", "MCC", "TP", "FP", "FN", "TN"
    };
    private static final String[] TOOLS = {"baseline", "sonar", "jdeodorant"};

    private final DefaultTableModel tableModel;

    /** Creates an empty AccuracyPanel. */
    public AccuracyPanel() {
        super(new BorderLayout());
        tableModel = new DefaultTableModel(COLS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(tableModel);
        table.setFillsViewportHeight(true);
        table.setRowHeight(22);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    /**
     * Replaces the table content with the given metrics.
     *
     * @param metrics map of tool name → EvaluationMetrics; null or empty clears the table
     */
    public void update(Map<String, EvaluationMetrics> metrics) {
        tableModel.setRowCount(0);
        if (metrics == null || metrics.isEmpty()) { return; }
        for (String tool : TOOLS) {
            EvaluationMetrics m = metrics.get(tool);
            if (m == null) { continue; }
            tableModel.addRow(buildRow(tool, m));
        }
    }

    private Object[] buildRow(String tool, EvaluationMetrics m) {
        return new Object[]{
            tool,
            String.format("%.3f", m.getPrecision()),
            String.format("%.3f", m.getRecall()),
            String.format("%.3f", m.getF1Score()),
            String.format("%.3f", m.getSpecificity()),
            String.format("%.4f", m.getMcc()),
            m.getTruePositives(),
            m.getFalsePositives(),
            m.getFalseNegatives(),
            m.getTrueNegatives()
        };
    }
}
