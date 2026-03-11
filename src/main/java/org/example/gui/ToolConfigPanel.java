package org.example.gui;

import java.util.ArrayList;
import java.util.List;
import javax.swing.JTabbedPane;
import org.example.baseline.BaselineThresholds;
import org.example.jdeodorant.ProjectConfig;
import org.example.sonar.SonarConfig;

/**
 * Composite panel showing tool configuration in a {@link JTabbedPane}.
 * <p>
 * Contains three tabs: {@link BaselineConfigTab}, {@link SonarConfigTab},
 * {@link JDeodorantConfigTab}. Provides typed getters for the analysis and
 * notifies registered listeners whenever a tool's enable-state changes.
 */
public class ToolConfigPanel extends JTabbedPane {

    private static final String TAB_BASELINE    = "Baseline";
    private static final String TAB_SONAR       = "SonarQube";
    private static final String TAB_JDEODORANT  = "JDeodorant";

    private final BaselineConfigTab    baselineTab;
    private final SonarConfigTab       sonarTab;
    private final JDeodorantConfigTab  jdeodorantTab;
    private final List<Runnable>       enablementListeners = new ArrayList<>();

    /** Constructs the tabbed panel and wires all inter-tab listeners. */
    public ToolConfigPanel() {
        baselineTab   = new BaselineConfigTab();
        sonarTab      = new SonarConfigTab();
        jdeodorantTab = new JDeodorantConfigTab();

        addTab(TAB_BASELINE,   baselineTab);
        addTab(TAB_SONAR,      sonarTab);
        addTab(TAB_JDEODORANT, jdeodorantTab);

        Runnable notify = this::fireEnablementChanged;
        baselineTab.addEnablementListener(notify);
        sonarTab.addEnablementListener(notify);
        jdeodorantTab.addEnablementListener(notify);
    }

    // -------------------------------------------------------------------------
    // Configuration getters
    // -------------------------------------------------------------------------

    /**
     * Returns the current {@link BaselineThresholds}.
     * Only call this when {@link #isBaselineEnabled()} is {@code true}.
     *
     * @return baseline thresholds
     */
    public BaselineThresholds getBaselineThresholds() {
        return baselineTab.getThresholds();
    }

    /**
     * Builds a {@link SonarConfig} for the given project key,
     * or {@code null} if SonarQube is disabled.
     *
     * @param projectKey key derived from the project directory name
     * @return SonarConfig or {@code null}
     */
    public SonarConfig getSonarConfig(String projectKey) {
        return sonarTab.buildConfig(projectKey);
    }

    /**
     * Returns a {@link ProjectConfig} for JDeodorant CSV import,
     * or {@code null} if JDeodorant is disabled or no CSV is set.
     *
     * @return ProjectConfig or {@code null}
     */
    public ProjectConfig getJDeodorantConfig() {
        String csv = jdeodorantTab.getCsvPath();
        return (csv != null) ? ProjectConfig.forJdeodorantCsv(csv) : null;
    }

    // -------------------------------------------------------------------------
    // Enable-state queries
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} if the Baseline tool is enabled.
     *
     * @return baseline enable state
     */
    public boolean isBaselineEnabled() {
        return baselineTab.isEnabled();
    }

    /**
     * Returns whether SonarQube Docker auto-stop is configured.
     *
     * @return {@code true} if Docker mode is on and the auto-stop checkbox is checked
     */
    public boolean isSonarAutoStopEnabled() {
        return sonarTab.isAutoStopEnabled();
    }

    /**
     * Returns {@code true} if at least one tool tab is enabled.
     * Used to decide whether the Run button should be active.
     *
     * @return {@code true} when at least one tool is on
     */
    public boolean isAtLeastOneEnabled() {
        return baselineTab.isEnabled()
                || sonarTab.isEnabled()
                || jdeodorantTab.isEnabled();
    }

    // -------------------------------------------------------------------------
    // Listener management
    // -------------------------------------------------------------------------

    /**
     * Registers a listener that is notified whenever any tool's enable-state changes.
     *
     * @param listener runnable to call on change
     */
    public void addEnablementListener(Runnable listener) {
        enablementListeners.add(listener);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void fireEnablementChanged() {
        for (Runnable listener : enablementListeners) {
            listener.run();
        }
    }
}
