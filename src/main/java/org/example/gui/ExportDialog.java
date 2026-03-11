package org.example.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import org.example.baseline.CandidateDTO;

/**
 * Modal dialog for exporting analysis results as CSV, JSON, and labeling template.
 * Delegates all I/O to {@link ExportService} and shows an overwrite-confirmation
 * prompt when target files already exist.
 */
public class ExportDialog extends JDialog {

    private final List<CandidateDTO> candidates;
    private final Path               outputDir;
    private final ExportService      exportService = new ExportService();

    private final JCheckBox csvBox      = new JCheckBox("results.csv  — Kandidaten-Tabelle", true);
    private final JCheckBox jsonBox     = new JCheckBox("results.json — Kandidaten-JSON", true);
    private final JCheckBox labelingBox = new JCheckBox("labeling_input.csv — Labeling-Vorlage", true);
    private final JCheckBox reportBox   = new JCheckBox("report.md — Markdown-Report (erst nach Evaluation verfügbar)", false);

    /**
     * Creates the export dialog.
     *
     * @param parent     owner frame
     * @param candidates current analysis candidates
     * @param outputDir  configured output directory
     */
    public ExportDialog(JFrame parent, List<CandidateDTO> candidates, Path outputDir) {
        super(parent, "Ergebnisse exportieren", true);
        this.candidates = candidates;
        this.outputDir  = outputDir;
        reportBox.setEnabled(false);
        buildUi();
        pack();
        setResizable(false);
        setLocationRelativeTo(parent);
    }

    // -------------------------------------------------------------------------
    // UI construction
    // -------------------------------------------------------------------------

    private void buildUi() {
        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        content.add(new JLabel("Zielverzeichnis: " + outputDir), BorderLayout.NORTH);
        content.add(buildCheckPanel(), BorderLayout.CENTER);
        content.add(buildButtonPanel(), BorderLayout.SOUTH);
        add(content);
    }

    private JPanel buildCheckPanel() {
        JPanel panel = new JPanel(new GridLayout(4, 1, 0, 4));
        panel.setBorder(BorderFactory.createTitledBorder("Exportformate"));
        panel.add(csvBox);
        panel.add(jsonBox);
        panel.add(labelingBox);
        panel.add(reportBox);
        return panel;
    }

    private JPanel buildButtonPanel() {
        JButton exportBtn = new JButton("Exportieren");
        JButton cancelBtn = new JButton("Abbrechen");
        exportBtn.addActionListener(e -> onExport());
        cancelBtn.addActionListener(e -> dispose());
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        panel.add(cancelBtn);
        panel.add(exportBtn);
        return panel;
    }

    // -------------------------------------------------------------------------
    // Event handling
    // -------------------------------------------------------------------------

    private void onExport() {
        boolean csv      = csvBox.isSelected();
        boolean json     = jsonBox.isSelected();
        boolean labeling = labelingBox.isSelected();
        if (!csv && !json && !labeling) {
            JOptionPane.showMessageDialog(this,
                    "Mindestens ein Format auswählen.", "Hinweis", JOptionPane.WARNING_MESSAGE);
            return;
        }
        List<Path> existing = exportService.findExisting(outputDir, csv, json, labeling);
        if (!existing.isEmpty() && !confirmOverwrite(existing)) {
            return;
        }
        try {
            List<Path> written = exportService.runExport(outputDir, candidates, csv, json, labeling);
            showSuccess(written);
            dispose();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Export fehlgeschlagen:\n" + ex.getMessage(), "Fehler", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean confirmOverwrite(List<Path> existing) {
        String names = existing.stream()
                .map(p -> "  \u2022 " + p.getFileName())
                .collect(Collectors.joining("\n"));
        int choice = JOptionPane.showConfirmDialog(this,
                "Folgende Dateien existieren bereits:\n" + names + "\n\n\u00dcberschreiben?",
                "\u00dcberschreiben best\u00e4tigen",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        return choice == JOptionPane.YES_OPTION;
    }

    private void showSuccess(List<Path> written) {
        String paths = written.stream()
                .map(p -> "  \u2022 " + p.toAbsolutePath())
                .collect(Collectors.joining("\n"));
        JOptionPane.showMessageDialog(this,
                "Export erfolgreich!\n\n" + paths,
                "Export abgeschlossen", JOptionPane.INFORMATION_MESSAGE);
    }
}
