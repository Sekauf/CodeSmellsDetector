package org.example.reporting;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.ToDoubleFunction;
import java.util.logging.Logger;
import org.example.metrics.EvaluationMetrics;
import org.example.metrics.MetricsEngine;

/**
 * Aggregates per-tool EvaluationMetrics across multiple projects
 * into micro-averaged, macro-averaged, and population standard-deviation summaries.
 */
public final class MultiProjectAggregator {

    private static final Logger LOGGER =
            Logger.getLogger(MultiProjectAggregator.class.getName());

    private static final String OUTPUT_FILE = "aggregated_metrics.csv";
    private static final String CSV_HEADER =
            "tool,microP,microR,microF1,microMCC,macroP,macroR,macroF1,macroMCC,"
                    + "stdP,stdR,stdF1,stdMCC,projects";
    private static final List<String> TOOL_ORDER =
            List.of("baseline", "jdeodorant", "sonarqube");

    private MultiProjectAggregator() {
    }

    /**
     * Holds micro-average, macro-average, and population standard-deviation
     * of evaluation metrics aggregated over several projects.
     */
    public record AggregatedMetrics(
            EvaluationMetrics microAverage,
            double macroPrecision, double macroRecall, double macroF1, double macroMcc,
            double stdPrecision, double stdRecall, double stdF1, double stdMcc,
            int projectCount
    ) {}

    /**
     * Aggregates metrics over multiple projects.
     *
     * @param perProject map of project name to (map of tool name to EvaluationMetrics)
     * @return map of tool name to AggregatedMetrics (empty if input is null or empty)
     */
    public static Map<String, AggregatedMetrics> aggregate(
            Map<String, Map<String, EvaluationMetrics>> perProject) {
        if (perProject == null || perProject.isEmpty()) {
            return Map.of();
        }
        LOGGER.info("MultiProjectAggregator.aggregate started: projects=" + perProject.size());

        TreeSet<String> allTools = collectToolNames(perProject);
        Map<String, AggregatedMetrics> result = new LinkedHashMap<>();

        for (String tool : allTools) {
            List<EvaluationMetrics> metricsList = collectMetricsForTool(perProject, tool);
            if (!metricsList.isEmpty()) {
                result.put(tool, aggregateForTool(metricsList));
            }
        }

        LOGGER.info("MultiProjectAggregator.aggregate finished: tools=" + result.size());
        return result;
    }

    /**
     * Exports aggregated metrics as CSV to aggregated_metrics.csv in the output directory.
     *
     * @param aggregated map of tool name to AggregatedMetrics
     * @param outputDir  target directory (created if absent)
     * @return path of the written CSV file
     * @throws IOException on write failure
     */
    public static Path exportCsv(
            Map<String, AggregatedMetrics> aggregated, Path outputDir) throws IOException {
        LOGGER.info("MultiProjectAggregator.exportCsv started: tools="
                + (aggregated == null ? 0 : aggregated.size()));
        Files.createDirectories(outputDir);

        List<String> sortedKeys = sortKeys(aggregated);
        StringBuilder sb = new StringBuilder();
        sb.append(CSV_HEADER).append("\n");
        for (String tool : sortedKeys) {
            sb.append(toRow(tool, aggregated.get(tool))).append("\n");
        }

        Path file = outputDir.resolve(OUTPUT_FILE);
        Files.writeString(file, sb.toString(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        LOGGER.info("MultiProjectAggregator.exportCsv finished: file=" + file);
        return file;
    }

    // -- internal helpers ---------------------------------------------------------

    private static TreeSet<String> collectToolNames(
            Map<String, Map<String, EvaluationMetrics>> perProject) {
        TreeSet<String> tools = new TreeSet<>();
        for (Map<String, EvaluationMetrics> toolMap : perProject.values()) {
            if (toolMap != null) {
                tools.addAll(toolMap.keySet());
            }
        }
        return tools;
    }

    private static List<EvaluationMetrics> collectMetricsForTool(
            Map<String, Map<String, EvaluationMetrics>> perProject, String tool) {
        List<EvaluationMetrics> list = new ArrayList<>();
        for (Map<String, EvaluationMetrics> toolMap : perProject.values()) {
            if (toolMap != null && toolMap.containsKey(tool)) {
                list.add(toolMap.get(tool));
            }
        }
        return list;
    }

    private static AggregatedMetrics aggregateForTool(List<EvaluationMetrics> metricsList) {
        EvaluationMetrics micro = computeMicroAverage(metricsList);
        double[] precisions = extractValues(metricsList, EvaluationMetrics::getPrecision);
        double[] recalls = extractValues(metricsList, EvaluationMetrics::getRecall);
        double[] f1s = extractValues(metricsList, EvaluationMetrics::getF1Score);
        double[] mccs = extractValues(metricsList, EvaluationMetrics::getMcc);

        return new AggregatedMetrics(
                micro,
                mean(precisions), mean(recalls), mean(f1s), mean(mccs),
                populationStdDev(precisions), populationStdDev(recalls),
                populationStdDev(f1s), populationStdDev(mccs),
                metricsList.size()
        );
    }

    private static EvaluationMetrics computeMicroAverage(List<EvaluationMetrics> metricsList) {
        int sumTp = 0;
        int sumFp = 0;
        int sumFn = 0;
        int sumTn = 0;
        for (EvaluationMetrics m : metricsList) {
            sumTp += m.getTruePositives();
            sumFp += m.getFalsePositives();
            sumFn += m.getFalseNegatives();
            sumTn += m.getTrueNegatives();
        }
        return MetricsEngine.computeMetrics(sumTp, sumFp, sumFn, sumTn);
    }

    private static double[] extractValues(
            List<EvaluationMetrics> list, ToDoubleFunction<EvaluationMetrics> fn) {
        double[] values = new double[list.size()];
        for (int i = 0; i < list.size(); i++) {
            values[i] = fn.applyAsDouble(list.get(i));
        }
        return values;
    }

    private static double mean(double[] values) {
        if (values.length == 0) {
            return 0.0;
        }
        double sum = 0.0;
        for (double v : values) {
            sum += v;
        }
        return sum / values.length;
    }

    private static double populationStdDev(double[] values) {
        if (values.length == 0) {
            return 0.0;
        }
        double avg = mean(values);
        double sumSq = 0.0;
        for (double v : values) {
            double diff = v - avg;
            sumSq += diff * diff;
        }
        return Math.sqrt(sumSq / values.length);
    }

    private static List<String> sortKeys(Map<String, AggregatedMetrics> metrics) {
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

    private static String toRow(String tool, AggregatedMetrics a) {
        EvaluationMetrics m = a.microAverage();
        return tool
                + "," + fmt(m.getPrecision())
                + "," + fmt(m.getRecall())
                + "," + fmt(m.getF1Score())
                + "," + fmt(m.getMcc())
                + "," + fmt(a.macroPrecision())
                + "," + fmt(a.macroRecall())
                + "," + fmt(a.macroF1())
                + "," + fmt(a.macroMcc())
                + "," + fmt(a.stdPrecision())
                + "," + fmt(a.stdRecall())
                + "," + fmt(a.stdF1())
                + "," + fmt(a.stdMcc())
                + "," + a.projectCount();
    }

    private static String fmt(double value) {
        return String.format(Locale.ROOT, "%.4f", value);
    }
}
