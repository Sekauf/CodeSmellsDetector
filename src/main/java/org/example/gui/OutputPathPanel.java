package org.example.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.nio.file.Path;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Panel for configuring the output directory and project name.
 * <p>
 * The project name is auto-suggested from the selected project path (editable).
 * A dynamic info label shows the resolved path {@code <outputDir>/<projectName>/}.
 */
public class OutputPathPanel extends JPanel {

    private static final String DEFAULT_OUTPUT_DIR = "output";

    private final JTextField         outputDirField;
    private final JTextField         projectNameField;
    private final JLabel             resolvedPathLabel;
    private final OutputPathResolver resolver = new OutputPathResolver();

    /** Constructs the panel with default output directory {@code "output"}. */
    public OutputPathPanel() {
        super(new GridBagLayout());

        outputDirField    = new JTextField(DEFAULT_OUTPUT_DIR);
        projectNameField  = new JTextField();
        resolvedPathLabel = new JLabel(" ");
        resolvedPathLabel.setForeground(java.awt.Color.GRAY);

        buildLayout();

        DocumentListener updater = new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { updateLabel(); }
            public void removeUpdate(DocumentEvent e)  { updateLabel(); }
            public void changedUpdate(DocumentEvent e) { updateLabel(); }
        };
        outputDirField.getDocument().addDocumentListener(updater);
        projectNameField.getDocument().addDocumentListener(updater);
    }

    /**
     * Suggests a project name derived from the given project root path.
     * Called externally when the user selects a project directory.
     * Only updates the field if the user has not manually edited it,
     * i.e., the field is blank or matches the previous auto-suggestion.
     *
     * @param projectRootPath absolute path of the selected project root
     */
    public void suggestProjectName(String projectRootPath) {
        String suggested = resolver.suggestProjectName(projectRootPath);
        if (!suggested.isBlank()) {
            projectNameField.setText(suggested);
        }
    }

    /**
     * Returns the effective output directory for the analysis.
     * The directory is created automatically when the analysis starts via
     * {@link OutputPathResolver#ensureExists(Path)}.
     *
     * @return resolved path {@code <outputDir>/<projectName>}
     */
    public Path getResolvedOutputPath() {
        return resolver.resolve(outputDirField.getText(), projectNameField.getText());
    }

    /**
     * Returns the project name field value (used as SonarQube project key).
     *
     * @return trimmed project name, possibly empty
     */
    public String getProjectName() {
        return projectNameField.getText().trim();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void buildLayout() {
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(2, 4, 2, 4);
        g.fill   = GridBagConstraints.HORIZONTAL;
        int r = 0;

        // Output dir row
        g.gridx = 0; g.gridy = r; g.weightx = 0;
        add(new JLabel("Output-Verzeichnis:"), g);
        g.gridx = 1; g.weightx = 1;
        add(outputDirField, g);
        g.gridx = 2; g.weightx = 0;
        add(buildBrowseButton(), g);
        r++;

        // Project name row
        g.gridx = 0; g.gridy = r; g.weightx = 0;
        add(new JLabel("Projektname:"), g);
        g.gridx = 1; g.weightx = 1; g.gridwidth = 2;
        add(projectNameField, g);
        g.gridwidth = 1;
        r++;

        // Resolved path info label
        g.gridx = 0; g.gridy = r; g.gridwidth = 3; g.weightx = 1;
        JLabel hint = new JLabel("Ergebnis-Pfad:");
        add(hint, g);
        r++;

        g.gridx = 0; g.gridy = r; g.gridwidth = 3;
        add(resolvedPathLabel, g);
    }

    private JButton buildBrowseButton() {
        JButton btn = new JButton("Durchsuchen\u2026");
        btn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            String current = outputDirField.getText().trim();
            if (!current.isBlank()) {
                fc.setCurrentDirectory(new File(current));
            }
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                outputDirField.setText(fc.getSelectedFile().getAbsolutePath());
            }
        });
        return btn;
    }

    private void updateLabel() {
        Path resolved = getResolvedOutputPath();
        resolvedPathLabel.setText("\u2192 " + resolved.toString() + "/");
    }
}
