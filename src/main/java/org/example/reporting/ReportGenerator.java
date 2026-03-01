package org.example.reporting;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.example.evaluation.OverlapResult;
import org.example.metrics.EvaluationMetrics;
import org.example.metrics.ReliabilityMetrics;

/**
 * Generates a Markdown report ({@code report.md}) summarising God-Class detection results.
 * Sections: metadata/statistics, metrics per tool, confusion matrices, tool agreement, reliability.
 */
public class ReportGenerator {

    private static final Logger LOGGER = Logger.getLogger(ReportGenerator.class.getName());
    private static final String OUTPUT_FILE = "report.md";
    private static final List<String> TOOL_ORDER = List.of("baseline", "jdeodorant", "sonarqube");

    /**
     * Generates the Markdown report and writes it to {@code report.md} in {@code outputDir}.
     *
     * @param projectName     name of the analysed project
     * @param totalCandidates total number of candidate classes
     * @param toolMetrics     per-tool evaluation metrics (tool name → metrics)
     * @param agreement       pairwise tool-agreement results
     * @param reliability     inter-rater reliability metrics (may be null)
     * @param outputDir       target directory (created if absent)
     * @return path of the written file
     * @throws IOException on write failure
     */
    public Path generate(
            String projectName,
            int totalCandidates,
            Map<String, EvaluationMetrics> toolMetrics,
            List<OverlapResult> agreement,
            ReliabilityMetrics reliability,
            Path outputDir
    ) throws IOException {
        LOGGER.info("ReportGenerator.generate started: project=" + projectName);
        Files.createDirectories(outputDir);

        String content = buildHeader(projectName, totalCandidates)
                + buildMetricsTable(toolMetrics)
                + buildConfusionMatrix(toolMetrics)
                + buildAgreementTable(agreement)
                + buildReliabilityTable(reliability);

        Path file = outputDir.resolve(OUTPUT_FILE);
        Files.writeString(file, content, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        LOGGER.info("ReportGenerator.generate finished: file=" + file);
        return file;
    }

    private String buildHeader(String projectName, int totalCandidates) {
        return "# God-Class Detection Report\n\n"
                + "## Metadata\n\n"
                + "| Key | Value |\n"
                + "|-----|-------|\n"
                + "| Project | " + safe(projectName) + " |\n"
                + "| Total Candidates | " + totalCandidates + " |\n\n";
    }

    private String buildMetricsTable(Map<String, EvaluationMetrics> metrics) {
        if (metrics == null || metrics.isEmpty()) {
            return "## Metrics per Tool\n\n_No data._\n\n";
        }
        StringBuilder sb = new StringBuilder("## Metrics per Tool\n\n");
        sb.append("| Tool | Precision | Recall | F1 | Specificity | MCC |\n");
        sb.append("|------|-----------|--------|----|-------------|-----|\n");
        for (String tool : sortKeys(metrics)) {
            EvaluationMetrics m = metrics.get(tool);
            sb.append("| ").append(tool)
              .append(" | ").append(fmt(m.getPrecision()))
              .append(" | ").append(fmt(m.getRecall()))
              .append(" | ").append(fmt(m.getF1Score()))
              .append(" | ").append(fmt(m.getSpecificity()))
              .append(" | ").append(fmt(m.getMcc()))
              .append(" |\n");
        }
        return sb.append("\n").toString();
    }

    private String buildConfusionMatrix(Map<String, EvaluationMetrics> metrics) {
        if (metrics == null || metrics.isEmpty()) {
            return "## Confusion Matrix per Tool\n\n_No data._\n\n";
        }
        StringBuilder sb = new StringBuilder("## Confusion Matrix per Tool\n\n");
        sb.append("| Tool | TP | FP | FN | TN |\n");
        sb.append("|------|----|----|----|----|\n");
        for (String tool : sortKeys(metrics)) {
            EvaluationMetrics m = metrics.get(tool);
            sb.append("| ").append(tool)
              .append(" | ").append(m.getTruePositives())
              .append(" | ").append(m.getFalsePositives())
              .append(" | ").append(m.getFalseNegatives())
              .append(" | ").append(m.getTrueNegatives())
              .append(" |\n");
        }
        return sb.append("\n").toString();
    }

    private String buildAgreementTable(List<OverlapResult> agreement) {
        if (agreement == null || agreement.isEmpty()) {
            return "## Tool Agreement (Jaccard)\n\n_No data._\n\n";
        }
        StringBuilder sb = new StringBuilder("## Tool Agreement (Jaccard)\n\n");
        sb.append("| Tool A | Tool B | Jaccard | Both | Only A | Only B |\n");
        sb.append("|--------|--------|---------|------|--------|--------|\n");
        for (OverlapResult r : agreement) {
            sb.append("| ").append(r.getToolA())
              .append(" | ").append(r.getToolB())
              .append(" | ").append(fmt(r.getJaccard()))
              .append(" | ").append(r.getBoth())
              .append(" | ").append(r.getOnlyA())
              .append(" | ").append(r.getOnlyB())
              .append(" |\n");
        }
        return sb.append("\n").toString();
    }

    private String buildReliabilityTable(ReliabilityMetrics reliability) {
        if (reliability == null) {
            return "## Reliability\n\n_No data._\n\n";
        }
        return "## Reliability\n\n"
                + "| Metric | Value |\n"
                + "|--------|-------|\n"
                + "| Observed Agreement | " + fmt(reliability.getObservedAgreement()) + " |\n"
                + "| Cohen's κ | " + fmt(reliability.getKappa()) + " |\n"
                + "| AC1 | " + fmt(reliability.getAc1()) + " |\n"
                + "| Both Positive | " + reliability.getBothPositive() + " |\n"
                + "| Both Negative | " + reliability.getBothNegative() + " |\n\n";
    }

    private List<String> sortKeys(Map<String, EvaluationMetrics> metrics) {
        List<String> known = new ArrayList<>();
        List<String> others = new ArrayList<>();
        for (String key : metrics.keySet()) {
            if (TOOL_ORDER.contains(key)) {
                known.add(key);
            } else {
                others.add(key);
            }
        }
        known.sort(Comparator.comparingInt(TOOL_ORDER::indexOf));
        Collections.sort(others);
        known.addAll(others);
        return known;
    }

    private String fmt(double value) {
        return String.format(java.util.Locale.ROOT, "%.4f", value);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
