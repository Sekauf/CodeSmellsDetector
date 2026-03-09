package org.example.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import org.example.baseline.CandidateCsvUtil;

/**
 * Three-card GUI: SETUP → PROGRESS → RESULTS.
 * Wraps {@link org.example.orchestrator.AnalysisOrchestrator} via {@link AnalysisWorker}.
 */
public class MainWindow extends JFrame {

    private static final String CARD_SETUP    = "SETUP";
    private static final String CARD_PROGRESS = "PROGRESS";
    private static final String CARD_RESULTS  = "RESULTS";

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel     cards      = new JPanel(cardLayout);

    // Setup fields
    private ProjectPathPanel projectPathPanel;
    private JTextField       outputDirField;
    private ToolConfigPanel  toolConfigPanel;
    private JButton          runBtn;

    // Progress fields
    private JTextArea    logArea;
    private JProgressBar progressBar;
    private AnalysisWorker worker;

    // Results fields
    private JLabel summaryLabel;
    private JTable resultsTable;
    private Path   currentOutputDir;

    /** Constructs and wires the three-card window. */
    public MainWindow() {
        super("CodeSmellsDetector");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(960, 720);
        setLocationRelativeTo(null);
        cards.add(buildSetupCard(),    CARD_SETUP);
        cards.add(buildProgressCard(), CARD_PROGRESS);
        cards.add(buildResultsCard(),  CARD_RESULTS);
        add(cards);
    }

    // -------------------------------------------------------------------------
    // Card builders
    // -------------------------------------------------------------------------

    private JPanel buildSetupCard() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(3, 4, 3, 4);
        g.fill   = GridBagConstraints.HORIZONTAL;
        int r = 0;

        // Project path + output dir
        projectPathPanel = new ProjectPathPanel();
        outputDirField   = new JTextField("output");
        addLabeledPanel(p, g, r++, "Project Root:", projectPathPanel);
        addRow(p, g, r++, "Output Dir:",   outputDirField, browseDir(outputDirField));

        // Tool configuration tabs (grows to fill remaining vertical space)
        toolConfigPanel = new ToolConfigPanel();
        g.gridx = 0; g.gridy = r; g.gridwidth = 3; g.weightx = 1; g.weighty = 1;
        g.fill  = GridBagConstraints.BOTH;
        p.add(toolConfigPanel, g);
        g.weighty = 0; g.fill = GridBagConstraints.HORIZONTAL;
        r++;

        // Run button — disabled when no tool is enabled
        runBtn = new JButton("Analyse starten");
        runBtn.addActionListener(e -> onRunAnalysis());
        toolConfigPanel.addEnablementListener(this::updateRunButtonState);
        updateRunButtonState();

