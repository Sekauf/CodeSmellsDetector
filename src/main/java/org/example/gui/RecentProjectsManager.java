package org.example.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Persists the last {@value #MAX_ENTRIES} used project paths
 * via {@link java.util.prefs.Preferences}.
 */
public class RecentProjectsManager {

    private static final int MAX_ENTRIES = 5;
    private static final String KEY_PREFIX = "recent.project.";
    private static final String KEY_COUNT  = "recent.project.count";

    private final Preferences prefs;

    /** Creates a manager backed by user-scoped {@link Preferences}. */
    public RecentProjectsManager() {
        this.prefs = Preferences.userNodeForPackage(RecentProjectsManager.class);
    }

    /**
     * Adds {@code path} to the front of the recent-projects list.
     * Duplicates are removed first; the list is capped at {@value #MAX_ENTRIES}.
     *
     * @param path absolute path string; blank values are ignored
     */
    public void addProject(String path) {
        if (path == null || path.isBlank()) {
            return;
        }
        List<String> current = getProjects();
        current.remove(path);
        current.add(0, path);
        if (current.size() > MAX_ENTRIES) {
            current = current.subList(0, MAX_ENTRIES);
        }
        save(current);
    }

    /**
     * Returns up to {@value #MAX_ENTRIES} recent project paths, most-recent first.
     *
     * @return mutable list of stored paths
     */
    public List<String> getProjects() {
        int count = prefs.getInt(KEY_COUNT, 0);
        List<String> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String val = prefs.get(KEY_PREFIX + i, null);
            if (val != null) {
                result.add(val);
            }
        }
        return result;
    }

    /**
     * Removes all stored recent projects.
     * Intended for testing and user-initiated reset.
     */
    public void clear() {
        try {
            prefs.clear();
            prefs.flush();
        } catch (Exception ignored) {
            // Best-effort clear
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void save(List<String> paths) {
        prefs.putInt(KEY_COUNT, paths.size());
        for (int i = 0; i < paths.size(); i++) {
            prefs.put(KEY_PREFIX + i, paths.get(i));
        }
        try {
            prefs.flush();
        } catch (Exception ignored) {
            // Best-effort flush
        }
    }
}
