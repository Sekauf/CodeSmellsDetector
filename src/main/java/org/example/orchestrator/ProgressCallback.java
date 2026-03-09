package org.example.orchestrator;

/**
 * Callback for reporting analysis progress from {@link AnalysisOrchestrator} to the caller.
 *
 * <p>Invoked on the analysis background thread; implementations must be thread-safe
 * (e.g. delegate to {@code SwingUtilities.invokeLater} for GUI updates).</p>
 */
@FunctionalInterface
public interface ProgressCallback {

    /** No-op callback used when no progress reporting is needed. */
    ProgressCallback NOOP = (label, percent) -> {};

    /**
     * Called when a new analysis step starts or finishes.
     *
     * @param label   human-readable step description shown in the UI
     * @param percent completion percentage, 0–100 inclusive
     */
    void onStep(String label, int percent);
}
