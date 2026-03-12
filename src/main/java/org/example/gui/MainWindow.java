package org.example.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;
import org.example.baseline.CandidateCsvUtil;
import org.example.baseline.CandidateDTO;
import org.example.labeling.LabelDTO;

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
    private OutputPathPanel  outputPathPanel;
    private ToolConfigPanel  toolConfigPanel;
    private JButton          runBtn;

    // Progress fields
    private JLabel                stepLabel;
    private JTextArea             logArea;
    private JProgressBar          progressBar;
    private JButton               cancelBtn;
    private DockerStatusIndicator dockerIndicator;
    private AnalysisWorker        worker;

    // Results fields
    private JLabel                              summaryLabel;
    private JLabel                              statusBar;
    private JTable                              resultsTable;
    private CandidateTableModel                 candidateTableModel;
    private TableRowSorter<CandidateTableModel> rowSorter;
    private FilterPanel                         filterPanel;
    private AgreementPanel                      agreementPanel;
    private JButton                             exportBtn;
    private List<CandidateDTO>                  currentCandidates = List.of();
    private Path                                currentOutputDir;
    private final LabelPersistenceService       labelPersistenceService = new LabelPersistenceService();

    // Stored totals for status bar (set when new data arrives)
    private int  totalCandidates;
    private long baselineTotal;
    private long sonarTotal;
    private long jdeoTotal;

    /** Constructs and wires the three-card window. */
    public MainWindow() {
        super("CodeSmellsDetector");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) { onWindowClosing(); }
        });
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

        // Project path
        projectPathPanel = new ProjectPathPanel();
        addLabeledPanel(p, g, r++, "Project Root:", projectPathPanel);

        // Output path panel (auto-suggests project name from selected root)
        outputPathPanel = new OutputPathPanel();
        projectPathPanel.addPathChangeListener(outputPathPanel::suggestProjectName);
        g.gridx = 0; g.gridy = r++; g.gridwidth = 3; g.weightx = 1;
        p.add(outputPathPanel, g);
        g.gridwidth = 1; g.weightx = 0;

        // Tool configuration tabs (grows to fill remaining vertical space)
        toolConfigPanel = new ToolConfigPanel();
        g.gridx = 0; g.gridy = r; g.gridwidth = 3; g.weightx = 1; g.weighty = 1;
        g.fill  = GridBagConstraints.BOTH;
        p.add(toolConfigPanel, g);
        g.weighty = 0; g.fill = GridBagConstraints.HORIZONTAL;
        r++;

        // Run button — disabled when no valid project selected or no tool enabled
        runBtn = new JButton("Analyse starten");
        runBtn.addActionListener(e -> onRunAnalysis());
        toolConfigPanel.addEnablementListener(this::updateRunButtonState);
        projectPathPanel.addPathChangeListener(path -> updateRunButtonState());
        updateRunButtonState();

        g.gridx = 0; g.gridy = r; g.gridwidth = 3; g.anchor = GridBagConstraints.CENTER;
        p.add(runBtn, g);
        return p;
    }

    private JPanel buildProgressCard() {
        JPanel p = new JPanel(new BorderLayout(4, 4));
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        stepLabel       = new JLabel("Analyse l\u00e4uft\u2026");
        dockerIndicator = new DockerStatusIndicator();
        JPanel northRow = new JPanel(new java.awt.BorderLayout(8, 0));
        northRow.add(stepLabel,       java.awt.BorderLayout.CENTER);
        northRow.add(dockerIndicator, java.awt.BorderLayout.EAST);
        p.add(northRow, BorderLayout.NORTH);

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        p.add(new JScrollPane(logArea), BorderLayout.CENTER);

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        cancelBtn = new JButton("Abbrechen");
        cancelBtn.setEnabled(false);
        cancelBtn.addActionListener(e -> onCancelOrBack());
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

        candidateTableModel = new CandidateTableModel();
        resultsTable = buildCandidateTable();
        filterPanel = new FilterPanel(this::applyFilter);
        JPanel candidatesTab = new JPanel(new BorderLayout(0, 4));
        candidatesTab.add(filterPanel, BorderLayout.NORTH);
        candidatesTab.add(new JScrollPane(resultsTable), BorderLayout.CENTER);
        candidatesTab.add(buildLabelingToolbar(), BorderLayout.SOUTH);

        agreementPanel = new AgreementPanel();
        JTabbedPane resultsTabs = new JTabbedPane();
        resultsTabs.addTab("Kandidaten", candidatesTab);
        resultsTabs.addTab("Agreement",  agreementPanel);
        p.add(resultsTabs, BorderLayout.CENTER);

        statusBar = new JLabel(" ");
        JButton openBtn = new JButton("Ausgabeordner öffnen");
        openBtn.addActionListener(e -> openOutputFolder());
        exportBtn = new JButton("Exportieren\u2026");
        exportBtn.setEnabled(false);
        exportBtn.addActionListener(e -> new ExportDialog(this, currentCandidates, currentOutputDir).setVisible(true));
        JButton newBtn = new JButton("Neue Analyse");
        newBtn.addActionListener(e -> cardLayout.show(cards, CARD_SETUP));
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(openBtn); buttons.add(exportBtn); buttons.add(newBtn);
        JPanel south = new JPanel(new BorderLayout(4, 0));
        south.add(statusBar, BorderLayout.CENTER);
        south.add(buttons, BorderLayout.EAST);
        p.add(south, BorderLayout.SOUTH);
        return p;
    }

    /** Builds a sortable, color-coded JTable for candidate results. */
    private JTable buildCandidateTable() {
        JTable table = new JTable(candidateTableModel) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int col) {
                Component c = super.prepareRenderer(renderer, row, col);
                if (!isRowSelected(row)) {
                    int modelRow = convertRowIndexToModel(row);
                    Color bg = RowColorRenderer.backgroundFor(candidateTableModel.getRow(modelRow));
                    if (bg != null) { c.setBackground(bg); }
                }
                return c;
            }
        };
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        BooleanCellRenderer boolRenderer = new BooleanCellRenderer();
        table.getColumnModel().getColumn(CandidateTableModel.COL_BASELINE).setCellRenderer(boolRenderer);
        table.getColumnModel().getColumn(CandidateTableModel.COL_SONAR).setCellRenderer(boolRenderer);
        table.getColumnModel().getColumn(CandidateTableModel.COL_JDEO).setCellRenderer(boolRenderer);
        installFinalLabelEditor(table);
        installSorter(table);
        setColumnWidths(table);
        return table;
    }

    private void installSorter(JTable table) {
        rowSorter = new TableRowSorter<>(candidateTableModel);
        Comparator<Integer> nullInt = Comparator.nullsFirst(Integer::compareTo);
        Comparator<Double>  nullDbl = Comparator.nullsFirst(Double::compareTo);
        for (int col : new int[]{
                CandidateTableModel.COL_WMC, CandidateTableModel.COL_ATFD_CBO,
                CandidateTableModel.COL_LOC, CandidateTableModel.COL_METHODS,
                CandidateTableModel.COL_FIELDS, CandidateTableModel.COL_DEPTYPES}) {
            rowSorter.setComparator(col, nullInt);
        }
        rowSorter.setComparator(CandidateTableModel.COL_TCC, nullDbl);
        table.setRowSorter(rowSorter);
    }

    private void installFinalLabelEditor(JTable table) {
        JComboBox<String> combo = new JComboBox<>(
                new String[]{"", "GOD_CLASS", "UNCERTAIN", "NO"});
        table.getColumnModel().getColumn(CandidateTableModel.COL_FINAL_LABEL)
                .setCellEditor(new DefaultCellEditor(combo));
    }

    private void setColumnWidths(JTable table) {
        int[] widths = {300, 65, 75, 85, 50, 55, 65, 50, 60, 55, 70, 35, 35, 35, 35, 150, 90};
        for (int i = 0; i < widths.length && i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
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
        currentOutputDir = outputPathPanel.getResolvedOutputPath();
        try {
            new OutputPathResolver().ensureExists(currentOutputDir);
        } catch (java.io.IOException ex) {
            JOptionPane.showMessageDialog(this, "Output-Verzeichnis konnte nicht erstellt werden:\n" + ex.getMessage(),
                    "Fehler", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String projectName = outputPathPanel.getProjectName();
        if (projectName.isBlank()) {
            projectName = projectRootPath.getFileName().toString();
        }

        var thresholds  = toolConfigPanel.getBaselineThresholds();
        var sonarConfig = toolConfigPanel.getSonarConfig(projectName);
        var jdConfig    = toolConfigPanel.getJDeodorantConfig();

        boolean autoStop = toolConfigPanel.isSonarAutoStopEnabled();

        logArea.setText("");
        stepLabel.setText("Analyse wird vorbereitet\u2026");
        progressBar.setValue(0);
        dockerIndicator.setState(
                sonarConfig != null && sonarConfig.isDockerEnabled()
                        ? DockerStatusIndicator.State.IDLE
                        : DockerStatusIndicator.State.IDLE);
        runBtn.setText("L\u00e4uft\u2026");
        runBtn.setEnabled(false);
        cancelBtn.setText("Abbrechen");
        cancelBtn.setEnabled(true);
        cardLayout.show(cards, CARD_PROGRESS);
        worker = new AnalysisWorker(
                this, projectRootPath, thresholds, sonarConfig, jdConfig,
                currentOutputDir, dockerIndicator, autoStop);
        worker.execute();
    }

    /**
     * Handles the Cancel/Back button depending on current state.
     * During analysis the button cancels the worker; after cancellation it
     * acts as a "Back to configuration" navigation button.
     */
    private void onCancelOrBack() {
        if (worker != null && !worker.isDone()) {
            cancelBtn.setText("Wird abgebrochen\u2026");
            cancelBtn.setEnabled(false);
            worker.cancel(true);
        } else {
            cancelBtn.setEnabled(false);
            cancelBtn.setText("Abbrechen");
            cardLayout.show(cards, CARD_SETUP);
        }
    }

    /** Thread-safe: appends a line to the log area and auto-scrolls. */
    public void appendLog(String line) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(line + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    /** Called on EDT by {@link AnalysisWorker} to update the step label and progress bar. */
    public void updateProgress(String label, int percent) {
        stepLabel.setText(label);
        progressBar.setValue(percent);
    }

    /** Called on EDT by {@link AnalysisWorker#done()} on success. */
    public void onAnalysisComplete(Path outputDir) {
        runBtn.setText("Analyse starten");
        updateRunButtonState();
        cancelBtn.setEnabled(false);
        cancelBtn.setText("Abbrechen");
        progressBar.setValue(100);

        List<CandidateDTO> candidates = parseCandidatesFromCsv(outputDir);
        currentCandidates = candidates;
        totalCandidates = candidates.size();
        baselineTotal   = candidates.stream().filter(CandidateDTO::isBaselineFlag).count();
        sonarTotal      = candidates.stream().filter(CandidateDTO::isSonarFlag).count();
        jdeoTotal       = candidates.stream().filter(CandidateDTO::isJdeodorantFlag).count();
        candidateTableModel.setData(candidates);
        agreementPanel.update(candidates);
        exportBtn.setEnabled(true);
        applyFilter();
        summaryLabel.setText("Analyse abgeschlossen. " + candidates.size() + " Kandidaten gefunden.");
        cardLayout.show(cards, CARD_RESULTS);
    }

    private List<CandidateDTO> parseCandidatesFromCsv(Path outputDir) {
        List<CandidateDTO> result = new ArrayList<>();
        try {
            List<String> lines = Files.readAllLines(outputDir.resolve("results.csv"), StandardCharsets.UTF_8);
            for (int i = 1; i < lines.size(); i++) {
                if (lines.get(i).isBlank()) { continue; }
                CandidateDTO dto = parseCsvRow(CandidateCsvUtil.parseCsvLine(lines.get(i)));
                if (dto != null) { result.add(dto); }
            }
        } catch (Exception e) {
            appendLog("Warnung: results.csv konnte nicht gelesen werden: " + e.getMessage());
        }
        return result;
    }

    private CandidateDTO parseCsvRow(List<String> f) {
        if (f == null || f.size() < 12) { return null; }
        // CSV columns: 0=fqcn, 1=baseline, 2=sonar, 3=jdeo, 4=wmc, 5=tcc, 6=atfd, 7=cbo,
        //              8=loc, 9=godClass, 10=usedCboFallback, 11=methods, 12=fields, 13=deptypes, 14=reasons
        return new CandidateDTO(
                f.get(0),
                "true".equalsIgnoreCase(f.get(1)),
                "true".equalsIgnoreCase(f.get(2)),
                "true".equalsIgnoreCase(f.get(3)),
                parseNullableInt(f.get(4)),
                parseNullableDouble(f.get(5)),
                parseNullableInt(f.get(6)),
                parseNullableInt(f.get(7)),
                parseNullableInt(f.get(8)),
                safeInt(f.size() > 11 ? f.get(11) : ""),
                safeInt(f.size() > 12 ? f.get(12) : ""),
                safeInt(f.size() > 13 ? f.get(13) : ""),
                f.size() > 14 ? parseReasons(f.get(14)) : List.of()
        );
    }

    private void applyFilter() {
        if (rowSorter == null || filterPanel == null) { return; }
        rowSorter.setRowFilter(filterPanel.buildFilter());
        refreshStatusBar();
    }

    private void refreshStatusBar() {
        int visible = resultsTable.getRowCount();
        statusBar.setText(String.format(
                "%d von %d sichtbar  |  %d Baseline  |  %d SonarQube  |  %d JDeodorant (gesamt)",
                visible, totalCandidates, baselineTotal, sonarTotal, jdeoTotal));
    }

    private static Integer parseNullableInt(String s) {
        if (s == null || s.isBlank()) { return null; }
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return null; }
    }

    private static Double parseNullableDouble(String s) {
        if (s == null || s.isBlank()) { return null; }
        try { return Double.parseDouble(s.trim()); } catch (NumberFormatException e) { return null; }
    }

    private static int safeInt(String s) {
        Integer v = parseNullableInt(s);
        return v == null ? 0 : v;
    }

    private static List<String> parseReasons(String s) {
        if (s == null || s.isBlank()) { return List.of(); }
        return List.of(s.split(";"));
    }

    /** Called on EDT by {@link AnalysisWorker#done()} when the user cancelled. */
    public void onAnalysisCancelled() {
        runBtn.setText("Analyse starten");
        updateRunButtonState();
        stepLabel.setText("Analyse abgebrochen.");
        progressBar.setString("Abgebrochen");
        appendLog("--- Analyse wurde abgebrochen ---");
        cancelBtn.setText("Zur\u00fcck zur Konfiguration");
        cancelBtn.setEnabled(true);
    }

    /** Called on EDT by {@link AnalysisWorker#done()} on failure. */
    public void onAnalysisFailed(Throwable error) {
        runBtn.setText("Analyse starten");
        updateRunButtonState();
        cancelBtn.setEnabled(false);
        cancelBtn.setText("Abbrechen");
        JOptionPane.showMessageDialog(this,
                "Analyse fehlgeschlagen:\n" + error.getMessage(), "Fehler", JOptionPane.ERROR_MESSAGE);
        cardLayout.show(cards, CARD_SETUP);
    }

    private void updateRunButtonState() {
        String path = projectPathPanel.getSelectedPath();
        boolean projectValid = !path.isEmpty()
                && new ProjectValidator().validate(Path.of(path)).isValid();
        runBtn.setEnabled(projectValid && toolConfigPanel.isAtLeastOneEnabled());
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
    // Labeling actions
    // -------------------------------------------------------------------------

    private JPanel buildLabelingToolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        JButton autoLabelBtn = new JButton("Auto-Label");
        autoLabelBtn.setToolTipText("Setzt finalLabel aus ≥3/4 Kriterien");
        autoLabelBtn.addActionListener(e -> onAutoLabel());
        JButton saveBtn = new JButton("Speichern");
        saveBtn.setToolTipText("Labels als labeling_input.csv exportieren");
        saveBtn.addActionListener(e -> onSaveLabels());
        JButton loadBtn = new JButton("Laden");
        loadBtn.setToolTipText("Bereits gelabelte CSV importieren");
        loadBtn.addActionListener(e -> onLoadLabels());
        bar.add(autoLabelBtn);
        bar.add(saveBtn);
        bar.add(loadBtn);
        return bar;
    }

    private void onAutoLabel() {
        candidateTableModel.applyAutoLabels();
    }

    private void onSaveLabels() {
        Path defaultPath = currentOutputDir != null
                ? currentOutputDir.resolve("labeling_input.csv")
                : Path.of("labeling_input.csv");
        JFileChooser chooser = new JFileChooser(defaultPath.getParent().toFile());
        chooser.setSelectedFile(defaultPath.toFile());
        chooser.setFileFilter(new FileNameExtensionFilter("CSV-Dateien", "csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) { return; }
        try {
            labelPersistenceService.save(candidateTableModel.getAllLabels(),
                    chooser.getSelectedFile().toPath());
            candidateTableModel.clearDirty();
            JOptionPane.showMessageDialog(this, "Labels gespeichert.", "Gespeichert",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Fehler beim Speichern:\n" + ex.getMessage(),
                    "Fehler", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onLoadLabels() {
        JFileChooser chooser = new JFileChooser(
                currentOutputDir != null ? currentOutputDir.toFile() : new File("."));
        chooser.setFileFilter(new FileNameExtensionFilter("CSV-Dateien", "csv"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) { return; }
        try {
            Map<String, LabelDTO> labels =
                    labelPersistenceService.load(chooser.getSelectedFile().toPath());
            candidateTableModel.loadLabels(labels);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Fehler beim Laden:\n" + ex.getMessage(),
                    "Fehler", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onWindowClosing() {
        if (candidateTableModel.isDirty()) {
            int choice = JOptionPane.showConfirmDialog(this,
                    "Es gibt ungespeicherte Label-Änderungen. Trotzdem beenden?",
                    "Ungespeicherte Änderungen",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (choice != JOptionPane.YES_OPTION) { return; }
        }
        System.exit(0);
    }

    // -------------------------------------------------------------------------
    // Layout helpers
    // -------------------------------------------------------------------------

    /** Adds label + panel spanning the field and button columns. */
    private void addLabeledPanel(JPanel p, GridBagConstraints g, int row,
            String label, JPanel panel) {
        g.gridx = 0; g.gridy = row; g.gridwidth = 1; g.weightx = 0; g.anchor = GridBagConstraints.WEST;
        p.add(new JLabel(label), g);
        g.gridx = 1; g.gridwidth = 2; g.weightx = 1;
        p.add(panel, g);
        g.gridwidth = 1; g.weightx = 0;
    }

}
