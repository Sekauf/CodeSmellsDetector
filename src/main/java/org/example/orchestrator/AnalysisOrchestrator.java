package org.example.orchestrator;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import org.example.baseline.BaselineAnalyzer;
import org.example.baseline.BaselineThresholds;
import org.example.baseline.CandidateDTO;
import org.example.export.ResultExporter;
import org.example.jdeodorant.JDeodorantIntegration;
import org.example.jdeodorant.ProjectConfig;
import org.example.sonar.SonarAnalyzer;
import org.example.sonar.SonarConfig;

public class AnalysisOrchestrator {
    private static final Logger LOGGER = Logger.getLogger(AnalysisOrchestrator.class.getName());

    @FunctionalInterface
    public interface BaselineRunner {
        List<CandidateDTO> run(Path projectRoot, BaselineThresholds thresholds) throws IOException;
    }

    @FunctionalInterface
    public interface SonarRunner {
        List<CandidateDTO> run(Path projectRoot, SonarConfig config) throws IOException, InterruptedException;
    }

    @FunctionalInterface
    public interface JdeodorantRunner {
        List<CandidateDTO> run(ProjectConfig config) throws IOException, InterruptedException;
    }

    private final BaselineRunner baselineRunner;
    private final SonarRunner sonarRunner;
    private final JdeodorantRunner jdeodorantRunner;
    private final ResultExporter exporter;

    public AnalysisOrchestrator() {
        this(
                (projectRoot, thresholds) -> new BaselineAnalyzer().analyze(projectRoot, thresholds),
                (projectRoot, config) -> new SonarAnalyzer().analyze(projectRoot, config),
                config -> new JDeodorantIntegration().getJDeodorantCandidates(config),
                new ResultExporter()
        );
    }

    public AnalysisOrchestrator(
            BaselineRunner baselineRunner,
            SonarRunner sonarRunner,
            JdeodorantRunner jdeodorantRunner,
            ResultExporter exporter
    ) {
        this.baselineRunner = Objects.requireNonNull(baselineRunner, "baselineRunner");
        this.sonarRunner = Objects.requireNonNull(sonarRunner, "sonarRunner");
        this.jdeodorantRunner = Objects.requireNonNull(jdeodorantRunner, "jdeodorantRunner");
        this.exporter = Objects.requireNonNull(exporter, "exporter");
    }

    public List<CandidateDTO> run(
            Path projectRoot,
            BaselineThresholds thresholds,
            SonarConfig sonarConfig,
            ProjectConfig jdeodorantConfig,
            Path outputDir
    ) throws IOException, InterruptedException {
        LOGGER.info("Orchestrator started.");

        List<CandidateDTO> baselineCandidates = List.of();
        if (projectRoot != null && thresholds != null) {
            LOGGER.info("BaselineAnalyzer started.");
            baselineCandidates = baselineRunner.run(projectRoot, thresholds);
            LOGGER.info("BaselineAnalyzer finished. candidates=" + baselineCandidates.size());
        } else {
            LOGGER.info("BaselineAnalyzer skipped.");
        }

        List<CandidateDTO> sonarCandidates = List.of();
        if (projectRoot != null && sonarConfig != null) {
            LOGGER.info("SonarAnalyzer started.");
            sonarCandidates = sonarRunner.run(projectRoot, sonarConfig);
            LOGGER.info("SonarAnalyzer finished. candidates=" + sonarCandidates.size());
        } else {
            LOGGER.info("SonarAnalyzer skipped.");
        }

        List<CandidateDTO> jdeodorantCandidates = List.of();
        if (jdeodorantConfig != null) {
            LOGGER.info("JDeodorantIntegration started.");
            jdeodorantCandidates = jdeodorantRunner.run(jdeodorantConfig);
            LOGGER.info("JDeodorantIntegration finished. candidates=" + jdeodorantCandidates.size());
        } else {
            LOGGER.info("JDeodorantIntegration skipped.");
        }

        List<CandidateDTO> merged = mergeCandidates(baselineCandidates, sonarCandidates, jdeodorantCandidates);
        exporter.writeCsv(merged, outputDir);
        exporter.writeJson(merged, outputDir);
        LOGGER.info("Orchestrator finished. candidates=" + merged.size());
        return merged;
    }

