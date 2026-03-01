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
import org.example.baseline.CandidateCsvUtil;
import org.example.metrics.EvaluationMetrics;

/**
 * Exports a one-row-per-tool metrics summary as {@code metrics_summary.csv}.
 * Rows are sorted: baseline, jdeodorant, sonarqube, then any additional tools alphabetically.
 */
public class MetricsSummaryCsvExporter {

    private static final Logger LOGGER = Logger.getLogger(MetricsSummaryCsvExporter.class.getName());

    private static final String OUTPUT_FILE = "metrics_summary.csv";
    private static final String CSV_HEADER = "tool,precision,recall,f1,specificity,mcc,tp,fp,fn,tn";
    private static final List<String> TOOL_ORDER = List.of("baseline", "jdeodorant", "sonarqube");

    /**
     * Exports the metrics summary to {@code metrics_summary.csv} in {@code outputDir}.
     *
     * @param metrics   map of tool name to EvaluationMetrics
     * @param outputDir target directory (created if absent)
     * @return path of the written CSV file
     * @throws IOException on write failure
     */
    public Path exportCsv(Map<String, EvaluationMetrics> metrics, Path outputDir) throws IOException {
        LOGGER.info("MetricsSummaryCsvExporter.exportCsv started: tools="
                + (metrics == null ? 0 : metrics.size()));
        Files.createDirectories(outputDir);

        List<String> sortedKeys = sortKeys(metrics);

        StringBuilder sb = new StringBuilder();
        sb.append(CSV_HEADER).append("\n");
        for (String tool : sortedKeys) {
            sb.append(toRow(tool, metrics.get(tool))).append("\n");
        }

        Path file = outputDir.resolve(OUTPUT_FILE);
        Files.writeString(file, sb.toString(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        LOGGER.info("MetricsSummaryCsvExporter.exportCsv finished: file=" + file
                + " rows=" + sortedKeys.size());
        return file;
    }

    private List<String> sortKeys(Map<String, EvaluationMetrics> metrics) {
        if (metrics == null || metrics.isEmpty()) {
            return List.of();
        }
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

    private String toRow(String tool, EvaluationMetrics m) {
        return CandidateCsvUtil.escapeCsvField(tool)
                + "," + m.getPrecision()
                + "," + m.getRecall()
                + "," + m.getF1Score()
                + "," + m.getSpecificity()
                + "," + m.getMcc()
                + "," + m.getTruePositives()
                + "," + m.getFalsePositives()
                + "," + m.getFalseNegatives()
                + "," + m.getTrueNegatives();
    }
}
