package org.example.gui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import org.example.metrics.ReliabilityMetrics;

/**
 * Displays inter-rater reliability metrics (observed agreement, Cohen's κ, Gwet's AC1)
 * computed from a primary and a second-reviewer CSV.
 */
public class ReliabilityPanel extends JPanel {

    private static final String NO_DATA = "–";
    private static final String PENDING = "Kein zweiter Reviewer geladen.";

    private final JLabel statusLabel      = new JLabel(PENDING, SwingConstants.CENTER);
    private final JLabel agreementLabel   = new JLabel(NO_DATA);
    private final JLabel kappaLabel       = new JLabel(NO_DATA);
    private final JLabel ac1Label         = new JLabel(NO_DATA);
    private final JLabel countsLabel      = new JLabel(NO_DATA);

    /** Creates an empty ReliabilityPanel. */
    public ReliabilityPanel() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        add(statusLabel, BorderLayout.NORTH);
        add(buildMetricsGrid(), BorderLayout.CENTER);
    }

    private JPanel buildMetricsGrid() {
        JPanel grid = new JPanel(new GridLayout(4, 2, 8, 8));
        grid.add(bold("Beobachtete Übereinstimmung:"));   grid.add(agreementLabel);
        grid.add(bold("Cohen's κ (Kappa):"));             grid.add(kappaLabel);
        grid.add(bold("Gwet's AC1:"));                    grid.add(ac1Label);
        grid.add(bold("Counts (++/+-/-+/--):")); grid.add(countsLabel);
        return grid;
    }

    private JLabel bold(String text) {
        JLabel l = new JLabel(text);
        l.setFont(l.getFont().deriveFont(java.awt.Font.BOLD));
        return l;
    }

    /**
     * Updates the panel with the given reliability metrics.
     * Passing {@code null} resets the panel to its initial state.
     *
     * @param m reliability metrics, or null when no second review is available
     */
    public void update(ReliabilityMetrics m) {
        if (m == null) {
            statusLabel.setText(PENDING);
            agreementLabel.setText(NO_DATA);
            kappaLabel.setText(NO_DATA);
            ac1Label.setText(NO_DATA);
            countsLabel.setText(NO_DATA);
            return;
        }
        statusLabel.setText("Reliabilität berechnet.");
        agreementLabel.setText(String.format("%.3f (%.0f%%)",
                m.getObservedAgreement(), m.getObservedAgreement() * 100));
        kappaLabel.setText(String.format("%.4f", m.getKappa()));
        ac1Label.setText(String.format("%.4f", m.getAc1()));
        countsLabel.setText(m.getBothPositive() + " / "
                + m.getRater1PositiveRater2Negative() + " / "
                + m.getRater1NegativeRater2Positive() + " / "
                + m.getBothNegative());
    }
}
