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
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import org.example.reporting.MultiProjectAggregator.AggregatedMetrics;

/**
 * Generates Markdown reports summarising God-Class detection results.
 * Supports single-project ({@code report.md}) and multi-project ({@code aggregated_report.md}).
 */
public class ReportGenerator {

    private static final Logger LOGGER = Logger.getLogger(ReportGenerator.class.getName());
    private static final String OUTPUT_FILE = "report.md";
    private static final String AGGREGATED_FILE = "aggregated_report.md";
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

    /**
     * Generates aggregated_report.md summarising God-Class detection across multiple projects.
     *
     * @param aggregated          per-tool aggregated metrics (micro/macro)
     * @param perProject          per-project per-tool evaluation metrics
     * @param agreementPerProject per-project pairwise tool-agreement results
     * @param outputDir           target directory (created if absent)
     * @return path of the written file
     * @throws IOException on write failure
     */
    public Path generateAggregated(
            Map<String, AggregatedMetrics> aggregated,
            Map<String, Map<String, EvaluationMetrics>> perProject,
            Map<String, List<OverlapResult>> agreementPerProject,
            Path outputDir
    ) throws IOException {
        LOGGER.info("ReportGenerator.generateAggregated started");
        Files.createDirectories(outputDir);

        String content = buildAggregatedHeader()
                + buildPerProjectSummary(perProject)
                + buildMicroAverageTable(aggregated)
                + buildMacroAverageTable(aggregated)
                + buildAgreementPerProject(agreementPerProject);

        Path file = outputDir.resolve(AGGREGATED_FILE);
        Files.writeString(file, content, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        LOGGER.info("ReportGenerator.generateAggregated finished: file=" + file);
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
        return sortToolKeys(metrics.keySet());
    }

    private List<String> sortToolKeys(Set<String> keys) {
        List<String> known = new ArrayList<>();
        List<String> others = new ArrayList<>();
        for (String key : keys) {
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
        return String.format(Locale.ROOT, "%.4f", value);
    }


    // -- aggregated report helpers --------------------------------------------

    private String buildAggregatedHeader() {
        return "# God-Class Detection \u2013 Aggregated Report\n\n";
    }

    private String buildPerProjectSummary(Map<String, Map<String, EvaluationMetrics>> perProject) {
        if (perProject == null || perProject.isEmpty()) {
            return "## Per-Project Summary\n\n_No data._\n\n";
        }
        StringBuilder sb = new StringBuilder("## Per-Project Summary\n\n");
        sb.append("| Project | Tool | P | R | F1 | MCC | TP | FP | FN | TN |\n");
        sb.append("|---------|------|---|---|----|----|----|----|----|----|----|\n");
        for (String project : new TreeSet<>(perProject.keySet())) {
            Map<String, EvaluationMetrics> tools = perProject.get(project);
            if (tools == null) {
                continue;
            }
            for (String tool : sortToolKeys(tools.keySet())) {
                appendPerProjectRow(sb, project, tool, tools.get(tool));
            }
        }
        return sb.append("\n").toString();
    }

    private void appendPerProjectRow(StringBuilder sb, String project, String tool,
                                     EvaluationMetrics m) {
        sb.append("| ").append(project)
          .append(" | ").append(tool)
          .append(" | ").append(fmt(m.getPrecision()))
          .append(" | ").append(fmt(m.getRecall()))
          .append(" | ").append(fmt(m.getF1Score()))
          .append(" | ").append(fmt(m.getMcc()))
          .append(" | ").append(m.getTruePositives())
          .append(" | ").append(m.getFalsePositives())
          .append(" | ").append(m.getFalseNegatives())
          .append(" | ").append(m.getTrueNegatives())
          .append(" |\n");
    }

    private String buildMicroAverageTable(Map<String, AggregatedMetrics> aggregated) {
        if (aggregated == null || aggregated.isEmpty()) {
            return "## Aggregated Metrics (Micro-Average)\n\n_No data._\n\n";
        }
        StringBuilder sb = new StringBuilder("## Aggregated Metrics (Micro-Average)\n\n");
        sb.append("| Tool | P | R | F1 | MCC |\n");
        sb.append("|------|---|---|----|-----|\n");
        for (String tool : sortToolKeys(aggregated.keySet())) {
            EvaluationMetrics m = aggregated.get(tool).microAverage();
            sb.append("| ").append(tool)
              .append(" | ").append(fmt(m.getPrecision()))
              .append(" | ").append(fmt(m.getRecall()))
              .append(" | ").append(fmt(m.getF1Score()))
              .append(" | ").append(fmt(m.getMcc()))
              .append(" |\n");
        }
        return sb.append("\n").toString();
    }

    private String buildMacroAverageTable(Map<String, AggregatedMetrics> aggregated) {
        if (aggregated == null || aggregated.isEmpty()) {
            return "## Aggregated Metrics (Macro-Average \u00b1 StdDev)\n\n_No data._\n\n";
        }
        StringBuilder sb = new StringBuilder("## Aggregated Metrics (Macro-Average \u00b1 StdDev)\n\n");
        sb.append("| Tool | P | R | F1 | MCC |\n");
        sb.append("|------|---|---|----|-----|\n");
        for (String tool : sortToolKeys(aggregated.keySet())) {
            AggregatedMetrics a = aggregated.get(tool);
            sb.append("| ").append(tool)
              .append(" | ").append(fmtMacro(a.macroPrecision(), a.stdPrecision()))
              .append(" | ").append(fmtMacro(a.macroRecall(), a.stdRecall()))
              .append(" | ").append(fmtMacro(a.macroF1(), a.stdF1()))
              .append(" | ").append(fmtMacro(a.macroMcc(), a.stdMcc()))
              .append(" |\n");
        }
        return sb.append("\n").toString();
    }

    private String buildAgreementPerProject(Map<String, List<OverlapResult>> agreementPerProject) {
        if (agreementPerProject == null || agreementPerProject.isEmpty()) {
            return "## Tool Agreement (per project)\n\n_No data._\n\n";
        }
        StringBuilder sb = new StringBuilder("## Tool Agreement (per project)\n\n");
        for (String project : new TreeSet<>(agreementPerProject.keySet())) {
            List<OverlapResult> results = agreementPerProject.get(project);
            if (results == null || results.isEmpty()) {
                continue;
            }
            sb.append("### ").append(project).append("\n\n");
            sb.append("| Tool A | Tool B | Jaccard | Both | Only A | Only B |\n");
            sb.append("|--------|--------|---------|------|--------|--------|\n");
            for (OverlapResult r : results) {
                sb.append("| ").append(r.getToolA())
                  .append(" | ").append(r.getToolB())
                  .append(" | ").append(fmt(r.getJaccard()))
                  .append(" | ").append(r.getBoth())
                  .append(" | ").append(r.getOnlyA())
                  .append(" | ").append(r.getOnlyB())
                  .append(" |\n");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private String fmtMacro(double mean, double stdDev) {
        return String.format(Locale.ROOT, "%.4f \u00b1 %.4f", mean, stdDev);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
