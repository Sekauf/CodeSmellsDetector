package org.example.orchestrator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import org.example.baseline.CandidateCsvUtil;
import org.example.baseline.BaselineAnalyzer;
import org.example.baseline.BaselineThresholds;
import org.example.baseline.CandidateDTO;
import org.example.evaluation.ErrorAnalysisExporter;
import org.example.evaluation.OverlapResult;
import org.example.evaluation.ToolAgreementCalculator;
import org.example.evaluation.ToolEvaluator;
import org.example.export.ResultExporter;
import org.example.jdeodorant.JDeodorantIntegration;
import org.example.jdeodorant.ProjectConfig;
import org.example.labeling.LabelCsvExporter;
import org.example.labeling.LabelCsvImporter;
import org.example.labeling.LabelDTO;
import org.example.labeling.SecondReviewEvaluator;
import org.example.metrics.EvaluationMetrics;
import org.example.metrics.MetricsEngine;
import org.example.metrics.ReliabilityMetrics;
import org.example.reporting.MetricsSummaryCsvExporter;
import org.example.reporting.MultiProjectAggregator;
import org.example.reporting.ReportGenerator;
import org.example.sampling.SamplingEngine;
import org.example.sonar.SonarAnalyzer;
import org.example.sonar.SonarConfig;

public class AnalysisOrchestrator {
    private static final Logger LOGGER = Logger.getLogger(AnalysisOrchestrator.class.getName());

    private static final int DEFAULT_LABEL_THRESHOLD = 3;
    private static final int MIN_LABEL_THRESHOLD = 1;
    private static final int MAX_LABEL_THRESHOLD = 4;

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

    /**
     * Runs the full analysis pipeline without progress reporting.
     *
     * @see #run(Path, BaselineThresholds, SonarConfig, ProjectConfig, Path, ProgressCallback)
     */
    public List<CandidateDTO> run(
            Path projectRoot,
            BaselineThresholds thresholds,
            SonarConfig sonarConfig,
            ProjectConfig jdeodorantConfig,
            Path outputDir
    ) throws IOException, InterruptedException {
        return run(projectRoot, thresholds, sonarConfig, jdeodorantConfig, outputDir, ProgressCallback.NOOP);
    }

