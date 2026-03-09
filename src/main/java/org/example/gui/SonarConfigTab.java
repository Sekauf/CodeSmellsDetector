package org.example.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import org.example.sonar.SonarConfig;

/**
 * Configuration tab for the SonarQube God-Class detector.
 * <p>
 * Exposes host URL, masked token, Docker-toggle and the fixed rule {@code java:S6539}.
 */
public class SonarConfigTab extends JPanel {

    private static final String DEFAULT_HOST  = "http://localhost:9000";
    private static final String SONAR_RULE    = "java:S6539";

    private final JCheckBox      enableCheckBox;
    private final JTextField     hostField;
    private final JPasswordField tokenField;
    private final JCheckBox      dockerCheckBox;

    /** Constructs the tab with pre-filled defaults. */
    public SonarConfigTab() {
        super(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        enableCheckBox = new JCheckBox("SonarQube aktivieren", false);
        hostField      = new JTextField(DEFAULT_HOST);
        tokenField     = new JPasswordField();
        dockerCheckBox = new JCheckBox("SonarQube via Docker starten");

        buildLayout();
        enableCheckBox.addItemListener(e -> updateFieldState());
        updateFieldState();
    }

    /**
     * Returns whether SonarQube is enabled.
     *
     * @return {@code true} if the enable checkbox is selected
     */
    public boolean isEnabled() {
        return enableCheckBox.isSelected();
    }

    /**
     * Builds a {@link SonarConfig} from the current field values.
     *
     * @param projectKey project key derived from the project directory name
     * @return configured SonarConfig, or {@code null} if not enabled
     */
    public SonarConfig buildConfig(String projectKey) {
        if (!isEnabled()) {
            return null;
        }
        String host  = hostField.getText().trim();
        String token = new String(tokenField.getPassword()).trim();
        return SonarConfig.builder()
                .hostUrl(host.isEmpty() ? DEFAULT_HOST : host)
                .token(token.isEmpty() ? null : token)
                .projectKey(projectKey)
                .dockerEnabled(dockerCheckBox.isSelected())
                .build();
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
        g.gridx = 0; g.gridy = r++; g.gridwidth = 2; g.weightx = 1;
        add(enableCheckBox, g);
        g.gridwidth = 1; g.weightx = 0;

        // Fields
        addRow("Host-URL:",     hostField,      r++, g);
        addRow("Token:",        tokenField,     r++, g);

        // Docker toggle
        g.gridx = 0; g.gridy = r++; g.gridwidth = 2;
        add(dockerCheckBox, g);
        g.gridwidth = 1;

        // Fixed rule (read-only)
        addRow("Regel (fest):", buildRuleLabel(), r, g);
    }

    private JLabel buildRuleLabel() {
        JLabel lbl = new JLabel(SONAR_RULE);
        lbl.setToolTipText("God-Class-Regel, die von SonarQube ausgewertet wird (nicht änderbar)");
        return lbl;
    }

    private void addRow(String label, java.awt.Component field, int row, GridBagConstraints g) {
        g.gridx = 0; g.gridy = row; g.weightx = 0;
        add(new JLabel(label), g);
        g.gridx = 1; g.weightx = 1;
        add(field, g);
    }

    private void updateFieldState() {
        boolean on = enableCheckBox.isSelected();
        hostField.setEnabled(on);
        tokenField.setEnabled(on);
        dockerCheckBox.setEnabled(on);
    }
}
