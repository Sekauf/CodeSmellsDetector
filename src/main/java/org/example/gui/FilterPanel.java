package org.example.gui;

import java.awt.FlowLayout;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Filter control panel for the candidate results table.
 * Provides a FQCN text-search field, tool-flag checkboxes, predefined quick-filter
 * buttons, and a reset button. Calls {@code onFilterChanged} on every state change.
 */
public class FilterPanel extends JPanel {

    private final JTextField searchField = new JTextField(20);
    private final JCheckBox  cbBaseline  = new JCheckBox("Baseline");
    private final JCheckBox  cbSonar     = new JCheckBox("SonarQube");
    private final JCheckBox  cbJdeo      = new JCheckBox("JDeodorant");
    private final JButton    btnAllAgree     = new JButton("Alle einig");
    private final JButton    btnOneTool      = new JButton("Nur 1 Tool");
    private final JButton    btnDisagreement = new JButton("Disagreement");

    private CandidateRowFilter.QuickMode activeQuick = CandidateRowFilter.QuickMode.NONE;
    private final Runnable onFilterChanged;

    /**
     * @param onFilterChanged callback invoked on the EDT whenever the filter state changes
     */
    public FilterPanel(Runnable onFilterChanged) {
        this.onFilterChanged = onFilterChanged;
        buildUi();
    }

    private void buildUi() {
        setLayout(new FlowLayout(FlowLayout.LEFT, 6, 4));
        setBorder(BorderFactory.createTitledBorder("Filter"));

        add(new JLabel("Suche:"));
        add(searchField);
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  { fireChanged(); }
            @Override public void removeUpdate(DocumentEvent e)  { fireChanged(); }
            @Override public void changedUpdate(DocumentEvent e) { fireChanged(); }
        });

        add(cbBaseline);
        add(cbSonar);
        add(cbJdeo);
        cbBaseline.addActionListener(e -> fireChanged());
        cbSonar.addActionListener(e -> fireChanged());
        cbJdeo.addActionListener(e -> fireChanged());

        btnAllAgree.addActionListener(e -> toggleQuick(CandidateRowFilter.QuickMode.ALL_AGREE));
        btnOneTool.addActionListener(e -> toggleQuick(CandidateRowFilter.QuickMode.ONE_TOOL));
        btnDisagreement.addActionListener(e -> toggleQuick(CandidateRowFilter.QuickMode.DISAGREEMENT));
        add(btnAllAgree);
        add(btnOneTool);
        add(btnDisagreement);

        JButton resetBtn = new JButton("Alle anzeigen");
        resetBtn.addActionListener(e -> resetAll());
        add(resetBtn);
    }

    /**
     * Builds a {@link CandidateRowFilter} from the current UI state.
     * Returns {@code null} when no restrictions are active (show all rows).
     *
     * @return an active filter, or {@code null} if no filtering should be applied
     */
    public RowFilter<CandidateTableModel, Integer> buildFilter() {
        CandidateRowFilter filter = new CandidateRowFilter(
                searchField.getText(),
                cbBaseline.isSelected(),
                cbSonar.isSelected(),
                cbJdeo.isSelected(),
                activeQuick);
        return filter.isActive() ? filter : null;
    }

    private void toggleQuick(CandidateRowFilter.QuickMode mode) {
        activeQuick = (activeQuick == mode) ? CandidateRowFilter.QuickMode.NONE : mode;
        updateQuickButtonStyles();
        fireChanged();
    }

    private void updateQuickButtonStyles() {
        styleButton(btnAllAgree,     activeQuick == CandidateRowFilter.QuickMode.ALL_AGREE);
        styleButton(btnOneTool,      activeQuick == CandidateRowFilter.QuickMode.ONE_TOOL);
        styleButton(btnDisagreement, activeQuick == CandidateRowFilter.QuickMode.DISAGREEMENT);
    }

    private void styleButton(JButton btn, boolean active) {
        btn.setFont(btn.getFont().deriveFont(active ? Font.BOLD : Font.PLAIN));
    }

    private void resetAll() {
        searchField.setText("");
        cbBaseline.setSelected(false);
        cbSonar.setSelected(false);
        cbJdeo.setSelected(false);
        activeQuick = CandidateRowFilter.QuickMode.NONE;
        updateQuickButtonStyles();
        fireChanged();
    }

    private void fireChanged() {
        onFilterChanged.run();
    }
}
