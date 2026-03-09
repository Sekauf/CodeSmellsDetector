package org.example.gui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Resolves the effective output path as {@code <outputDir>/<projectName>/}
 * and creates the directory on demand.
 */
public class OutputPathResolver {

    private static final String DEFAULT_OUTPUT_DIR    = "output";
    private static final String DEFAULT_PROJECT_NAME  = "project";

    /**
     * Combines {@code outputDir} and {@code projectName} into a resolved output path.
     * Blank values fall back to safe defaults.
     *
     * @param outputDir   base output directory (blank → {@code "output"})
     * @param projectName project sub-directory name (blank → {@code "project"})
     * @return resolved path {@code <outputDir>/<projectName>}
     */
    public Path resolve(String outputDir, String projectName) {
        String dir  = (outputDir   == null || outputDir.isBlank())   ? DEFAULT_OUTPUT_DIR   : outputDir.trim();
        String name = (projectName == null || projectName.isBlank()) ? DEFAULT_PROJECT_NAME : sanitize(projectName.trim());
        return Path.of(dir, name);
    }

    /**
     * Ensures the given directory exists, creating it (and any parents) if necessary.
     *
     * @param dir path to create
     * @return the same path after creation
     * @throws IOException if the directory cannot be created
     */
    public Path ensureExists(Path dir) throws IOException {
        Files.createDirectories(dir);
        return dir;
    }

    /**
     * Derives a safe project name from the last component of a path string.
     * Returns an empty string when the path is blank.
     *
     * @param projectRootPath absolute or relative path string
     * @return last path component, or empty string
     */
    public String suggestProjectName(String projectRootPath) {
        if (projectRootPath == null || projectRootPath.isBlank()) {
            return "";
        }
        Path p = Path.of(projectRootPath.trim());
        Path fileName = p.getFileName();
        return fileName != null ? sanitize(fileName.toString()) : "";
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /** Strips characters that are unsafe in directory names. */
    private static String sanitize(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
