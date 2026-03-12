package org.example.gui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

/**
 * Shows False-Positive and False-Negative FQCNs per tool in a tabbed layout.
 * Each tool tab contains two side-by-side lists: FP (left) and FN (right).
 */
public class FpFnPanel extends JPanel {

    private static final String[] TOOLS = {"baseline", "sonar", "jdeodorant"};

    private final Map<String, DefaultListModel<String>> fpModels = new LinkedHashMap<>();
    private final Map<String, DefaultListModel<String>> fnModels = new LinkedHashMap<>();

    /** Creates an empty FpFnPanel with one tab per tool. */
    public FpFnPanel() {
        super(new BorderLayout());
        JTabbedPane tabs = new JTabbedPane();
        for (String tool : TOOLS) {
            DefaultListModel<String> fpModel = new DefaultListModel<>();
            DefaultListModel<String> fnModel = new DefaultListModel<>();
            fpModels.put(tool, fpModel);
            fnModels.put(tool, fnModel);
            tabs.addTab(tool, buildToolTab(fpModel, fnModel));
        }
        add(tabs, BorderLayout.CENTER);
    }

    /**
     * Replaces all list contents with the given FP/FN maps.
     *
     * @param fpFqcns per-tool false-positive FQCN lists
     * @param fnFqcns per-tool false-negative FQCN lists
     */
    public void update(Map<String, List<String>> fpFqcns, Map<String, List<String>> fnFqcns) {
        for (String tool : TOOLS) {
            populateModel(fpModels.get(tool), fpFqcns.getOrDefault(tool, List.of()));
            populateModel(fnModels.get(tool), fnFqcns.getOrDefault(tool, List.of()));
        }
    }

    private JPanel buildToolTab(DefaultListModel<String> fpModel, DefaultListModel<String> fnModel) {
        JPanel panel = new JPanel(new GridLayout(1, 2, 4, 0));
        panel.add(buildListPanel("False Positives (fälschlich erkannt)", fpModel));
        panel.add(buildListPanel("False Negatives (nicht erkannt)", fnModel));
        return panel;
    }

    private JPanel buildListPanel(String title, DefaultListModel<String> model) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setBorder(BorderFactory.createTitledBorder(title));
        p.add(new JScrollPane(new JList<>(model)), BorderLayout.CENTER);
        return p;
    }

    private void populateModel(DefaultListModel<String> model, List<String> items) {
        model.clear();
        items.forEach(model::addElement);
    }
}
