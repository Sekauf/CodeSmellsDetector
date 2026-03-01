package org.example.evaluation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.example.baseline.CandidateDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class ErrorAnalysisExporterTest {

    @TempDir
    Path tempDir;

    // ground truth: A=true(God), B=false, C=true(God), D=false
    private static final Map<String, Boolean> GROUND_TRUTH = Map.of(
            "com.example.A", true,
            "com.example.B", false,
            "com.example.C", true,
            "com.example.D", false
    );

    // predictions: A (TP), B (FP), D (FP) — C is missing → FN
    private static final Set<String> PREDICTIONS = Set.of(
            "com.example.A", "com.example.B", "com.example.D"
    );

    private static Map<String, CandidateDTO> buildCandidates() {
        return Map.of(
                "com.example.A", new CandidateDTO("com.example.A", true,  false, false, null, null, null, null, null, 10, 5, 3, List.of()),
                "com.example.B", new CandidateDTO("com.example.B", false, true,  false, null, null, null, null, null, 20, 8, 4, List.of()),
                "com.example.C", new CandidateDTO("com.example.C", true,  true,  false, null, null, null, null, null, 15, 6, 2, List.of()),
                "com.example.D", new CandidateDTO("com.example.D", false, false, true,  null, null, null, null, null,  5, 2, 1, List.of())
        );
    }

    // ── exportErrorList() ─────────────────────────────────────────────────────

    @Test
    void testExportErrorList_fileCreated() throws IOException {
        Path file = new ErrorAnalysisExporter()
                .exportErrorList("baseline", GROUND_TRUTH, PREDICTIONS, buildCandidates(), tempDir);

        assertTrue(Files.exists(file));
        assertEquals("fp_fn_baseline.csv", file.getFileName().toString());
    }

    @Test
    void testExportErrorList_headerPresent() throws IOException {
        Path file = new ErrorAnalysisExporter()
                .exportErrorList("sonar", GROUND_TRUTH, PREDICTIONS, buildCandidates(), tempDir);

        String content = Files.readString(file);
        assertTrue(content.startsWith("fqcn,errorType,methodCount,fieldCount,dependencyTypeCount,flags"));
    }

    @Test
    void testExportErrorList_correctRowCount() throws IOException {
        Path file = new ErrorAnalysisExporter()
                .exportErrorList("sonar", GROUND_TRUTH, PREDICTIONS, buildCandidates(), tempDir);

        // FP: B, D (predicted but not true God Class) = 2
        // FN: C (true God Class but not predicted)    = 1
        List<String> dataLines = Files.readAllLines(file).stream()
                .filter(l -> !l.isBlank() && !l.startsWith("fqcn"))
                .toList();
        assertEquals(3, dataLines.size());
    }

    @Test
    void testExportErrorList_sortedByErrorTypeThenFqcn() throws IOException {
        Path file = new ErrorAnalysisExporter()
                .exportErrorList("jdeodorant", GROUND_TRUTH, PREDICTIONS, buildCandidates(), tempDir);

        List<String> dataLines = Files.readAllLines(file).stream()
                .filter(l -> !l.isBlank() && !l.startsWith("fqcn"))
                .toList();

        // "FN" < "FP" alphabetically → FN rows come first
        assertTrue(dataLines.get(0).contains("FN"),        "First row should be FN");
        assertTrue(dataLines.get(1).contains("FP"),        "Second row should be FP");
        assertTrue(dataLines.get(2).contains("FP"),        "Third row should be FP");
        // FP rows sorted by FQCN: com.example.B before com.example.D
        assertTrue(dataLines.get(1).contains("com.example.B"), "Second row should be B");
        assertTrue(dataLines.get(2).contains("com.example.D"), "Third row should be D");
    }

    @Test
    void testExportErrorList_metricsColumnsPopulated() throws IOException {
        Path file = new ErrorAnalysisExporter()
                .exportErrorList("baseline", GROUND_TRUTH, PREDICTIONS, buildCandidates(), tempDir);

        String content = Files.readString(file);
        // com.example.B is FP with methodCount=20, fieldCount=8, dependencyTypeCount=4
        assertTrue(content.contains("com.example.B,FP,20,8,4"));
    }

    @Test
    void testExportErrorList_flagsPopulated() throws IOException {
        Path file = new ErrorAnalysisExporter()
                .exportErrorList("baseline", GROUND_TRUTH, PREDICTIONS, buildCandidates(), tempDir);

        String content = Files.readString(file);
        // com.example.B: sonarFlag=true, others false
        assertTrue(content.contains("baseline=false;sonar=true;jdeodorant=false"));
    }

    @Test
    void testExportErrorList_emptyPredictions_onlyFN() throws IOException {
        Path file = new ErrorAnalysisExporter()
                .exportErrorList("baseline", GROUND_TRUTH, Set.of(), buildCandidates(), tempDir);

        List<String> dataLines = Files.readAllLines(file).stream()
                .filter(l -> !l.isBlank() && !l.startsWith("fqcn"))
                .toList();

        // No FP; 2 FN (A and C are true God Classes)
        assertEquals(2, dataLines.size());
        assertTrue(dataLines.stream().allMatch(l -> l.contains("FN")));
    }

    @Test
    void testExportErrorList_nullCandidates_defaultsToZero() throws IOException {
        Path file = new ErrorAnalysisExporter()
                .exportErrorList("baseline", GROUND_TRUTH, PREDICTIONS, null, tempDir);

        List<String> dataLines = Files.readAllLines(file).stream()
                .filter(l -> !l.isBlank() && !l.startsWith("fqcn"))
                .toList();

        // Should still produce FP+FN rows with 0 metrics
        assertEquals(3, dataLines.size());
        assertTrue(dataLines.stream().anyMatch(l -> l.contains(",FP,0,0,0,")));
    }

    // ── exportAll() ───────────────────────────────────────────────────────────

    @Test
    void testExportAll_threeFilesCreated() throws IOException {
        Map<String, Path> paths = new ErrorAnalysisExporter().exportAll(
                GROUND_TRUTH, PREDICTIONS, PREDICTIONS, PREDICTIONS,
                buildCandidates(), tempDir);

        assertEquals(3, paths.size());
        assertTrue(paths.containsKey("baseline"));
        assertTrue(paths.containsKey("sonar"));
        assertTrue(paths.containsKey("jdeodorant"));
        for (Path p : paths.values()) {
            assertTrue(Files.exists(p));
        }
    }

    @Test
    void testExportAll_fileNamesCorrect() throws IOException {
        Map<String, Path> paths = new ErrorAnalysisExporter().exportAll(
                GROUND_TRUTH, PREDICTIONS, PREDICTIONS, PREDICTIONS,
                buildCandidates(), tempDir);

        assertEquals("fp_fn_baseline.csv",   paths.get("baseline").getFileName().toString());
        assertEquals("fp_fn_sonar.csv",      paths.get("sonar").getFileName().toString());
        assertEquals("fp_fn_jdeodorant.csv", paths.get("jdeodorant").getFileName().toString());
    }
}
