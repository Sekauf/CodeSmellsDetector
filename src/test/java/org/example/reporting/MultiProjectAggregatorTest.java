package org.example.reporting;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.example.metrics.EvaluationMetrics;
import org.example.metrics.MetricsEngine;
import org.example.reporting.MultiProjectAggregator.AggregatedMetrics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class MultiProjectAggregatorTest {

    @TempDir
    Path tempDir;

    private static final double DELTA = 1e-6;

    // -- test data helpers --------------------------------------------------------

    /** Creates EvaluationMetrics via MetricsEngine from raw confusion matrix values. */
    private static EvaluationMetrics metrics(int tp, int fp, int fn, int tn) {
        return MetricsEngine.computeMetrics(tp, fp, fn, tn);
    }

    /**
     * Three projects, one tool ("baseline"):
     * Project A: tp=5, fp=1, fn=2, tn=42
     * Project B: tp=3, fp=2, fn=1, tn=44
     * Project C: tp=4, fp=0, fn=3, tn=43
     */
    private static Map<String, Map<String, EvaluationMetrics>> threeProjectsOneTool() {
        Map<String, Map<String, EvaluationMetrics>> data = new LinkedHashMap<>();
        data.put("projectA", Map.of("baseline", metrics(5, 1, 2, 42)));
        data.put("projectB", Map.of("baseline", metrics(3, 2, 1, 44)));
        data.put("projectC", Map.of("baseline", metrics(4, 0, 3, 43)));
        return data;
    }

    /**
     * Two projects, three tools each.
     */
    private static Map<String, Map<String, EvaluationMetrics>> twoProjectsThreeTools() {
        Map<String, Map<String, EvaluationMetrics>> data = new LinkedHashMap<>();
        data.put("projectA", Map.of(
                "baseline", metrics(5, 1, 2, 42),
                "jdeodorant", metrics(6, 0, 1, 43),
                "sonarqube", metrics(4, 2, 3, 41)
        ));
        data.put("projectB", Map.of(
                "baseline", metrics(3, 2, 1, 44),
                "jdeodorant", metrics(4, 1, 2, 43),
                "sonarqube", metrics(2, 3, 4, 41)
        ));
        return data;
    }

    // -- aggregate: empty / null --------------------------------------------------

    @Test
    void testAggregate_nullInput_returnsEmptyMap() {
        Map<String, AggregatedMetrics> result = MultiProjectAggregator.aggregate(null);
        assertTrue(result.isEmpty());
    }

    @Test
    void testAggregate_emptyInput_returnsEmptyMap() {
        Map<String, AggregatedMetrics> result = MultiProjectAggregator.aggregate(Map.of());
        assertTrue(result.isEmpty());
    }

    // -- aggregate: single project ------------------------------------------------

    @Test
    void testAggregate_singleProject_microEqualsMacro() {
        Map<String, Map<String, EvaluationMetrics>> data = Map.of(
                "only", Map.of("baseline", metrics(5, 1, 2, 42))
        );
        Map<String, AggregatedMetrics> result = MultiProjectAggregator.aggregate(data);

        AggregatedMetrics agg = result.get("baseline");
        assertNotNull(agg);
        assertEquals(1, agg.projectCount());

        // Micro should equal the single-project values
        EvaluationMetrics micro = agg.microAverage();
        assertEquals(micro.getPrecision(), agg.macroPrecision(), DELTA);
        assertEquals(micro.getRecall(), agg.macroRecall(), DELTA);
        assertEquals(micro.getF1Score(), agg.macroF1(), DELTA);
        assertEquals(micro.getMcc(), agg.macroMcc(), DELTA);
    }

    @Test
    void testAggregate_singleProject_stdDevIsZero() {
        Map<String, Map<String, EvaluationMetrics>> data = Map.of(
                "only", Map.of("baseline", metrics(5, 1, 2, 42))
        );
        Map<String, AggregatedMetrics> result = MultiProjectAggregator.aggregate(data);
        AggregatedMetrics agg = result.get("baseline");

        assertEquals(0.0, agg.stdPrecision(), DELTA);
        assertEquals(0.0, agg.stdRecall(), DELTA);
        assertEquals(0.0, agg.stdF1(), DELTA);
        assertEquals(0.0, agg.stdMcc(), DELTA);
    }

    // -- aggregate: micro-average -------------------------------------------------

    @Test
    void testAggregate_threeProjects_microAverageSumsConfusionMatrix() {
        Map<String, AggregatedMetrics> result =
                MultiProjectAggregator.aggregate(threeProjectsOneTool());
        AggregatedMetrics agg = result.get("baseline");

        // tp_total = 5+3+4=12, fp_total = 1+2+0=3, fn_total = 2+1+3=6, tn_total = 42+44+43=129
        EvaluationMetrics micro = agg.microAverage();
        assertEquals(12, micro.getTruePositives());
        assertEquals(3, micro.getFalsePositives());
        assertEquals(6, micro.getFalseNegatives());
        assertEquals(129, micro.getTrueNegatives());

        // precision = 12/(12+3) = 0.8
        assertEquals(0.8, micro.getPrecision(), DELTA);
        // recall = 12/(12+6) = 0.666...
        assertEquals(12.0 / 18.0, micro.getRecall(), DELTA);
    }

    // -- aggregate: macro-average -------------------------------------------------

    @Test
    void testAggregate_threeProjects_macroAverageIsMeanOfPerProject() {
        Map<String, AggregatedMetrics> result =
                MultiProjectAggregator.aggregate(threeProjectsOneTool());
        AggregatedMetrics agg = result.get("baseline");

        // Per-project precision: A=5/6, B=3/5, C=4/4=1.0
        double expectedMacroP = (5.0 / 6.0 + 3.0 / 5.0 + 1.0) / 3.0;
        assertEquals(expectedMacroP, agg.macroPrecision(), DELTA);

        assertEquals(3, agg.projectCount());
    }

    // -- aggregate: population stddev ---------------------------------------------

    @Test
    void testAggregate_threeProjects_stdDevIsPopulationStdDev() {
        Map<String, AggregatedMetrics> result =
                MultiProjectAggregator.aggregate(threeProjectsOneTool());
        AggregatedMetrics agg = result.get("baseline");

        // Per-project precision: A=5/6, B=3/5, C=1.0
        double pA = 5.0 / 6.0;
        double pB = 3.0 / 5.0;
        double pC = 1.0;
        double mean = (pA + pB + pC) / 3.0;
        double variance = ((pA - mean) * (pA - mean)
                + (pB - mean) * (pB - mean)
                + (pC - mean) * (pC - mean)) / 3.0;
        double expectedStd = Math.sqrt(variance);

        assertEquals(expectedStd, agg.stdPrecision(), DELTA);
    }

    // -- aggregate: multiple tools ------------------------------------------------

    @Test
    void testAggregate_twoProjectsThreeTools_allToolsPresent() {
        Map<String, AggregatedMetrics> result =
                MultiProjectAggregator.aggregate(twoProjectsThreeTools());

        assertEquals(3, result.size());
        assertTrue(result.containsKey("baseline"));
        assertTrue(result.containsKey("jdeodorant"));
        assertTrue(result.containsKey("sonarqube"));
    }

    @Test
    void testAggregate_twoProjectsThreeTools_projectCountIsTwo() {
        Map<String, AggregatedMetrics> result =
                MultiProjectAggregator.aggregate(twoProjectsThreeTools());

        for (AggregatedMetrics agg : result.values()) {
            assertEquals(2, agg.projectCount());
        }
    }

    // -- CSV export ---------------------------------------------------------------

    @Test
    void testExportCsv_fileCreated() throws IOException {
        Map<String, AggregatedMetrics> aggregated =
                MultiProjectAggregator.aggregate(twoProjectsThreeTools());
        Path file = MultiProjectAggregator.exportCsv(aggregated, tempDir);

        assertTrue(Files.exists(file));
        assertEquals("aggregated_metrics.csv", file.getFileName().toString());
    }

    @Test
    void testExportCsv_headerCorrect() throws IOException {
        Map<String, AggregatedMetrics> aggregated =
                MultiProjectAggregator.aggregate(twoProjectsThreeTools());
        Path file = MultiProjectAggregator.exportCsv(aggregated, tempDir);

        String firstLine = Files.readAllLines(file).get(0);
        assertEquals(
                "tool,microP,microR,microF1,microMCC,macroP,macroR,macroF1,macroMCC,stdP,stdR,stdF1,stdMCC,projects",
                firstLine);
    }

    @Test
    void testExportCsv_threeDataRows() throws IOException {
        Map<String, AggregatedMetrics> aggregated =
                MultiProjectAggregator.aggregate(twoProjectsThreeTools());
        Path file = MultiProjectAggregator.exportCsv(aggregated, tempDir);

        long dataRows = Files.readAllLines(file).stream()
                .filter(l -> !l.isBlank() && !l.startsWith("tool"))
                .count();
        assertEquals(3, dataRows);
    }

    @Test
    void testExportCsv_sortOrder_baselineJdeodorantSonarqube() throws IOException {
        Map<String, AggregatedMetrics> aggregated =
                MultiProjectAggregator.aggregate(twoProjectsThreeTools());
        Path file = MultiProjectAggregator.exportCsv(aggregated, tempDir);

        List<String> dataLines = Files.readAllLines(file).stream()
                .filter(l -> !l.isBlank() && !l.startsWith("tool"))
                .toList();

        assertTrue(dataLines.get(0).startsWith("baseline"), "Row 1 should be baseline");
        assertTrue(dataLines.get(1).startsWith("jdeodorant"), "Row 2 should be jdeodorant");
        assertTrue(dataLines.get(2).startsWith("sonarqube"), "Row 3 should be sonarqube");
    }

    @Test
    void testExportCsv_fourteenColumns() throws IOException {
        Map<String, AggregatedMetrics> aggregated =
                MultiProjectAggregator.aggregate(twoProjectsThreeTools());
        Path file = MultiProjectAggregator.exportCsv(aggregated, tempDir);

        // Each data row must have exactly 13 commas (14 columns)
        Files.readAllLines(file).stream()
                .filter(l -> !l.isBlank() && !l.startsWith("tool"))
                .forEach(l -> assertEquals(13, l.chars().filter(c -> c == ',').count(),
                        "Wrong column count in row: " + l));
    }

    @Test
    void testExportCsv_emptyAggregated_onlyHeader() throws IOException {
        Path file = MultiProjectAggregator.exportCsv(Map.of(), tempDir);

        List<String> nonBlank = Files.readAllLines(file).stream()
                .filter(l -> !l.isBlank())
                .toList();
        assertEquals(1, nonBlank.size(), "Only header expected for empty input");
    }

    @Test
    void testExportCsv_projectCountInLastColumn() throws IOException {
        Map<String, AggregatedMetrics> aggregated =
                MultiProjectAggregator.aggregate(twoProjectsThreeTools());
        Path file = MultiProjectAggregator.exportCsv(aggregated, tempDir);

        Files.readAllLines(file).stream()
                .filter(l -> !l.isBlank() && !l.startsWith("tool"))
                .forEach(l -> assertTrue(l.endsWith(",2"),
                        "Last column should be project count 2: " + l));
    }
}