    private List<CandidateDTO> mergeCandidates(
            List<CandidateDTO> baselineCandidates,
            List<CandidateDTO> sonarCandidates,
            List<CandidateDTO> jdeodorantCandidates
    ) {
        Map<String, CandidateDTO> merged = new HashMap<>();
        addAll(merged, baselineCandidates, true, false, false);
        addAll(merged, sonarCandidates, false, true, false);
        addAll(merged, jdeodorantCandidates, false, false, true);
        return List.copyOf(merged.values());
    }

    private void addAll(
            Map<String, CandidateDTO> merged,
            List<CandidateDTO> incoming,
            boolean baselineFlag,
            boolean sonarFlag,
            boolean jdeodorantFlag
    ) {
        if (incoming == null || incoming.isEmpty()) {
            return;
        }
        for (CandidateDTO candidate : incoming) {
            if (candidate == null) {
                continue;
            }
            String key = candidate.getFullyQualifiedClassName();
            if (key == null || key.isBlank()) {
                key = candidate.toString();
            }
            CandidateDTO existing = merged.get(key);
            if (existing == null) {
                merged.put(key, withFlags(candidate, baselineFlag, sonarFlag, jdeodorantFlag));
            } else {
                merged.put(key, merge(existing, candidate, baselineFlag, sonarFlag, jdeodorantFlag));
            }
        }
    }

    private CandidateDTO withFlags(
            CandidateDTO candidate,
            boolean baselineFlag,
            boolean sonarFlag,
            boolean jdeodorantFlag
    ) {
        return new CandidateDTO(
                candidate.getFullyQualifiedClassName(),
                candidate.isBaselineFlag() || baselineFlag,
                candidate.isSonarFlag() || sonarFlag,
                candidate.isJdeodorantFlag() || jdeodorantFlag,
                candidate.getWmcNullable(),
                candidate.getTccNullable(),
                candidate.getAtfdNullable(),
                candidate.getCboNullable(),
                candidate.getLocNullable(),
                candidate.getMethodCount(),
                candidate.getFieldCount(),
                candidate.getDependencyTypeCount(),
                candidate.getReasons()
        );
    }

    private CandidateDTO merge(
            CandidateDTO left,
            CandidateDTO right,
            boolean baselineFlag,
            boolean sonarFlag,
            boolean jdeodorantFlag
    ) {
        String fqn = left.getFullyQualifiedClassName() != null
                ? left.getFullyQualifiedClassName()
                : right.getFullyQualifiedClassName();
        boolean baseline = left.isBaselineFlag() || right.isBaselineFlag() || baselineFlag;
        boolean sonar = left.isSonarFlag() || right.isSonarFlag() || sonarFlag;
        boolean jdeodorant = left.isJdeodorantFlag() || right.isJdeodorantFlag() || jdeodorantFlag;

        Integer wmc = coalesce(left.getWmcNullable(), right.getWmcNullable());
        Double tcc = coalesce(left.getTccNullable(), right.getTccNullable());
        Integer atfd = coalesce(left.getAtfdNullable(), right.getAtfdNullable());
        Integer cbo = coalesce(left.getCboNullable(), right.getCboNullable());
        Integer loc = coalesce(left.getLocNullable(), right.getLocNullable());

        int methodCount = coalesceInt(left.getMethodCount(), right.getMethodCount());
        int fieldCount = coalesceInt(left.getFieldCount(), right.getFieldCount());
        int dependencyTypeCount = coalesceInt(left.getDependencyTypeCount(), right.getDependencyTypeCount());
        List<String> reasons = coalesceList(left.getReasons(), right.getReasons());

        return new CandidateDTO(
                fqn,
                baseline,
                sonar,
                jdeodorant,
                wmc,
                tcc,
                atfd,
                cbo,
                loc,
                methodCount,
                fieldCount,
                dependencyTypeCount,
                reasons
        );
    }

    private static Integer coalesce(Integer left, Integer right) {
        return left != null ? left : right;
    }

    private static Double coalesce(Double left, Double right) {
        return left != null ? left : right;
    }

    private static int coalesceInt(int left, int right) {
        return left != 0 ? left : right;
    }

    private static List<String> coalesceList(List<String> left, List<String> right) {
        if (left != null && !left.isEmpty()) {
            return left;
        }
        return right == null ? List.of() : right;
    }
}
