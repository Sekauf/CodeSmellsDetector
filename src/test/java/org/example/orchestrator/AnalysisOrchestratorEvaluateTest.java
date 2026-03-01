package org.example.orchestrator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalysisOrchestratorEvaluateTest {

    @TempDir
    Path tempDir;

    /** Full labeling CSV with tool flags — mirrors the format written by LabelCsvExporter. */
    private static final String FULL_CSV =
            "fullyQualifiedClassName,baselineFlag,sonarFlag,jdeodorantFlag,"
            + "methodCount,fieldCount,dependencyTypeCount,k1,k2,k3,k4,comment,finalLabel\n"
            + "com.example.GodClass,true,true,false,50,20,10,true,true,true,false,,GOD_CLASS\n"
            + "com.example.NotGod,false,false,false,5,2,1,false,false,false,false,,NO\n"
            + "com.example.SonarFp,false,true,false,30,10,5,false,false,false,false,,NO\n";

    /**
     * US-12: --evaluate with valid labels CSV writes all expected output files.
     */
    @Test
    void evaluate_withValidCsv_writesAllExpectedOutputFiles() throws IOException {
        Path labelsFile = tempDir.resolve("labeling_input.csv");
        Files.writeString(labelsFile, FULL_CSV, StandardCharsets.UTF_8);

        new AnalysisOrchestrator().evaluate(labelsFile, null, tempDir);

        assertTrue(Files.exists(tempDir.resolve("evaluation_per_tool.json")),
                "evaluation_per_tool.json must exist");
        assertTrue(Files.exists(tempDir.resolve("tool_agreement.csv")),
                "tool_agreement.csv must exist");
        assertTrue(Files.exists(tempDir.resolve("fp_fn_baseline.csv")),
                "fp_fn_baseline.csv must exist");
        assertTrue(Files.exists(tempDir.resolve("fp_fn_sonar.csv")),
                "fp_fn_sonar.csv must exist");
        assertTrue(Files.exists(tempDir.resolve("fp_fn_jdeodorant.csv")),
                "fp_fn_jdeodorant.csv must exist");
        assertTrue(Files.exists(tempDir.resolve("metrics_summary.csv")),
                "metrics_summary.csv must exist");
        assertTrue(Files.exists(tempDir.resolve("report.md")),
                "report.md must exist");
    }

    /**
     * US-12: --evaluate --second-review-labels additionally writes reliability output.
     */
    @Test
    void evaluate_withSecondReviewLabels_writesReliabilityOutput() throws IOException {
        Path labelsFile = tempDir.resolve("labeling_input.csv");
        Path secondReviewFile = tempDir.resolve("second_review.csv");
        Files.writeString(labelsFile, FULL_CSV, StandardCharsets.UTF_8);
        Files.writeString(secondReviewFile, FULL_CSV, StandardCharsets.UTF_8);

        new AnalysisOrchestrator().evaluate(labelsFile, secondReviewFile, tempDir);

        assertTrue(Files.exists(tempDir.resolve("second_review_conflicts.csv")),
                "second_review_conflicts.csv must exist");
        assertTrue(Files.exists(tempDir.resolve("reliability.json")),
                "reliability.json must exist");
    }

    /**
     * US-12: evaluate with non-existent labels file propagates IOException.
     */
    @Test
    void evaluate_withNonExistentLabelsFile_throwsIOException() {
        Path missing = tempDir.resolve("nonexistent.csv");
        assertThrows(IOException.class,
                () -> new AnalysisOrchestrator().evaluate(missing, null, tempDir));
    }

    /**
     * US-12: evaluate with a CSV that has no tool-flag columns still completes without crash.
     * Predictions default to empty sets; evaluation metrics are computed on empty sets.
     */
    @Test
    void evaluate_withMinimalCsvNoPredictionColumns_completesWithoutCrash() throws IOException {
        String minimalCsv = "fullyQualifiedClassName,k1,k2,k3,k4,comment,finalLabel\n"
                + "com.example.GodClass,true,true,true,false,,GOD_CLASS\n"
                + "com.example.NotGod,false,false,false,false,,NO\n";
        Path labelsFile = tempDir.resolve("minimal.csv");
        Files.writeString(labelsFile, minimalCsv, StandardCharsets.UTF_8);

        assertDoesNotThrow(() -> new AnalysisOrchestrator().evaluate(labelsFile, null, tempDir));
        assertTrue(Files.exists(tempDir.resolve("report.md")), "report.md must exist even with minimal CSV");
    }
}
