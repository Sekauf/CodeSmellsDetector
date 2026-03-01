package org.example.labeling;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.example.labeling.LabelDTO.FinalLabel;
import org.example.metrics.ReliabilityMetrics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class SecondReviewEvaluatorTest {

    @TempDir
    Path tempDir;

    // ── helpers ──────────────────────────────────────────────────────────────

    private LabelDTO label(String fqcn, FinalLabel finalLabel) {
        return new LabelDTO(fqcn, null, null, null, null, null, finalLabel);
    }

    // ── SecondReviewEvaluator tests ───────────────────────────────────────────

    /**
     * 10 overlapping classes, 2 conflicts:
     *   6 × both GOD_CLASS  → bothPositive = 6
     *   2 × both NO         → bothNegative = 2
     *   1 × GOD_CLASS vs NO → rater1+/rater2- = 1
     *   1 × NO vs GOD_CLASS → rater1-/rater2+ = 1
     *
     * Expected: agreement=0.8, kappa=0.22/0.42, ac1=0.38/0.58
     */
    @Test
    void testEvaluateTenLabelsWithTwoConflicts() throws IOException {
        Map<String, LabelDTO> primary = new LinkedHashMap<>();
        Map<String, LabelDTO> secondary = new LinkedHashMap<>();

        // 6 agreements: GOD_CLASS
        for (int i = 1; i <= 6; i++) {
            String fqcn = "com.example.ClassA" + i;
            primary.put(fqcn, label(fqcn, FinalLabel.GOD_CLASS));
            secondary.put(fqcn, label(fqcn, FinalLabel.GOD_CLASS));
        }
        // 2 agreements: NO
        for (int i = 1; i <= 2; i++) {
            String fqcn = "com.example.ClassB" + i;
            primary.put(fqcn, label(fqcn, FinalLabel.NO));
            secondary.put(fqcn, label(fqcn, FinalLabel.NO));
        }
        // Conflict 1: primary=GOD_CLASS, secondary=NO
        primary.put("com.example.Conflict1", label("com.example.Conflict1", FinalLabel.GOD_CLASS));
        secondary.put("com.example.Conflict1", label("com.example.Conflict1", FinalLabel.NO));
        // Conflict 2: primary=NO, secondary=GOD_CLASS
        primary.put("com.example.Conflict2", label("com.example.Conflict2", FinalLabel.NO));
        secondary.put("com.example.Conflict2", label("com.example.Conflict2", FinalLabel.GOD_CLASS));

        SecondReviewEvaluator evaluator = new SecondReviewEvaluator();
        ReliabilityMetrics metrics = evaluator.evaluate(primary, secondary, tempDir);

        // agreement = (6+2)/10 = 0.8
        assertEquals(0.8, metrics.getObservedAgreement(), 1e-9);

        // kappa: p1 = p2 = 7/10 = 0.7
        // expectedKappa = 0.7*0.7 + 0.3*0.3 = 0.58
        // kappa = (0.8 - 0.58) / (1 - 0.58) = 0.22/0.42
        double expectedKappa = 0.22 / 0.42;
        assertEquals(expectedKappa, metrics.getKappa(), 1e-9);

        // ac1: avgPos = 0.7, expectedAc1 = 2*0.7*0.3 = 0.42
        // ac1 = (0.8 - 0.42) / (1 - 0.42) = 0.38/0.58
        double expectedAc1 = 0.38 / 0.58;
        assertEquals(expectedAc1, metrics.getAc1(), 1e-9);

        // Conflict CSV: header + 2 conflict rows
        Path conflictCsv = tempDir.resolve("second_review_conflicts.csv");
        assertTrue(Files.exists(conflictCsv));
        String csvContent = Files.readString(conflictCsv);
        assertTrue(csvContent.startsWith("fullyQualifiedClassName,primaryLabel,secondaryLabel,resolvedLabel"));
        assertTrue(csvContent.contains("com.example.Conflict1"));
        assertTrue(csvContent.contains("com.example.Conflict2"));
        assertFalse(csvContent.contains("com.example.ClassA1"), "Agreements must not appear in conflict CSV");

        // Reliability JSON contains all three metrics
        Path reliabilityJson = tempDir.resolve("reliability.json");
        assertTrue(Files.exists(reliabilityJson));
        String jsonContent = Files.readString(reliabilityJson);
        assertTrue(jsonContent.contains("\"agreement\""));
        assertTrue(jsonContent.contains("\"kappa\""));
        assertTrue(jsonContent.contains("\"ac1\""));
    }

    @Test
    void testEvaluateIgnoresNonOverlappingEntries() throws IOException {
        Map<String, LabelDTO> primary = new LinkedHashMap<>();
        Map<String, LabelDTO> secondary = new LinkedHashMap<>();

        // 1 overlapping (agreement)
        primary.put("com.example.A", label("com.example.A", FinalLabel.GOD_CLASS));
        secondary.put("com.example.A", label("com.example.A", FinalLabel.GOD_CLASS));

        // primary-only entry
        primary.put("com.example.PrimaryOnly", label("com.example.PrimaryOnly", FinalLabel.GOD_CLASS));
        // secondary-only entry
        secondary.put("com.example.SecondaryOnly", label("com.example.SecondaryOnly", FinalLabel.NO));

        ReliabilityMetrics metrics = new SecondReviewEvaluator().evaluate(primary, secondary, tempDir);

        assertEquals(1.0, metrics.getObservedAgreement(), 1e-9);

        Path conflictCsv = tempDir.resolve("second_review_conflicts.csv");
        String csvContent = Files.readString(conflictCsv);
        // Only header, no conflict rows
        assertFalse(csvContent.contains("com.example.PrimaryOnly"));
        assertFalse(csvContent.contains("com.example.SecondaryOnly"));
    }

    // ── SecondReviewExporter tests ────────────────────────────────────────────

    @Test
    void testExportProducesBlindCsvWithTwentyPercent() throws IOException {
        List<LabelDTO> primaryLabels = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            primaryLabels.add(label("com.example.Class" + i, FinalLabel.GOD_CLASS));
        }

        SecondReviewExporter exporter = new SecondReviewExporter();
        Path outputFile = tempDir.resolve("second_review.csv");
        exporter.export(primaryLabels, outputFile);

        assertTrue(Files.exists(outputFile));
        String content = Files.readString(outputFile);

        // Header matches LabelCsvImporter requirements
        assertTrue(content.startsWith(SecondReviewExporter.CSV_HEADER));

        // 20% of 10 = 2 data rows
        long dataRows = content.lines()
                .filter(l -> !l.isBlank() && !l.startsWith("fullyQualifiedClassName"))
                .count();
        assertEquals(2, dataRows);

        // Label columns are empty — no label values in data rows
        assertFalse(content.contains("GOD_CLASS"));
        assertFalse(content.contains("NO"));
    }

    @Test
    void testExportSortsAlphabetically() throws IOException {
        List<LabelDTO> labels = List.of(
                label("com.example.Zebra", FinalLabel.NO),
                label("com.example.Apple", FinalLabel.GOD_CLASS),
                label("com.example.Mango", FinalLabel.NO),
                label("com.example.Berry", FinalLabel.GOD_CLASS),
                label("com.example.Cherry", FinalLabel.NO)
        );

        Path outputFile = tempDir.resolve("second_review_sorted.csv");
        new SecondReviewExporter().export(labels, outputFile);

        String content = Files.readString(outputFile);
        // 20% of 5 = 1 data row → first alphabetically (Apple)
        assertTrue(content.contains("com.example.Apple"));
        assertFalse(content.contains("com.example.Zebra"), "Only first alphabetical item expected in 20% sample");
    }
}
