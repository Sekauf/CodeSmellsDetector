package org.example.baseline;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BaselineAnalyzer {
    private static final Logger LOGGER = Logger.getLogger(BaselineAnalyzer.class.getName());

    public List<CandidateDTO> analyze(Path projectRoot, BaselineThresholds thresholds) throws IOException {
        return analyzeWithMetadata(projectRoot, thresholds).getCandidates();
    }

    public List<CandidateDTO> analyze(List<Path> javaFiles, BaselineThresholds thresholds) throws IOException {
        return analyzeWithMetadata(javaFiles, thresholds).getCandidates();
    }

    public BaselineAnalysisResult analyzeWithMetadata(Path projectRoot, BaselineThresholds thresholds)
            throws IOException {
        SourceScanner scanner = new SourceScanner();
        List<Path> javaFiles = scanner.findProductionJavaFiles(projectRoot);
        return analyzeWithMetadata(javaFiles, thresholds);
    }

    public BaselineAnalysisResult analyzeWithMetadata(List<Path> javaFiles, BaselineThresholds thresholds)
            throws IOException {
        Instant start = Instant.now();
        LOGGER.info("Baseline analysis started.");
        JavaSourceParser parser = new JavaSourceParser();
        List<ParsedType> parsedTypes = parser.parseFiles(javaFiles);
        Map<Path, List<ParsedType>> typesByFile = groupByFile(parsedTypes);

        MetricsCalculator calculator = new MetricsCalculator();
        List<CandidateDTO> candidates = new ArrayList<>();

        for (Path javaFile : javaFiles) {
            String source = Files.readString(javaFile, StandardCharsets.UTF_8);
            ClassMetrics metrics = calculator.calculateFromSource(source);
            String fqn = metrics.getFullyQualifiedName();

            boolean exceedsSize = (metrics.getMethodCount() + metrics.getFieldCount())
                    > thresholds.getMethodPlusFieldThreshold();
            boolean exceedsDependency = metrics.getDependencyTypeCount()
                    > thresholds.getDependencyTypeThreshold();
            if (!exceedsSize && !exceedsDependency) {
                continue;
            }

            List<String> reasons = new ArrayList<>();
            if (exceedsSize) {
                reasons.add("METHODS_PLUS_FIELDS>" + thresholds.getMethodPlusFieldThreshold());
            }
            if (exceedsDependency) {
                reasons.add("DEPENDENCY_TYPES>" + thresholds.getDependencyTypeThreshold());
            }

            String resolvedFqn = resolveFqn(typesByFile.get(javaFile), fqn);
            candidates.add(new CandidateDTO(
                    resolvedFqn,
                    metrics.getMethodCount(),
                    metrics.getFieldCount(),
                    metrics.getDependencyTypeCount(),
                    reasons
            ));
        }

        candidates.sort(Comparator.comparing(CandidateDTO::getFullyQualifiedClassName));
        Instant end = Instant.now();
        LOGGER.info(String.format("Baseline analysis finished in %d ms. Files=%d Types=%d Candidates=%d",
                Duration.between(start, end).toMillis(),
                javaFiles.size(),
                parsedTypes.size(),
                candidates.size()));
        AnalysisMetadata metadata = new AnalysisMetadata(
                System.getProperty("java.version", "unknown"),
                resolveJavaParserVersion(),
                end.toString()
        );
        return new BaselineAnalysisResult(candidates, metadata);
    }

    private Map<Path, List<ParsedType>> groupByFile(List<ParsedType> parsedTypes) {
        Map<Path, List<ParsedType>> grouped = new HashMap<>();
        for (ParsedType parsedType : parsedTypes) {
            grouped.computeIfAbsent(parsedType.getSourceFile(), path -> new ArrayList<>())
                    .add(parsedType);
        }
        for (List<ParsedType> list : grouped.values()) {
            list.sort(Comparator.comparing(ParsedType::getFullyQualifiedName));
        }
        return grouped;
    }

    private String resolveFqn(List<ParsedType> parsedTypes, String fallback) {
        if (parsedTypes == null || parsedTypes.isEmpty()) {
            return fallback;
        }
        for (ParsedType parsedType : parsedTypes) {
            if (parsedType.getFullyQualifiedName().equals(fallback)) {
                return fallback;
            }
        }
        return parsedTypes.get(0).getFullyQualifiedName();
    }

    private String resolveJavaParserVersion() {
        try {
            Class<?> parser = Class.forName("com.github.javaparser.StaticJavaParser");
            Package parserPackage = parser.getPackage();
            if (parserPackage == null) {
                return "unknown";
            }
            return Optional.ofNullable(parserPackage.getImplementationVersion()).orElse("unknown");
        } catch (ClassNotFoundException ex) {
            return "unknown";
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Failed to resolve JavaParser version.", ex);
            return "unknown";
        }
    }
}
