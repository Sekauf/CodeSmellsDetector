package org.example.baseline;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SourceScanner {
    public List<Path> findProductionJavaFiles(Path projectRoot) throws IOException {
        List<Path> sourceRoots = findSourceRoots(projectRoot);
        return listJavaFiles(sourceRoots);
    }

    public List<Path> findSourceRoots(Path projectRoot) throws IOException {
        List<Path> roots = new ArrayList<>();
        Path direct = projectRoot.resolve("src").resolve("main").resolve("java");
        if (Files.isDirectory(direct)) {
            roots.add(direct);
        }

        try (Stream<Path> stream = Files.walk(projectRoot, Integer.MAX_VALUE, FileVisitOption.FOLLOW_LINKS)) {
            List<Path> found = stream
                    .filter(Files::isDirectory)
                    .filter(path -> path.endsWith(Path.of("src", "main", "java")))
                    .filter(path -> !isExcluded(path))
                    .collect(Collectors.toList());
            for (Path path : found) {
                if (!roots.contains(path)) {
                    roots.add(path);
                }
            }
        }

        roots.sort(Comparator.comparing(Path::toString));
        return roots;
    }

    public List<Path> listJavaFiles(List<Path> sourceRoots) throws IOException {
        List<Path> javaFiles = new ArrayList<>();
        for (Path root : sourceRoots) {
            try (Stream<Path> stream = Files.walk(root, Integer.MAX_VALUE, FileVisitOption.FOLLOW_LINKS)) {
                javaFiles.addAll(stream
                        .filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".java"))
                        .filter(path -> !isExcluded(path))
                        .collect(Collectors.toList()));
            }
        }
        javaFiles.sort(Comparator.comparing(Path::toString));
        return javaFiles;
    }

    public boolean isExcluded(Path path) {
        String normalized = path.toString().replace('\\', '/');
        if (normalized.contains("/src/test/")) {
            return true;
        }
        return normalized.contains("/target/")
                || normalized.contains("/build/")
                || normalized.contains("/out/")
                || normalized.contains("/generated/");
    }
}