    /**
     * Runs the full analysis pipeline and reports progress via {@code callback}.
     *
     * <p>The callback is invoked before and after each major step; disabled tools are skipped
     * and the percentage jumps accordingly.</p>
     *
     * @param callback receives step labels and percentages (0–100) during execution
     */
    public List<CandidateDTO> run(
            Path projectRoot,
            BaselineThresholds thresholds,
            SonarConfig sonarConfig,
            ProjectConfig jdeodorantConfig,
            Path outputDir,
            ProgressCallback callback
    ) throws IOException, InterruptedException {
        LOGGER.info("Orchestrator started.");
        callback.onStep("Analyse wird vorbereitet\u2026", 5);

        List<CandidateDTO> baselineCandidates = List.of();
        if (projectRoot != null && thresholds != null) {
            callback.onStep("Baseline-Analyse l\u00e4uft\u2026", 15);
            LOGGER.info("BaselineAnalyzer started.");
            baselineCandidates = baselineRunner.run(projectRoot, thresholds);
            LOGGER.info("BaselineAnalyzer finished. candidates=" + baselineCandidates.size());
            callback.onStep("Baseline abgeschlossen", 35);
        } else {
            LOGGER.info("BaselineAnalyzer skipped.");
        }

        List<CandidateDTO> sonarCandidates = List.of();
        if (projectRoot != null && sonarConfig != null) {
            callback.onStep("SonarQube-Scan l\u00e4uft\u2026", 40);
            LOGGER.info("SonarAnalyzer started.");
            sonarCandidates = sonarRunner.run(projectRoot, sonarConfig);
            LOGGER.info("SonarAnalyzer finished. candidates=" + sonarCandidates.size());
            callback.onStep("SonarQube abgeschlossen", 65);
        } else {
            LOGGER.info("SonarAnalyzer skipped.");
        }

        List<CandidateDTO> jdeodorantCandidates = List.of();
        if (jdeodorantConfig != null) {
            callback.onStep("JDeodorant-Import l\u00e4uft\u2026", 70);
            LOGGER.info("JDeodorantIntegration started.");
            jdeodorantCandidates = jdeodorantRunner.run(jdeodorantConfig);
            LOGGER.info("JDeodorantIntegration finished. candidates=" + jdeodorantCandidates.size());
            callback.onStep("JDeodorant abgeschlossen", 80);
        } else {
            LOGGER.info("JDeodorantIntegration skipped.");
        }

        callback.onStep("Merge & Export\u2026", 85);
        List<CandidateDTO> merged = mergeCandidates(baselineCandidates, sonarCandidates, jdeodorantCandidates);
        String pn = outputDir != null && outputDir.getFileName() != null
                ? outputDir.getFileName().toString() : "";
        exporter.writeCsv(merged, outputDir, ResultExporter.csvFileName(pn));
        exporter.writeJson(merged, outputDir, ResultExporter.jsonFileName(pn));
        List<CandidateDTO> labelingInput = buildLabelingInput(merged);
        new LabelCsvExporter().export(labelingInput, outputDir.resolve(ResultExporter.labelingFileName(pn)));
        LOGGER.info("Orchestrator finished. candidates=" + labelingInput.size());
        callback.onStep("Abgeschlossen", 100);
        return labelingInput;
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

    private static List<CandidateDTO> buildLabelingInput(List<CandidateDTO> merged) {
        List<CandidateDTO> detected = new ArrayList<>();
        for (CandidateDTO c : merged) {
            if (c.isSonarFlag() || c.isJdeodorantFlag()) {
                detected.add(c);
            }
        }
        LOGGER.info("BlindNegativeSampling started. pool=" + merged.size()
                + " detected=" + detected.size());
        List<CandidateDTO> blindNegatives = SamplingEngine.sampleBlindNegativesTopPercentile(
                merged,
                5,
                0.1,
                42L,
                c -> c.isSonarFlag() || c.isJdeodorantFlag(),
                CandidateDTO::getMethodCount
        );
        LOGGER.info("BlindNegativeSampling finished. blindSamples=" + blindNegatives.size());
        List<CandidateDTO> labelingInput = new ArrayList<>(detected);
        labelingInput.addAll(blindNegatives);
        return labelingInput;
    }

    // -------------------------------------------------------------------------
    // Evaluate mode
    // -------------------------------------------------------------------------

    /**
     * Runs the full evaluation pipeline with the default label threshold of 3.
     *
     * @param labelsFile       annotated labeling CSV produced by the analyze phase
     * @param secondReviewFile optional second-review CSV for reliability analysis (may be null)
     * @param outputDir        directory for all evaluation output files
     * @throws IOException if the labels file cannot be read
     */
    public void evaluate(Path labelsFile, Path secondReviewFile, Path outputDir) throws IOException {
        evaluate(labelsFile, secondReviewFile, outputDir, DEFAULT_LABEL_THRESHOLD);
    }

    /**
     * Runs the full evaluation pipeline with a configurable label threshold.
     * The threshold defines how many of K1-K4 must be true for a class to count as GOD_CLASS.
     *
     * @param labelsFile       annotated labeling CSV
     * @param secondReviewFile optional second-review CSV (may be null)
     * @param outputDir        output directory
     * @param labelThreshold   number of K1-K4 criteria required (1-4, default 3)
     * @throws IOException              if the labels file cannot be read
     * @throws IllegalArgumentException if labelThreshold is not in range 1-4
     */
    public void evaluate(Path labelsFile, Path secondReviewFile, Path outputDir,
            int labelThreshold) throws IOException {
        if (labelThreshold < MIN_LABEL_THRESHOLD || labelThreshold > MAX_LABEL_THRESHOLD) {
            throw new IllegalArgumentException(
                    "labelThreshold must be between 1 and 4, got: " + labelThreshold);
        }
        LOGGER.info("Evaluate mode started: labelsFile=" + labelsFile
                + " labelThreshold=" + labelThreshold);
        Files.createDirectories(outputDir);

        Map<String, LabelDTO> primaryLabels = new LabelCsvImporter().importLabels(labelsFile);
        Map<String, Boolean> groundTruth = buildGroundTruth(primaryLabels, labelThreshold);
        PredictionData preds = parsePredictionData(labelsFile);
        int total = groundTruth.size();

        Map<String, EvaluationMetrics> toolMetrics = runToolEvaluation(groundTruth, preds, total, outputDir);
        List<OverlapResult> agreement = runAgreementAnalysis(preds, outputDir);
        runErrorAnalysis(groundTruth, preds, outputDir);
        runMetricsSummary(toolMetrics, outputDir);
        ReliabilityMetrics reliability = tryReliabilityEvaluation(primaryLabels, secondReviewFile, outputDir);
        runReport(labelsFile.getFileName().toString(), total, toolMetrics, agreement, reliability, outputDir);

        LOGGER.info("Evaluate mode finished: total=" + total + " labelThreshold=" + labelThreshold);
    }

    /**
     * Builds a ground-truth map by counting how many of K1-K4 are true per label
     * and comparing against the given threshold.
     */
    Map<String, Boolean> buildGroundTruth(Map<String, LabelDTO> labels, int labelThreshold) {
        Map<String, Boolean> truth = new LinkedHashMap<>();
        for (Map.Entry<String, LabelDTO> entry : labels.entrySet()) {
            LabelDTO dto = entry.getValue();
            int trueCount = countTrue(dto.getK1()) + countTrue(dto.getK2())
                    + countTrue(dto.getK3()) + countTrue(dto.getK4());
            truth.put(entry.getKey(), trueCount >= labelThreshold);
        }
        return truth;
    }

    private static int countTrue(Boolean value) {
        return Boolean.TRUE.equals(value) ? 1 : 0;
    }

    private Map<String, EvaluationMetrics> runToolEvaluation(
            Map<String, Boolean> groundTruth, PredictionData preds, int total, Path outputDir) {
        try {
            ToolEvaluator evaluator = new ToolEvaluator();
            Map<String, EvaluationMetrics> metrics = evaluator.evaluateAll(
                    groundTruth, preds.baseline, preds.sonar, preds.jdeodorant, total);
            evaluator.exportJson(metrics, outputDir);
            return metrics;
        } catch (Exception e) {
            LOGGER.warning("Tool evaluation failed: " + e.getMessage());
            return Collections.emptyMap();
        }
    }

    private List<OverlapResult> runAgreementAnalysis(PredictionData preds, Path outputDir) {
        try {
            ToolAgreementCalculator calculator = new ToolAgreementCalculator();
            List<OverlapResult> agreement = calculator.computeAll(
                    preds.baseline, preds.sonar, preds.jdeodorant);
            calculator.exportCsv(agreement, outputDir);
            return agreement;
        } catch (Exception e) {
            LOGGER.warning("Agreement analysis failed: " + e.getMessage());
            return List.of();
        }
    }

    private void runErrorAnalysis(
            Map<String, Boolean> groundTruth, PredictionData preds, Path outputDir) {
        try {
            new ErrorAnalysisExporter().exportAll(
                    groundTruth, preds.baseline, preds.sonar, preds.jdeodorant,
                    preds.candidates, outputDir);
        } catch (Exception e) {
            LOGGER.warning("Error analysis failed: " + e.getMessage());
        }
    }

    private void runMetricsSummary(Map<String, EvaluationMetrics> toolMetrics, Path outputDir) {
        try {
            new MetricsSummaryCsvExporter().exportCsv(toolMetrics, outputDir);
        } catch (Exception e) {
            LOGGER.warning("Metrics summary export failed: " + e.getMessage());
        }
    }

    private ReliabilityMetrics tryReliabilityEvaluation(
            Map<String, LabelDTO> primaryLabels, Path secondReviewFile, Path outputDir) {
        if (secondReviewFile == null) {
            return null;
        }
        try {
            Map<String, LabelDTO> secondary = new LabelCsvImporter().importLabels(secondReviewFile);
            return new SecondReviewEvaluator().evaluate(primaryLabels, secondary, outputDir);
        } catch (Exception e) {
            LOGGER.warning("Reliability evaluation failed: " + e.getMessage());
            return null;
        }
    }

    private void runReport(String projectName, int total, Map<String, EvaluationMetrics> toolMetrics,
            List<OverlapResult> agreement, ReliabilityMetrics reliability, Path outputDir) {
        try {
            new ReportGenerator().generate(projectName, total, toolMetrics, agreement, reliability, outputDir);
        } catch (Exception e) {
            LOGGER.warning("Report generation failed: " + e.getMessage());
        }
    }

    private PredictionData parsePredictionData(Path labelsFile) {
        try {
            List<String> lines = Files.readAllLines(labelsFile, StandardCharsets.UTF_8);
            if (lines.size() < 2) {
                return PredictionData.empty();
            }
            Map<String, Integer> colIdx = buildColumnIndex(CandidateCsvUtil.parseCsvLine(lines.get(0)));
            Set<String> baseline = new HashSet<>();
            Set<String> sonar = new HashSet<>();
            Set<String> jdeodorant = new HashSet<>();
            Map<String, CandidateDTO> candidates = new LinkedHashMap<>();
            for (int i = 1; i < lines.size(); i++) {
                if (lines.get(i).trim().isEmpty()) {
                    continue;
                }
                populatePredictionRow(CandidateCsvUtil.parseCsvLine(lines.get(i)), colIdx,
                        baseline, sonar, jdeodorant, candidates);
            }
            return new PredictionData(baseline, sonar, jdeodorant, candidates);
        } catch (Exception e) {
            LOGGER.warning("Failed to parse prediction data from " + labelsFile + ": " + e.getMessage());
            return PredictionData.empty();
        }
    }

    private void populatePredictionRow(
            List<String> fields, Map<String, Integer> colIdx,
            Set<String> baseline, Set<String> sonar, Set<String> jdeodorant,
            Map<String, CandidateDTO> candidates) {
        String fqcn = getField(fields, colIdx, "fullyQualifiedClassName");
        if (fqcn == null || fqcn.isBlank()) {
            return;
        }
        boolean bl = parseFlag(getField(fields, colIdx, "baselineFlag"));
        boolean so = parseFlag(getField(fields, colIdx, "sonarFlag"));
        boolean jd = parseFlag(getField(fields, colIdx, "jdeodorantFlag"));
        int mc = parseIntField(getField(fields, colIdx, "methodCount"));
        int fc = parseIntField(getField(fields, colIdx, "fieldCount"));
        int dc = parseIntField(getField(fields, colIdx, "dependencyTypeCount"));
        if (bl) { baseline.add(fqcn); }
        if (so) { sonar.add(fqcn); }
        if (jd) { jdeodorant.add(fqcn); }
        candidates.put(fqcn, new CandidateDTO(
                fqcn, bl, so, jd, null, null, null, null, null, mc, fc, dc, List.of()));
    }

    private Map<String, Integer> buildColumnIndex(List<String> headers) {
        Map<String, Integer> idx = new LinkedHashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            idx.put(headers.get(i), i);
        }
        return idx;
    }

