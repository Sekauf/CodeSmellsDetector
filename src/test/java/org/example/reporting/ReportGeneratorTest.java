package org.example.reporting;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.example.evaluation.OverlapResult;
import org.example.metrics.EvaluationMetrics;
import org.example.metrics.ReliabilityMetrics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class ReportGeneratorTest {

    @TempDir
    Path tempDir;

    private static final String PROJECT = "my-project";
    private static final int TOTAL = 50;

    private static Map<String, EvaluationMetrics> toolMetrics() {
        return Map.of(
                "baseline",   new EvaluationMetrics(5, 1, 2, 42, 0.8333, 0.7143, 0.7692, 0.7206, 0.9767),
                "sonarqube",  new EvaluationMetrics(4, 2, 3, 41, 0.6667, 0.5714, 0.6154, 0.5443, 0.9535),
                "jdeodorant", new EvaluationMetrics(6, 0, 1, 43, 1.0,    0.8571, 0.9231, 0.9224, 1.0)
        );
    }

    private static List<OverlapResult> agreement() {
        return List.of(
                new OverlapResult("baseline", "jdeodorant", 0.625, 5, 2, 1),
                new OverlapResult("baseline", "sonarqube",  0.5,   4, 2, 2),
                new OverlapResult("jdeodorant", "sonarqube", 0.4,  4, 3, 2)
        );
    }

    private static ReliabilityMetrics reliability() {
        return new ReliabilityMetrics(6, 1, 2, 41, 0.94, 0.72, 0.85);
    }

    // ── file creation ─────────────────────────────────────────────────────────

    @Test
    void testGenerate_fileCreated() throws IOException {
        Path file = new ReportGenerator()
                .generate(PROJECT, TOTAL, toolMetrics(), agreement(), reliability(), tempDir);

        assertTrue(Files.exists(file));
        assertEquals("report.md", file.getFileName().toString());
    }

    // ── metadata section ──────────────────────────────────────────────────────

    @Test
    void testGenerate_containsTitle() throws IOException {
        String content = generateReport();
        assertTrue(content.contains("# God-Class Detection Report"));
    }

    @Test
    void testGenerate_containsProjectName() throws IOException {
        String content = generateReport();
        assertTrue(content.contains(PROJECT));
    }

    @Test
    void testGenerate_containsTotalCandidates() throws IOException {
        String content = generateReport();
        assertTrue(content.contains(String.valueOf(TOTAL)));
    }

    // ── metrics section ───────────────────────────────────────────────────────

    @Test
    void testGenerate_containsMetricsSectionHeader() throws IOException {
        String content = generateReport();
        assertTrue(content.contains("## Metrics per Tool"));
    }

    @Test
    void testGenerate_metricsTableHeaderCorrect() throws IOException {
        String content = generateReport();
        assertTrue(content.contains("| Tool | Precision | Recall | F1 | Specificity | MCC |"));
    }

    @Test
    void testGenerate_metricsTableContainsAllTools() throws IOException {
        String content = generateReport();
        assertTrue(content.contains("| baseline |"));
        assertTrue(content.contains("| jdeodorant |"));
        assertTrue(content.contains("| sonarqube |"));
    }

    @Test
    void testGenerate_metricsTableSortOrder() throws IOException {
        String content = generateReport();
        int posBaseline   = content.indexOf("| baseline |");
        int posJdeodorant = content.indexOf("| jdeodorant |");
        int posSonar      = content.indexOf("| sonarqube |");
        assertTrue(posBaseline < posJdeodorant, "baseline should precede jdeodorant");
        assertTrue(posJdeodorant < posSonar,    "jdeodorant should precede sonarqube");
    }

    // ── confusion matrix section ───────────────────────────────────────────────

    @Test
    void testGenerate_containsConfusionMatrixHeader() throws IOException {
        String content = generateReport();
        assertTrue(content.contains("## Confusion Matrix per Tool"));
    }

    @Test
    void testGenerate_confusionMatrixColumnsPresent() throws IOException {
        String content = generateReport();
        assertTrue(content.contains("| Tool | TP | FP | FN | TN |"));
    }

    @Test
    void testGenerate_confusionMatrixValues() throws IOException {
        String content = generateReport();
        // jdeodorant: tp=6, fp=0, fn=1, tn=43
        assertTrue(content.contains("| 6 | 0 | 1 | 43 |"));
    }

    // ── agreement section ─────────────────────────────────────────────────────

    @Test
    void testGenerate_containsAgreementHeader() throws IOException {
        String content = generateReport();
        assertTrue(content.contains("## Tool Agreement (Jaccard)"));
    }

    @Test
    void testGenerate_agreementTableColumnsPresent() throws IOException {
        String content = generateReport();
        assertTrue(content.contains("| Tool A | Tool B | Jaccard | Both | Only A | Only B |"));
    }

    @Test
    void testGenerate_agreementValues() throws IOException {
        String content = generateReport();
        // baseline-jdeodorant: jaccard=0.625, both=5
        assertTrue(content.contains("0.6250"));
        assertTrue(content.contains("| baseline | jdeodorant |"));
    }

    // ── reliability section ───────────────────────────────────────────────────

    @Test
    void testGenerate_containsReliabilityHeader() throws IOException {
        String content = generateReport();
        assertTrue(content.contains("## Reliability"));
    }

    @Test
    void testGenerate_reliabilityValues() throws IOException {
        String content = generateReport();
        assertTrue(content.contains("0.9400")); // observed agreement
        assertTrue(content.contains("0.7200")); // kappa
        assertTrue(content.contains("0.8500")); // ac1
    }

    // ── null / empty guards ───────────────────────────────────────────────────

    @Test
    void testGenerate_nullReliability_showsNoData() throws IOException {
        Path file = new ReportGenerator()
                .generate(PROJECT, TOTAL, toolMetrics(), agreement(), null, tempDir);
        String content = Files.readString(file);
        assertTrue(content.contains("## Reliability"));
        assertTrue(content.contains("_No data._"));
    }

    @Test
    void testGenerate_emptyAgreement_showsNoData() throws IOException {
        Path file = new ReportGenerator()
                .generate(PROJECT, TOTAL, toolMetrics(), List.of(), reliability(), tempDir);
        String content = Files.readString(file);
        assertTrue(content.contains("## Tool Agreement (Jaccard)"));
        assertTrue(content.contains("_No data._"));
    }

    // ── helper ────────────────────────────────────────────────────────────────

    private String generateReport() throws IOException {
        Path file = new ReportGenerator()
                .generate(PROJECT, TOTAL, toolMetrics(), agreement(), reliability(), tempDir);
        return Files.readString(file);
    }
}
