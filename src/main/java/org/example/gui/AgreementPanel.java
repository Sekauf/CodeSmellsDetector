package org.example.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import org.example.baseline.CandidateDTO;

/**
 * Results tab panel displaying tool-agreement data:
 * a Jaccard matrix (3×3), pairwise detail statistics,
 * and a graphical 3-circle Venn diagram.
 */
public class AgreementPanel extends JPanel {

    private static final String[] TOOLS = {"Baseline", "SonarQube", "JDeodorant"};

    private final AgreementCalculator calculator = new AgreementCalculator();
    private final DefaultTableModel   jaccardModel;
    private final DefaultTableModel   detailModel;
    private final VennDiagramPanel    vennPanel;

    /** Constructs the panel with empty initial state. */
    public AgreementPanel() {
        super(new BorderLayout(4, 4));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        jaccardModel = buildJaccardModel();
        detailModel  = buildDetailModel();
        vennPanel    = new VennDiagramPanel();

        JPanel topRow = new JPanel(new GridLayout(1, 2, 8, 0));
        topRow.add(buildJaccardSection());
        topRow.add(buildDetailSection());

        add(topRow,   BorderLayout.NORTH);
        add(vennPanel, BorderLayout.CENTER);
    }

    /**
     * Refreshes all sub-components with data from the given candidate list.
     *
     * @param candidates current analysis candidates; null clears display
     */
    public void update(List<CandidateDTO> candidates) {
        List<AgreementCalculator.PairStats> pairs = calculator.computePairs(candidates);
        AgreementCalculator.VennCounts venn = calculator.computeVenn(candidates);
        updateJaccardMatrix(pairs);
        updateDetailTable(pairs);
        vennPanel.update(venn);
    }

    // -------------------------------------------------------------------------
    // Section builders
    // -------------------------------------------------------------------------

    private JPanel buildJaccardSection() {
        JTable table = new JTable(jaccardModel);
        table.setEnabled(false);
        table.setRowHeight(24);
        table.getTableHeader().setReorderingAllowed(false);
        installJaccardRenderer(table);

        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setBorder(BorderFactory.createTitledBorder("Jaccard-Matrix"));
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildDetailSection() {
        JTable table = new JTable(detailModel);
        table.setEnabled(false);
        table.setRowHeight(22);
        table.getTableHeader().setReorderingAllowed(false);

        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setBorder(BorderFactory.createTitledBorder("Paarweise Übersicht"));
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    // -------------------------------------------------------------------------
    // Model factories
    // -------------------------------------------------------------------------

    private DefaultTableModel buildJaccardModel() {
        String[] cols = {"", "Baseline", "SonarQube", "JDeodorant"};
        Object[][] rows = new Object[TOOLS.length][cols.length];
        for (int i = 0; i < TOOLS.length; i++) {
            rows[i][0] = TOOLS[i];
            for (int j = 1; j < cols.length; j++) {
                rows[i][j] = i == (j - 1) ? "1.00" : "—";
            }
        }
        return new DefaultTableModel(rows, cols) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    }

    private DefaultTableModel buildDetailModel() {
        String[] cols = {"Tool A", "Tool B", "Beide", "Nur A", "Nur B", "Jaccard"};
        Object[][] rows = new Object[3][cols.length];
        for (int i = 0; i < 3; i++) {
            rows[i] = new Object[]{"—", "—", 0, 0, 0, "—"};
        }
        return new DefaultTableModel(rows, cols) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    }

    // -------------------------------------------------------------------------
    // Update helpers
    // -------------------------------------------------------------------------

    private void updateJaccardMatrix(List<AgreementCalculator.PairStats> pairs) {
        // Build symmetric 3×3 Jaccard lookup
        double[][] j = new double[3][3];
        for (int k = 0; k < 3; k++) { j[k][k] = 1.0; }
        int[][] pairIndices = {{0, 1}, {0, 2}, {1, 2}};
        for (int k = 0; k < pairs.size(); k++) {
            int a = pairIndices[k][0], b = pairIndices[k][1];
            j[a][b] = j[b][a] = pairs.get(k).jaccard();
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                jaccardModel.setValueAt(String.format("%.2f", j[row][col]), row, col + 1);
            }
        }
    }

    private void updateDetailTable(List<AgreementCalculator.PairStats> pairs) {
        for (int i = 0; i < pairs.size(); i++) {
            AgreementCalculator.PairStats p = pairs.get(i);
            detailModel.setValueAt(p.toolA(),                    i, 0);
            detailModel.setValueAt(p.toolB(),                    i, 1);
            detailModel.setValueAt(p.both(),                     i, 2);
            detailModel.setValueAt(p.onlyA(),                    i, 3);
            detailModel.setValueAt(p.onlyB(),                    i, 4);
            detailModel.setValueAt(String.format("%.3f", p.jaccard()), i, 5);
        }
    }

    // -------------------------------------------------------------------------
    // Cell renderer
    // -------------------------------------------------------------------------

    private void installJaccardRenderer(JTable table) {
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object value, boolean selected, boolean focused, int row, int col) {
                super.getTableCellRendererComponent(t, value, selected, focused, row, col);
                setHorizontalAlignment(SwingConstants.CENTER);
                if (col == 0) {
                    setBackground(new Color(230, 230, 230));
                    setFont(getFont().deriveFont(java.awt.Font.BOLD));
                    return this;
                }
                if (!selected && value instanceof String s && !s.equals("—")) {
                    try {
                        setBackground(colorForJaccard(Double.parseDouble(s)));
                    } catch (NumberFormatException ignored) {
                        setBackground(Color.WHITE);
                    }
                }
                return this;
            }
        };
        for (int col = 0; col < table.getColumnCount(); col++) {
            table.getColumnModel().getColumn(col).setCellRenderer(renderer);
        }
    }

    private static Color colorForJaccard(double j) {
        float ratio = (float) Math.min(1.0, Math.max(0.0, j));
        int r, g;
        if (ratio < 0.5f) {
            r = 255;
            g = (int) (ratio * 2 * 180);
        } else {
            r = (int) ((1 - ratio) * 2 * 255);
            g = 180;
        }
        return new Color(r, g, 80);
    }
}