    private String getField(List<String> fields, Map<String, Integer> colIdx, String name) {
        Integer i = colIdx.get(name);
        if (i == null || i >= fields.size()) {
            return "";
        }
        return fields.get(i);
    }

    private boolean parseFlag(String value) {
        return "true".equalsIgnoreCase(value) || "1".equals(value) || "yes".equalsIgnoreCase(value);
    }

    private int parseIntField(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static final class PredictionData {
        final Set<String> baseline;
        final Set<String> sonar;
        final Set<String> jdeodorant;
        final Map<String, CandidateDTO> candidates;

        PredictionData(Set<String> baseline, Set<String> sonar, Set<String> jdeodorant,
                Map<String, CandidateDTO> candidates) {
            this.baseline = baseline;
            this.sonar = sonar;
            this.jdeodorant = jdeodorant;
            this.candidates = candidates;
        }

        static PredictionData empty() {
            return new PredictionData(Set.of(), Set.of(), Set.of(), Map.of());
        }
    }

    // -------------------------------------------------------------------------
    // Aggregate mode
    // -------------------------------------------------------------------------

    private static final String METRICS_SUMMARY_FILE = "metrics_summary.csv";

    /**
     * Scans outputRoot sub-directories for metrics_summary.csv, aggregates via
     * MultiProjectAggregator and writes aggregated_metrics.csv + aggregated_report.md.
     *
     * @param outputRoot root directory containing per-project sub-directories
     * @param outputDir  target directory for aggregated output
     * @throws IOException on read/write failure
     */
    public void aggregate(Path outputRoot, Path outputDir) throws IOException {
        LOGGER.info("Aggregate mode started: outputRoot=" + outputRoot);
        Files.createDirectories(outputDir);

        Map<String, Map<String, EvaluationMetrics>> perProject = scanProjects(outputRoot);
        if (perProject.isEmpty()) {
            LOGGER.warning("No metrics_summary.csv files found in " + outputRoot);
            return;
        }
        LOGGER.info("Aggregate: found " + perProject.size() + " projects");

        Map<String, MultiProjectAggregator.AggregatedMetrics> aggregated =
                MultiProjectAggregator.aggregate(perProject);
        MultiProjectAggregator.exportCsv(aggregated, outputDir);

        Map<String, List<OverlapResult>> agreementPerProject = Map.of();
        new ReportGenerator().generateAggregated(
                aggregated, perProject, agreementPerProject, outputDir);

        LOGGER.info("Aggregate mode finished: outputDir=" + outputDir);
    }

    private Map<String, Map<String, EvaluationMetrics>> scanProjects(
            Path outputRoot) throws IOException {
        Map<String, Map<String, EvaluationMetrics>> perProject = new LinkedHashMap<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(outputRoot)) {
            for (Path entry : stream) {
                if (!Files.isDirectory(entry)) {
                    continue;
                }
                Path csvFile = entry.resolve(METRICS_SUMMARY_FILE);
                if (!Files.exists(csvFile)) {
                    continue;
                }
                String projectName = entry.getFileName().toString();
                Map<String, EvaluationMetrics> metrics = parseMetricsSummaryCsv(csvFile);
                if (!metrics.isEmpty()) {
                    perProject.put(projectName, metrics);
                }
            }
        }
        return perProject;
    }

    private Map<String, EvaluationMetrics> parseMetricsSummaryCsv(
            Path csvFile) throws IOException {
        List<String> lines = Files.readAllLines(csvFile, StandardCharsets.UTF_8);
        if (lines.size() < 2) {
            return Map.of();
        }
        Map<String, EvaluationMetrics> result = new LinkedHashMap<>();
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) {
                continue;
            }
            parseMetricsSummaryRow(line, result);
        }
        return result;
    }

    private void parseMetricsSummaryRow(
            String line, Map<String, EvaluationMetrics> result) {
        String[] parts = line.split(",");
        if (parts.length < 10) {
            LOGGER.warning("Skipping malformed metrics row: " + line);
            return;
        }
        try {
            String tool = parts[0].trim();
            int tp = Integer.parseInt(parts[6].trim());
            int fp = Integer.parseInt(parts[7].trim());
            int fn = Integer.parseInt(parts[8].trim());
            int tn = Integer.parseInt(parts[9].trim());
            result.put(tool, MetricsEngine.computeMetrics(tp, fp, fn, tn));
        } catch (NumberFormatException e) {
            LOGGER.warning("Skipping row with invalid numbers: " + line);
        }
    }
}
