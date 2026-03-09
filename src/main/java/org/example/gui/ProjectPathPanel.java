package org.example.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Composite panel for selecting a Java project root directory.
 * <p>
 * Features:
 * <ul>
 *   <li>Editable combo box with the last 5 used projects</li>
 *   <li>"Durchsuchen…" button opening a directory-only {@link JFileChooser}</li>
 *   <li>Drag-and-Drop of a folder onto the combo box</li>
 *   <li>Live validation with ✔/✘ icon and tooltip on error</li>
 * </ul>
 */
public class ProjectPathPanel extends JPanel {

    private static final Color COLOR_VALID   = new Color(0, 150, 0);
    private static final Color COLOR_INVALID = new Color(180, 0, 0);

    private final JComboBox<String>     pathCombo;
    private final JLabel                statusLabel;
    private final ProjectValidator      validator = new ProjectValidator();
    private final RecentProjectsManager recents   = new RecentProjectsManager();

    /** Constructs the panel and loads the persisted recent-projects list. */
    public ProjectPathPanel() {
        super(new BorderLayout(4, 0));

        pathCombo   = new JComboBox<>();
        pathCombo.setEditable(true);
        statusLabel = new JLabel("  ");
        statusLabel.setFont(statusLabel.getFont().deriveFont(14f));

        loadRecentProjects();
        installDropTarget();

        pathCombo.addActionListener(e -> validateAndUpdate(getSelectedPath()));

        JButton browseBtn = new JButton("Durchsuchen\u2026");
        browseBtn.addActionListener(e -> onBrowse());

        JPanel right = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        right.add(browseBtn);
        right.add(statusLabel);

        add(pathCombo, BorderLayout.CENTER);
        add(right, BorderLayout.EAST);
    }

    /**
     * Returns the currently entered path string (may be empty).
     *
     * @return trimmed path or empty string
     */
    public String getSelectedPath() {
        Object item = pathCombo.getEditor().getItem();
        return item == null ? "" : item.toString().trim();
    }

    /**
     * Registers a listener that is called with the new path string whenever
     * the user selects or types a path.
     *
     * @param listener consumer receiving the current path (may be empty)
     */
    public void addPathChangeListener(java.util.function.Consumer<String> listener) {
        pathCombo.addActionListener(e -> listener.accept(getSelectedPath()));
    }

    /**
     * Sets the path programmatically, triggers live validation, and updates the UI.
     *
     * @param path absolute path string
     */
    public void setPath(String path) {
        pathCombo.getEditor().setItem(path);
        validateAndUpdate(path);
    }

    /**
     * Validates the current path. If valid, saves it to the recent-projects list.
     *
     * @return validation result for the caller to act on
     */
    public ProjectValidator.Result validatePath() {
        String path = getSelectedPath();
        ProjectValidator.Result result = validator.validate(path.isEmpty() ? null : Path.of(path));
        if (result.isValid()) {
            recents.addProject(path);
            loadRecentProjects();
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void onBrowse() {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        String current = getSelectedPath();
        if (!current.isEmpty()) {
            fc.setCurrentDirectory(new File(current));
        }
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            String chosen = fc.getSelectedFile().getAbsolutePath();
            pathCombo.getEditor().setItem(chosen);
            validateAndUpdate(chosen);
        }
    }

    private void installDropTarget() {
        new DropTarget(pathCombo, DnDConstants.ACTION_COPY, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent event) {
                try {
                    event.acceptDrop(DnDConstants.ACTION_COPY);
                    var transferable = event.getTransferable();
                    if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        @SuppressWarnings("unchecked")
                        List<File> files = (List<File>) transferable
                                .getTransferData(DataFlavor.javaFileListFlavor);
                        if (!files.isEmpty()) {
                            File dropped = files.get(0);
                            String path = dropped.isDirectory()
                                    ? dropped.getAbsolutePath()
                                    : dropped.getParentFile().getAbsolutePath();
                            pathCombo.getEditor().setItem(path);
                            validateAndUpdate(path);
                        }
                    }
                    event.dropComplete(true);
                } catch (Exception ex) {
                    event.dropComplete(false);
                }
            }
        });
    }

    private void validateAndUpdate(String pathStr) {
        if (pathStr == null || pathStr.isBlank()) {
            statusLabel.setText("  ");
            statusLabel.setToolTipText(null);
            return;
        }
        ProjectValidator.Result result = validator.validate(Path.of(pathStr));
        if (result.isValid()) {
            statusLabel.setText("\u2714");
            statusLabel.setForeground(COLOR_VALID);
            statusLabel.setToolTipText(null);
        } else {
            statusLabel.setText("\u2718");
            statusLabel.setForeground(COLOR_INVALID);
            statusLabel.setToolTipText(result.message());
        }
    }

    private void loadRecentProjects() {
        Object currentItem = pathCombo.getEditor().getItem();
        pathCombo.removeAllItems();
        for (String p : recents.getProjects()) {
            pathCombo.addItem(p);
        }
        if (currentItem != null && !currentItem.toString().isBlank()) {
            pathCombo.getEditor().setItem(currentItem.toString());
        } else if (pathCombo.getItemCount() > 0) {
            pathCombo.setSelectedIndex(0);
        }
    }
}