        g.gridx = 0; g.gridy = r; g.gridwidth = 3; g.anchor = GridBagConstraints.CENTER;
        p.add(runBtn, g);
        return p;
    }

    private JPanel buildProgressCard() {
        JPanel p = new JPanel(new BorderLayout(4, 4));
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        p.add(new JLabel("Analyse läuft\u2026"), BorderLayout.NORTH);

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        p.add(new JScrollPane(logArea), BorderLayout.CENTER);

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        JButton cancelBtn = new JButton("Abbrechen");
        cancelBtn.addActionListener(e -> { if (worker != null) worker.cancel(true); });
        JPanel south = new JPanel(new BorderLayout(4, 0));
        south.add(progressBar, BorderLayout.CENTER);
        south.add(cancelBtn,   BorderLayout.EAST);
        p.add(south, BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildResultsCard() {
        JPanel p = new JPanel(new BorderLayout(4, 4));
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        summaryLabel = new JLabel("Analyse abgeschlossen.");
        p.add(summaryLabel, BorderLayout.NORTH);

        resultsTable = new JTable(new ResultsTableModel(List.of()));
        resultsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        p.add(new JScrollPane(resultsTable), BorderLayout.CENTER);

        JButton openBtn = new JButton("Ausgabeordner öffnen");
        openBtn.addActionListener(e -> openOutputFolder());
        JButton newBtn = new JButton("Neue Analyse");
        newBtn.addActionListener(e -> cardLayout.show(cards, CARD_SETUP));
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.add(openBtn); south.add(newBtn);
        p.add(south, BorderLayout.SOUTH);
        return p;
    }

    // -------------------------------------------------------------------------
    // Event handlers
    // -------------------------------------------------------------------------

    private void onRunAnalysis() {
        String root = projectPathPanel.getSelectedPath();
        if (root.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Project Root ist erforderlich.", "Validierung", JOptionPane.ERROR_MESSAGE);
            return;
        }
        ProjectValidator.Result validation = projectPathPanel.validatePath();
        if (!validation.isValid()) {
            JOptionPane.showMessageDialog(this, validation.message(), "Ungültiges Projektverzeichnis", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!toolConfigPanel.isAtLeastOneEnabled()) {
            JOptionPane.showMessageDialog(this, "Mindestens ein Tool muss aktiviert sein.", "Konfiguration", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Path projectRootPath = Path.of(root);
        String outText = outputDirField.getText().trim();
        currentOutputDir = Path.of(outText.isEmpty() ? "output" : outText);

        var thresholds  = toolConfigPanel.getBaselineThresholds();
        var sonarConfig = toolConfigPanel.getSonarConfig(projectRootPath.getFileName().toString());
        var jdConfig    = toolConfigPanel.getJDeodorantConfig();

        logArea.setText("");
        cardLayout.show(cards, CARD_PROGRESS);
        worker = new AnalysisWorker(this, projectRootPath, thresholds, sonarConfig, jdConfig, currentOutputDir);
        worker.execute();
    }

    /** Thread-safe: appends a line to the log area and auto-scrolls. */
    public void appendLog(String line) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(line + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    /** Called on EDT by {@link AnalysisWorker#done()} on success. */
    public void onAnalysisComplete(Path outputDir) {
        progressBar.setIndeterminate(false);
        progressBar.setValue(100);

        List<String[]> rows = new ArrayList<>();
        int sonarCount = 0, jdeoCount = 0;
        try {
            List<String> lines = Files.readAllLines(outputDir.resolve("results.csv"), StandardCharsets.UTF_8);
            for (int i = 1; i < lines.size(); i++) {
                if (lines.get(i).isBlank()) continue;
                List<String> fields = CandidateCsvUtil.parseCsvLine(lines.get(i));
                String[] row = fields.toArray(new String[0]);
                rows.add(row);
                if (fields.size() > 2 && "true".equalsIgnoreCase(fields.get(2))) sonarCount++;
                if (fields.size() > 3 && "true".equalsIgnoreCase(fields.get(3))) jdeoCount++;
            }
        } catch (Exception e) {
            appendLog("Warnung: results.csv konnte nicht gelesen werden: " + e.getMessage());
        }

        summaryLabel.setText(String.format(
                "Analyse abgeschlossen. %d Kandidaten gefunden  (Sonar: %d | JDeodorant: %d)",
                rows.size(), sonarCount, jdeoCount));
        resultsTable.setModel(new ResultsTableModel(rows));
        cardLayout.show(cards, CARD_RESULTS);
    }

    /** Called on EDT by {@link AnalysisWorker#done()} on failure. */
    public void onAnalysisFailed(Throwable error) {
        progressBar.setIndeterminate(false);
        JOptionPane.showMessageDialog(this,
                "Analyse fehlgeschlagen:\n" + error.getMessage(), "Fehler", JOptionPane.ERROR_MESSAGE);
        cardLayout.show(cards, CARD_SETUP);
    }

    private void updateRunButtonState() {
        runBtn.setEnabled(toolConfigPanel.isAtLeastOneEnabled());
    }

    private void openOutputFolder() {
        File dir = (currentOutputDir != null) ? currentOutputDir.toFile() : new File("output");
        try {
            Desktop.getDesktop().open(dir.exists() ? dir : new File("."));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Ordner kann nicht geöffnet werden: " + ex.getMessage(), "Fehler", JOptionPane.ERROR_MESSAGE);
        }
    }

    // -------------------------------------------------------------------------
    // Layout helpers
    // -------------------------------------------------------------------------

    private JButton browseDir(JTextField field) {
        JButton btn = new JButton("Browse\u2026");
        btn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                field.setText(fc.getSelectedFile().getAbsolutePath());
            }
        });
        return btn;
    }

    /** Adds label + panel spanning the field and button columns. */
    private void addLabeledPanel(JPanel p, GridBagConstraints g, int row,
            String label, JPanel panel) {
        g.gridx = 0; g.gridy = row; g.gridwidth = 1; g.weightx = 0; g.anchor = GridBagConstraints.WEST;
        p.add(new JLabel(label), g);
        g.gridx = 1; g.gridwidth = 2; g.weightx = 1;
        p.add(panel, g);
        g.gridwidth = 1; g.weightx = 0;
    }

    /** Adds label + field + optional button in a single row. */
    private void addRow(JPanel p, GridBagConstraints g, int row,
            String label, JTextField field, JButton btn) {
        g.gridx = 0; g.gridy = row; g.gridwidth = 1; g.weightx = 0; g.anchor = GridBagConstraints.WEST;
        p.add(new JLabel(label), g);
        g.gridx = 1; g.weightx = 1;
        p.add(field, g);
        g.gridx = 2; g.weightx = 0;
        p.add(btn != null ? btn : new JLabel(), g);
    }
}
