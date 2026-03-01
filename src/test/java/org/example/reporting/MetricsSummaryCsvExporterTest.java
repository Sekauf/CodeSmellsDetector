package org.example.reporting;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.example.metrics.EvaluationMetrics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class MetricsSummaryCsvExporterTest {

    @TempDir
    Path tempDir;

    private static EvaluationMetrics metrics(int tp, int fp, int fn, int tn,
                                              double precision, double recall,
                                              double f1, double mcc, double specificity) {
        return new EvaluationMetrics(tp, fp, fn, tn, precision, recall, f1, mcc, specificity);
    }

    private static Map<String, EvaluationMetrics> threeTools() {
        // Deliberately inserted out-of-order to verify sorting
        Map<String, EvaluationMetrics> map = new LinkedHashMap<>();
        map.put("sonarqube",  metrics(3, 1, 2, 4, 0.75, 0.6,  0.666, 0.45, 0.8));
        map.put("baseline",   metrics(5, 0, 1, 4, 1.0,  0.83, 0.909, 0.9,  1.0));
        map.put("jdeodorant", metrics(4, 2, 1, 3, 0.67, 0.8,  0.727, 0.5,  0.6));
        return map;
    }

    // ── file creation ─────────────────────────────────────────────────────────

    @Test
    void testExportCsv_fileCreated() throws IOException {
        Path file = new MetricsSummaryCsvExporter().exportCsv(threeTools(), tempDir);

        assertTrue(Files.exists(file));
        assertEquals("metrics_summary.csv", file.getFileName().toString());
    }

    @Test
    void testExportCsv_headerCorrect() throws IOException {
        Path file = new MetricsSummaryCsvExporter().exportCsv(threeTools(), tempDir);

        String firstLine = Files.readAllLines(file).get(0);
        assertEquals("tool,precision,recall,f1,specificity,mcc,tp,fp,fn,tn", firstLine);
    }

    // ── row count ─────────────────────────────────────────────────────────────

    @Test
    void testExportCsv_threeDataRows() throws IOException {
        Path file = new MetricsSummaryCsvExporter().exportCsv(threeTools(), tempDir);

        long dataRows = Files.readAllLines(file).stream()
                .filter(l -> !l.isBlank() && !l.startsWith("tool"))
                .count();
        assertEquals(3, dataRows);
    }

    // ── sort order ────────────────────────────────────────────────────────────

    @Test
    void testExportCsv_sortOrder_baselineJdeodorantSonarqube() throws IOException {
        Path file = new MetricsSummaryCsvExporter().exportCsv(threeTools(), tempDir);

        List<String> dataLines = Files.readAllLines(file).stream()
                .filter(l -> !l.isBlank() && !l.startsWith("tool"))
                .toList();

        assertTrue(dataLines.get(0).startsWith("baseline"),   "Row 1 should be baseline");
        assertTrue(dataLines.get(1).startsWith("jdeodorant"), "Row 2 should be jdeodorant");
        assertTrue(dataLines.get(2).startsWith("sonarqube"),  "Row 3 should be sonarqube");
    }

    // ── values ────────────────────────────────────────────────────────────────

    @Test
    void testExportCsv_baselineRowValues() throws IOException {
        Path file = new MetricsSummaryCsvExporter().exportCsv(threeTools(), tempDir);

        String baselineLine = Files.readAllLines(file).stream()
                .filter(l -> l.startsWith("baseline"))
                .findFirst()
                .orElseThrow();

        // baseline: tp=5, fp=0, fn=1, tn=4
        assertTrue(baselineLine.contains(",5,0,1,4"), "baseline confusion matrix mismatch: " + baselineLine);
    }

    @Test
    void testExportCsv_allColumnsPresent() throws IOException {
        Path file = new MetricsSummaryCsvExporter().exportCsv(threeTools(), tempDir);

        String content = Files.readString(file);
        // Each data row must have exactly 9 commas (10 columns)
        Files.readAllLines(file).stream()
                .filter(l -> !l.isBlank() && !l.startsWith("tool"))
                .forEach(l -> assertEquals(9, l.chars().filter(c -> c == ',').count(),
                        "Wrong column count in row: " + l));
    }

    // ── edge cases ────────────────────────────────────────────────────────────

    @Test
    void testExportCsv_extraToolSortedAlphabeticallyAfterKnown() throws IOException {
        Map<String, EvaluationMetrics> map = new LinkedHashMap<>(threeTools());
        map.put("mytool", metrics(1, 1, 1, 1, 0.5, 0.5, 0.5, 0.0, 0.5));

        Path file = new MetricsSummaryCsvExporter().exportCsv(map, tempDir);

        List<String> dataLines = Files.readAllLines(file).stream()
                .filter(l -> !l.isBlank() && !l.startsWith("tool"))
                .toList();

        assertEquals(4, dataLines.size());
        assertTrue(dataLines.get(3).startsWith("mytool"), "Extra tool should appear last");
    }

    @Test
    void testExportCsv_emptyMap_onlyHeader() throws IOException {
        Path file = new MetricsSummaryCsvExporter().exportCsv(Map.of(), tempDir);

        List<String> nonBlank = Files.readAllLines(file).stream()
                .filter(l -> !l.isBlank())
                .toList();
        assertEquals(1, nonBlank.size(), "Only header expected for empty input");
    }
}
