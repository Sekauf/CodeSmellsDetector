package org.example.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import org.example.baseline.BaselineThresholds;

/**
 * Configuration tab for the Baseline God-Class detector.
 * <p>
 * Shows the two active thresholds (Methods+Fields, DepTypes) that are passed to
 * {@link BaselineThresholds}, plus four read-only Exposé reference values
 * (WMC, TCC, ATFD, CBO) for documentation purposes.
 */
public class BaselineConfigTab extends JPanel {

    // Active threshold defaults (wired to BaselineThresholds)
    private static final int DEFAULT_METHOD_PLUS_FIELD = 40;
    private static final int DEFAULT_DEPENDENCY_TYPE   = 5;

    // Exposé reference defaults (informational only, not passed to analysis)
    private static final int    DEFAULT_WMC  = 47;
    private static final double DEFAULT_TCC  = 0.33;
    private static final int    DEFAULT_ATFD = 5;
    private static final int    DEFAULT_CBO  = 20;

    private final JCheckBox enableCheckBox;
    private final JSpinner  methodFieldSpinner;
    private final JSpinner  depTypeSpinner;
    private final JSpinner  wmcSpinner;
    private final JSpinner  tccSpinner;
    private final JSpinner  atfdSpinner;
    private final JSpinner  cboSpinner;

    /** Constructs the tab with pre-filled default values. */
    public BaselineConfigTab() {
        super(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        enableCheckBox     = new JCheckBox("Baseline aktivieren", true);
        methodFieldSpinner = new JSpinner(new SpinnerNumberModel(DEFAULT_METHOD_PLUS_FIELD, 1, 9999, 1));
        depTypeSpinner     = new JSpinner(new SpinnerNumberModel(DEFAULT_DEPENDENCY_TYPE,   1, 9999, 1));
        wmcSpinner         = new JSpinner(new SpinnerNumberModel(DEFAULT_WMC,  1,   9999, 1));
        tccSpinner         = new JSpinner(new SpinnerNumberModel(DEFAULT_TCC,  0.0, 1.0,  0.01));
        atfdSpinner        = new JSpinner(new SpinnerNumberModel(DEFAULT_ATFD, 1,   9999, 1));
        cboSpinner         = new JSpinner(new SpinnerNumberModel(DEFAULT_CBO,  1,   9999, 1));

        buildLayout();
        enableCheckBox.addItemListener(e -> updateFieldState());
    }

    /**
     * Returns whether the Baseline tool is enabled.
     *
     * @return {@code true} if the enable checkbox is selected
     */
    public boolean isEnabled() {
        return enableCheckBox.isSelected();
    }

    /**
     * Builds a {@link BaselineThresholds} from the active spinner values.
     *
     * @return thresholds for the current configuration
     */
    public BaselineThresholds getThresholds() {
        return new BaselineThresholds(
                (Integer) methodFieldSpinner.getValue(),
                (Integer) depTypeSpinner.getValue());
    }

    /**
     * Registers a listener that is called whenever the enable checkbox changes.
     *
     * @param listener runnable to invoke on state change
     */
    public void addEnablementListener(Runnable listener) {
        enableCheckBox.addItemListener(e -> listener.run());
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void buildLayout() {
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(3, 4, 3, 4);
        g.fill   = GridBagConstraints.HORIZONTAL;
        int r = 0;

        // Enable row
        g.gridx = 0; g.gridy = r++; g.gridwidth = 2; g.weightx = 1;
        add(enableCheckBox, g);
        g.gridwidth = 1; g.weightx = 0;

        // Active thresholds section
        addSectionLabel("Aktive Schwellenwerte", r++, g);
        addRow("Methods + Fields ≥",  methodFieldSpinner, "Anzahl Methoden+Felder-Kombinationen", r++, g);
        addRow("Dependency Types ≥",  depTypeSpinner,     "Anzahl verschiedener Abhängigkeitstypen", r++, g);

        // Exposé reference section
        addSectionLabel("Exposé-Referenzwerte (informativ)", r++, g);
        addRow("WMC \u2265",  wmcSpinner,  "Weighted Method Count \u226547 (God-Class-Kriterium)", r++, g);
        addRow("TCC <",       tccSpinner,  "Tight Class Cohesion <0,33",                            r++, g);
        addRow("ATFD >",      atfdSpinner, "Access To Foreign Data >5",                             r++, g);
        addRow("CBO \u2265",  cboSpinner,  "Coupling Between Objects \u226520",                     r++, g);
    }

    private void addSectionLabel(String text, int row, GridBagConstraints g) {
        g.gridx = 0; g.gridy = row; g.gridwidth = 2; g.weightx = 1;
        JLabel lbl = new JLabel(text);
        lbl.setFont(lbl.getFont().deriveFont(java.awt.Font.BOLD));
        lbl.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, java.awt.Color.GRAY));
        add(lbl, g);
        g.gridwidth = 1; g.weightx = 0;
    }

    private void addRow(String label, JSpinner spinner, String tooltip, int row, GridBagConstraints g) {
        JLabel lbl = new JLabel(label);
        lbl.setToolTipText(tooltip);
        spinner.setToolTipText(tooltip);

        g.gridx = 0; g.gridy = row; g.weightx = 0;
        add(lbl, g);
        g.gridx = 1; g.weightx = 1;
        add(spinner, g);
    }

    private void updateFieldState() {
        boolean on = enableCheckBox.isSelected();
        methodFieldSpinner.setEnabled(on);
        depTypeSpinner.setEnabled(on);
        wmcSpinner.setEnabled(on);
        tccSpinner.setEnabled(on);
        atfdSpinner.setEnabled(on);
        cboSpinner.setEnabled(on);
    }
}
