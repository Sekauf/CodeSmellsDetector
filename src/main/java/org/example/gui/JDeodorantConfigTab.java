package org.example.gui;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Configuration tab for the JDeodorant God-Class detector.
 * <p>
 * Allows selecting a pre-exported JDeodorant CSV result file.
 * The path is validated to confirm the file exists and ends with {@code .csv}.
 */
public class JDeodorantConfigTab extends JPanel {

    private final JCheckBox  enableCheckBox;
    private final JTextField csvPathField;
    private final JLabel     validationLabel;

    /** Constructs the tab. JDeodorant is disabled by default. */
    public JDeodorantConfigTab() {
        super(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        enableCheckBox  = new JCheckBox("JDeodorant aktivieren", false);
        csvPathField    = new JTextField();
        validationLabel = new JLabel("  ");

        buildLayout();
        enableCheckBox.addItemListener(e -> updateFieldState());
        csvPathField.getDocument().addDocumentListener(
                new javax.swing.event.DocumentListener() {
                    public void insertUpdate(javax.swing.event.DocumentEvent e)  { validateCsv(); }
                    public void removeUpdate(javax.swing.event.DocumentEvent e)  { validateCsv(); }
                    public void changedUpdate(javax.swing.event.DocumentEvent e) { validateCsv(); }
                });
        updateFieldState();
    }

    /**
     * Returns whether JDeodorant is enabled.
     *
     * @return {@code true} if the enable checkbox is selected
     */
    public boolean isEnabled() {
        return enableCheckBox.isSelected();
    }

    /**
     * Returns the CSV path if enabled and non-empty, otherwise {@code null}.
     *
     * @return absolute CSV path string or {@code null}
     */
    public String getCsvPath() {
        if (!isEnabled()) {
            return null;
        }
        String path = csvPathField.getText().trim();
        return path.isEmpty() ? null : path;
    }

    /**
     * Registers a listener called whenever the enable checkbox changes.
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
        g.gridx = 0; g.gridy = r++; g.gridwidth = 3; g.weightx = 1;
        add(enableCheckBox, g);
        g.gridwidth = 1; g.weightx = 0;

        // Explanation
        g.gridx = 0; g.gridy = r++; g.gridwidth = 3;
        JLabel hint = new JLabel(
                "<html>CSV-Export aus JDeodorant (God Class Refactoring → Export Results).</html>");
        hint.setForeground(java.awt.Color.GRAY);
        add(hint, g);
        g.gridwidth = 1;

        // CSV path row
        g.gridx = 0; g.gridy = r; g.weightx = 0;
        add(new JLabel("CSV-Pfad:"), g);
        g.gridx = 1; g.weightx = 1;
        add(csvPathField, g);
        g.gridx = 2; g.weightx = 0;

        JPanel btnPanel = new JPanel(new BorderLayout(4, 0));
        JButton browseBtn = new JButton("Durchsuchen\u2026");
        browseBtn.addActionListener(e -> onBrowse());
        btnPanel.add(browseBtn, BorderLayout.WEST);
        btnPanel.add(validationLabel, BorderLayout.EAST);
        add(btnPanel, g);
    }

    private void onBrowse() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("CSV-Dateien (*.csv)", "csv"));
        String current = csvPathField.getText().trim();
        if (!current.isEmpty()) {
            fc.setCurrentDirectory(new File(current).getParentFile());
        }
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            csvPathField.setText(fc.getSelectedFile().getAbsolutePath());
        }
    }

    private void validateCsv() {
        String path = csvPathField.getText().trim();
        if (path.isEmpty()) {
            validationLabel.setText("  ");
            validationLabel.setToolTipText(null);
            return;
        }
        File f = new File(path);
        if (f.isFile() && f.getName().endsWith(".csv")) {
            validationLabel.setText("\u2714");
            validationLabel.setForeground(new java.awt.Color(0, 150, 0));
            validationLabel.setToolTipText("CSV-Datei gefunden.");
        } else {
            validationLabel.setText("\u2718");
            validationLabel.setForeground(new java.awt.Color(180, 0, 0));
            validationLabel.setToolTipText("Datei nicht gefunden oder kein .csv-Format.");
        }
    }

    private void updateFieldState() {
        boolean on = enableCheckBox.isSelected();
        csvPathField.setEnabled(on);
    }
}
