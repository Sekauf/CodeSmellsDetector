package org.example.evaluation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import org.example.metrics.EvaluationMetrics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class ToolEvaluatorTest {

    @TempDir
    Path tempDir;

    // Ground truth: A and B are God Classes; C, D, E are not
    private static final Map<String, Boolean> GT = Map.of(
            "A", true,
            "B", true,
            "C", false,
            "D", false,
            "E", false
    );
    private static final int TOTAL = 5;

    // ── evaluate() ────────────────────────────────────────────────────────────

    @Test
    void testEvaluateSingleTool_partialPrediction() {
        // predictions: {A, C} → tp=1(A), fp=1(C), fn=1(B), tn=2(D,E)
        EvaluationMetrics m = new ToolEvaluator().evaluate(GT, Set.of("A", "C"), TOTAL);

        assertEquals(1, m.getTruePositives());
        assertEquals(1, m.getFalsePositives());
        assertEquals(1, m.getFalseNegatives());
        assertEquals(2, m.getTrueNegatives());
        assertEquals(0.5, m.getPrecision(), 1e-9);
        assertEquals(0.5, m.getRecall(), 1e-9);
        assertEquals(0.5, m.getF1Score(), 1e-9);
        assertEquals(2.0 / 3.0, m.getSpecificity(), 1e-9); // tn=2, fp=1 → 2/3
    }

    @Test
    void testEvaluateSingleTool_perfectPrediction() {
        // predictions: {A, B} → tp=2, fp=0, fn=0, tn=3
        EvaluationMetrics m = new ToolEvaluator().evaluate(GT, Set.of("A", "B"), TOTAL);

        assertEquals(2, m.getTruePositives());
        assertEquals(0, m.getFalsePositives());
        assertEquals(0, m.getFalseNegatives());
        assertEquals(3, m.getTrueNegatives());
        assertEquals(1.0, m.getPrecision(), 1e-9);
        assertEquals(1.0, m.getRecall(), 1e-9);
        assertEquals(1.0, m.getF1Score(), 1e-9);
        assertEquals(1.0, m.getSpecificity(), 1e-9); // tn=3, fp=0 → 1.0
    }

    @Test
    void testEvaluateSingleTool_emptyPredictions() {
        // no predictions → tp=0, fp=0, fn=2, tn=3
        EvaluationMetrics m = new ToolEvaluator().evaluate(GT, Set.of(), TOTAL);

        assertEquals(0, m.getTruePositives());
        assertEquals(0, m.getFalsePositives());
        assertEquals(2, m.getFalseNegatives());
        assertEquals(3, m.getTrueNegatives());
        assertEquals(0.0, m.getPrecision(), 1e-9);
        assertEquals(0.0, m.getRecall(), 1e-9);
        assertEquals(1.0, m.getSpecificity(), 1e-9); // tn=3, fp=0 → 1.0
    }

    // ── evaluateAll() ─────────────────────────────────────────────────────────

    @Test
    void testEvaluateAll_correctToolKeys() {
        Map<String, EvaluationMetrics> results = new ToolEvaluator()
                .evaluateAll(GT, Set.of("A", "C"), Set.of("A", "B"), Set.of("C", "D"), TOTAL);

        assertEquals(3, results.size());
        assertTrue(results.containsKey("baseline"));
        assertTrue(results.containsKey("sonar"));
        assertTrue(results.containsKey("jdeodorant"));
    }

    @Test
    void testEvaluateAll_metricsPerTool() {
        // baseline: {A,C} → precision=0.5, recall=0.5
        // sonar:    {A,B} → precision=1.0, recall=1.0
        // jdeo:     {C,D} → precision=0.0, recall=0.0
        Map<String, EvaluationMetrics> results = new ToolEvaluator()
                .evaluateAll(GT, Set.of("A", "C"), Set.of("A", "B"), Set.of("C", "D"), TOTAL);

        EvaluationMetrics baseline = results.get("baseline");
        assertEquals(0.5, baseline.getPrecision(), 1e-9);
        assertEquals(0.5, baseline.getRecall(), 1e-9);

        EvaluationMetrics sonar = results.get("sonar");
        assertEquals(1.0, sonar.getPrecision(), 1e-9);
        assertEquals(1.0, sonar.getRecall(), 1e-9);
        assertEquals(1.0, sonar.getSpecificity(), 1e-9);

        EvaluationMetrics jdeo = results.get("jdeodorant");
        assertEquals(0.0, jdeo.getPrecision(), 1e-9);
        assertEquals(0.0, jdeo.getRecall(), 1e-9);
        // tn=1, fp=2 → specificity = 1/(1+2) = 1/3
        assertEquals(1.0 / 3.0, jdeo.getSpecificity(), 1e-9);
    }

    // ── exportJson() ──────────────────────────────────────────────────────────

    @Test
    void testExportJson_fileContainsExpectedContent() throws IOException {
        Map<String, EvaluationMetrics> results = new ToolEvaluator()
                .evaluateAll(GT, Set.of("A"), Set.of("A", "B"), Set.of(), TOTAL);

        Path file = new ToolEvaluator().exportJson(results, tempDir);

        assertTrue(Files.exists(file));
        assertEquals("evaluation_per_tool.json", file.getFileName().toString());

        String content = Files.readString(file);
        assertTrue(content.contains("\"baseline\""));
        assertTrue(content.contains("\"sonar\""));
        assertTrue(content.contains("\"jdeodorant\""));
        assertTrue(content.contains("\"truePositives\""));
        assertTrue(content.contains("\"specificity\""));
    }
}
