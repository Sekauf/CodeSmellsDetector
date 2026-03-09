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
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.example.baseline.BaselineThresholds;
import org.example.baseline.CandidateCsvUtil;
import org.example.jdeodorant.ProjectConfig;
import org.example.sonar.SonarConfig;

/**
 * Three-card GUI: SETUP → PROGRESS → RESULTS.
 * Wraps {@link org.example.orchestrator.AnalysisOrchestrator} via {@link AnalysisWorker}.
 */
public class MainWindow extends JFrame {

    private static final String CARD_SETUP = "SETUP";
    private static final String CARD_PROGRESS = "PROGRESS";
    private static final String CARD_RESULTS = "RESULTS";

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cards = new JPanel(cardLayout);

    // Setup fields
    private ProjectPathPanel projectPathPanel;
    private JTextField outputDirField;
    private JSpinner methodFieldSpinner;
    private JSpinner depTypeSpinner;
    private JCheckBox sonarCheckBox;
    private JTextField sonarHostField;
    private JTextField sonarTokenField;
    private JTextField jdeodorantCsvField;

    // Progress fields
    private JTextArea logArea;
    private JProgressBar progressBar;
    private AnalysisWorker worker;

    // Results fields
    private JLabel summaryLabel;
    private JTable resultsTable;
    private Path currentOutputDir;

    public MainWindow() {
        super("CodeSmellsDetector");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(960, 680);
        setLocationRelativeTo(null);
        cards.add(buildSetupCard(), CARD_SETUP);
        cards.add(buildProgressCard(), CARD_PROGRESS);
        cards.add(buildResultsCard(), CARD_RESULTS);
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
        g.fill = GridBagConstraints.HORIZONTAL;
        int r = 0;

        projectPathPanel = new ProjectPathPanel();
        outputDirField = new JTextField("output");
        addLabeledPanel(p, g, r++, "Project Root:", projectPathPanel);
        addRow(p, g, r++, "Output Dir:",   outputDirField,   browseDir(outputDirField));

        addSection(p, g, r++, "Baseline Thresholds");
        methodFieldSpinner = new JSpinner(new SpinnerNumberModel(40, 1, 9999, 1));
        depTypeSpinner     = new JSpinner(new SpinnerNumberModel(5,  1, 9999, 1));
        JPanel thresh = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        thresh.add(new JLabel("Methods+Fields:")); thresh.add(methodFieldSpinner);
        thresh.add(new JLabel("  DepTypes:"));     thresh.add(depTypeSpinner);
        addFull(p, g, r++, thresh);

        addSection(p, g, r++, "SonarQube (optional)");
        sonarCheckBox = new JCheckBox("Enable SonarQube");
        addFull(p, g, r++, sonarCheckBox);
        sonarHostField  = new JTextField("http://localhost:9000");
        sonarTokenField = new JTextField();
        addRow(p, g, r++, "Host:",  sonarHostField,  null);
        addRow(p, g, r++, "Token:", sonarTokenField, null);

        addSection(p, g, r++, "JDeodorant (optional)");
        jdeodorantCsvField = new JTextField(42);
        addRow(p, g, r++, "CSV Path:", jdeodorantCsvField, browseCsvBtn(jdeodorantCsvField));

        JButton runBtn = new JButton("Run Analysis");
        runBtn.addActionListener(e -> onRunAnalysis());
        g.gridx = 0; g.gridy = r; g.gridwidth = 3; g.anchor = GridBagConstraints.CENTER;
        p.add(runBtn, g);
        return p;
    }

    private JPanel buildProgressCard() {
        JPanel p = new JPanel(new BorderLayout(4, 4));
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        p.add(new JLabel("Analysis in progress…"), BorderLayout.NORTH);

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        p.add(new JScrollPane(logArea), BorderLayout.CENTER);

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> { if (worker != null) worker.cancel(true); });
        JPanel south = new JPanel(new BorderLayout(4, 0));
        south.add(progressBar, BorderLayout.CENTER);
        south.add(cancelBtn, BorderLayout.EAST);
        p.add(south, BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildResultsCard() {
        JPanel p = new JPanel(new BorderLayout(4, 4));
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        summaryLabel = new JLabel("Analysis complete.");
        p.add(summaryLabel, BorderLayout.NORTH);

        resultsTable = new JTable(new ResultsTableModel(List.of()));
        resultsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        p.add(new JScrollPane(resultsTable), BorderLayout.CENTER);

        JButton openBtn = new JButton("Open Output Folder");
        openBtn.addActionListener(e -> openOutputFolder());
        JButton newBtn = new JButton("New Analysis");
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
        Path projectRootPath = Path.of(root);
        String outText = outputDirField.getText().trim();
        currentOutputDir = Path.of(outText.isEmpty() ? "output" : outText);

        BaselineThresholds thresholds = new BaselineThresholds(
                (Integer) methodFieldSpinner.getValue(),
                (Integer) depTypeSpinner.getValue());

        SonarConfig sonarConfig = null;
        if (sonarCheckBox.isSelected()) {
            String host  = sonarHostField.getText().trim();
            String token = sonarTokenField.getText().trim();
            sonarConfig = SonarConfig.builder()
                    .hostUrl(host.isEmpty() ? "http://localhost:9000" : host)
                    .token(token.isEmpty() ? null : token)
                    .projectKey(projectRootPath.getFileName().toString())
                    .build();
        }

        ProjectConfig jdConfig = null;
        String jdCsv = jdeodorantCsvField.getText().trim();
        if (!jdCsv.isEmpty()) {
            jdConfig = ProjectConfig.forJdeodorantCsv(jdCsv);
        }

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
            appendLog("Warning: could not read results.csv: " + e.getMessage());
        }

        summaryLabel.setText(String.format(
                "Analysis complete. Found %d candidates  (Sonar: %d | JDeodorant: %d)",
                rows.size(), sonarCount, jdeoCount));
        resultsTable.setModel(new ResultsTableModel(rows));
        cardLayout.show(cards, CARD_RESULTS);
    }

    /** Called on EDT by {@link AnalysisWorker#done()} on failure. */
    public void onAnalysisFailed(Throwable error) {
        progressBar.setIndeterminate(false);
        JOptionPane.showMessageDialog(this,
                "Analysis failed:\n" + error.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        cardLayout.show(cards, CARD_SETUP);
    }

    private void openOutputFolder() {
        File dir = (currentOutputDir != null) ? currentOutputDir.toFile() : new File("output");
        try {
            Desktop.getDesktop().open(dir.exists() ? dir : new File("."));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Cannot open folder: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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

    private JButton browseCsvBtn(JTextField field) {
        JButton btn = new JButton("Browse…");
        btn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new FileNameExtensionFilter("CSV files", "csv"));
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                field.setText(fc.getSelectedFile().getAbsolutePath());
            }
        });
        return btn;
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

    /** Adds a section title + horizontal separator spanning all columns. */
    private void addSection(JPanel p, GridBagConstraints g, int row, String title) {
        g.gridx = 0; g.gridy = row; g.gridwidth = 3; g.weightx = 1;
        JPanel sep = new JPanel(new BorderLayout(6, 0));
        JLabel lbl = new JLabel(title);
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
        sep.add(lbl, BorderLayout.WEST);
        sep.add(new JSeparator(), BorderLayout.CENTER);
        p.add(sep, g);
        g.gridwidth = 1; g.weightx = 0;
    }

    private void addFull(JPanel p, GridBagConstraints g, int row, JComponent inner) {
        g.gridx = 0; g.gridy = row; g.gridwidth = 3;
        p.add(inner, g);
        g.gridwidth = 1;
    }
}
