package org.example.gui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Validates a directory as a Java project.
 * Checks (a) existence, (b) presence of .java files, (c) build descriptor.
 */
public class ProjectValidator {

    /** Outcome of a {@link #validate(Path)} call. */
    public enum Status { VALID, INVALID }

    /** Immutable validation result carrying status and human-readable message. */
    public record Result(Status status, String message) {
        /** Returns {@code true} when status is {@link Status#VALID}. */
        public boolean isValid() {
            return status == Status.VALID;
        }
    }

    private static final int MAX_WALK_DEPTH = 10;

    /**
     * Validates the given path as a Java project directory.
     *
     * @param dir path to validate; may be {@code null}
     * @return result with status and message
     */
    public Result validate(Path dir) {
        if (dir == null || !Files.isDirectory(dir)) {
            return invalid("Pfad existiert nicht oder ist kein Verzeichnis.");
        }
        if (!hasBuildFile(dir)) {
            return invalid("Kein pom.xml / build.gradle im Projektverzeichnis gefunden.");
        }
        if (!hasJavaFiles(dir)) {
            return invalid("Keine .java-Dateien im Projektverzeichnis gefunden.");
        }
        return new Result(Status.VALID, "Gültiges Java-Projekt.");
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private boolean hasBuildFile(Path dir) {
        return Files.exists(dir.resolve("pom.xml"))
                || Files.exists(dir.resolve("build.gradle"))
                || Files.exists(dir.resolve("build.gradle.kts"));
    }

    private boolean hasJavaFiles(Path dir) {
        try (Stream<Path> stream = Files.walk(dir, MAX_WALK_DEPTH)) {
            return stream
                    .filter(Files::isRegularFile)
                    .anyMatch(p -> p.toString().endsWith(".java"));
        } catch (IOException e) {
            return false;
        }
    }

    private static Result invalid(String message) {
        return new Result(Status.INVALID, message);
    }
}
