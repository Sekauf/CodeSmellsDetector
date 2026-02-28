package org.example.labeling;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.example.baseline.CandidateDTO;
import org.example.labeling.LabelDTO.FinalLabel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link LabelDTO#deriveLabel} and {@link LabelCsvExporter#export}.
 */
class LabelingTest {

    // -----------------------------------------------------------------------
    // deriveLabel — all truth-count combinations
    // -----------------------------------------------------------------------

    @Test
    void deriveLabelFourTrueIsGodClass() {
        assertEquals(FinalLabel.GOD_CLASS, LabelDTO.deriveLabel(true, true, true, true));
    }

    @Test
    void deriveLabelThreeTrueIsGodClass() {
        assertEquals(FinalLabel.GOD_CLASS, LabelDTO.deriveLabel(true, true, true, false));
        assertEquals(FinalLabel.GOD_CLASS, LabelDTO.deriveLabel(true, true, false, true));
        assertEquals(FinalLabel.GOD_CLASS, LabelDTO.deriveLabel(true, false, true, true));
        assertEquals(FinalLabel.GOD_CLASS, LabelDTO.deriveLabel(false, true, true, true));
    }

    @Test
    void deriveLabelTwoTrueIsUncertain() {
        assertEquals(FinalLabel.UNCERTAIN, LabelDTO.deriveLabel(true, true, false, false));
        assertEquals(FinalLabel.UNCERTAIN, LabelDTO.deriveLabel(true, false, true, false));
        assertEquals(FinalLabel.UNCERTAIN, LabelDTO.deriveLabel(true, false, false, true));
        assertEquals(FinalLabel.UNCERTAIN, LabelDTO.deriveLabel(false, true, true, false));
        assertEquals(FinalLabel.UNCERTAIN, LabelDTO.deriveLabel(false, true, false, true));
        assertEquals(FinalLabel.UNCERTAIN, LabelDTO.deriveLabel(false, false, true, true));
    }

    @Test
    void deriveLabelOneTrueIsNo() {
        assertEquals(FinalLabel.NO, LabelDTO.deriveLabel(true, false, false, false));
        assertEquals(FinalLabel.NO, LabelDTO.deriveLabel(false, true, false, false));
        assertEquals(FinalLabel.NO, LabelDTO.deriveLabel(false, false, true, false));
        assertEquals(FinalLabel.NO, LabelDTO.deriveLabel(false, false, false, true));
    }

    @Test
    void deriveLabelZeroTrueIsNo() {
        assertEquals(FinalLabel.NO, LabelDTO.deriveLabel(false, false, false, false));
    }

    @Test
    void deriveLabelNullCountsAsFalse() {
        assertEquals(FinalLabel.NO, LabelDTO.deriveLabel(null, null, null, null));
        assertEquals(FinalLabel.UNCERTAIN, LabelDTO.deriveLabel(null, null, true, true));
        assertEquals(FinalLabel.GOD_CLASS, LabelDTO.deriveLabel(true, true, true, null));
    }

    // -----------------------------------------------------------------------
    // LabelCsvExporter.export — CSV structure with 3 mock candidates
    // -----------------------------------------------------------------------

    @Test
    void exportWritesCorrectHeader(@TempDir Path tempDir) throws IOException {
        List<CandidateDTO> candidates = buildMockCandidates();
        Path outputFile = tempDir.resolve("labeling.csv");

        new LabelCsvExporter().export(candidates, outputFile);

        List<String> lines = Files.readAllLines(outputFile, StandardCharsets.UTF_8);
        String expectedHeader = "fullyQualifiedClassName,baselineFlag,sonarFlag,jdeodorantFlag,"
                + "methodCount,fieldCount,dependencyTypeCount,k1,k2,k3,k4,comment,finalLabel";
        assertEquals(expectedHeader, lines.get(0));
    }

    @Test
    void exportWritesThreeDataRows(@TempDir Path tempDir) throws IOException {
        List<CandidateDTO> candidates = buildMockCandidates();
        Path outputFile = tempDir.resolve("labeling.csv");

        new LabelCsvExporter().export(candidates, outputFile);

        List<String> lines = Files.readAllLines(outputFile, StandardCharsets.UTF_8);
        // header + 3 data rows (trailing newline produces no extra empty line with readAllLines)
        assertEquals(4, lines.size());
    }

    @Test
    void exportSortsRowsAlphabetically(@TempDir Path tempDir) throws IOException {
        List<CandidateDTO> candidates = buildMockCandidates();
        Path outputFile = tempDir.resolve("labeling.csv");

        new LabelCsvExporter().export(candidates, outputFile);

        List<String> lines = Files.readAllLines(outputFile, StandardCharsets.UTF_8);
        assertTrue(lines.get(1).startsWith("com.example.Alpha"));
        assertTrue(lines.get(2).startsWith("com.example.Beta"));
        assertTrue(lines.get(3).startsWith("com.example.Zeta"));
    }

    @Test
    void exportLeavesLabelColumnsEmpty(@TempDir Path tempDir) throws IOException {
        List<CandidateDTO> candidates = buildMockCandidates();
        Path outputFile = tempDir.resolve("labeling.csv");

        new LabelCsvExporter().export(candidates, outputFile);

        List<String> lines = Files.readAllLines(outputFile, StandardCharsets.UTF_8);
        // Each data row must end with 6 empty label columns: ...,,,,,  (6 commas after dependencyTypeCount)
        for (int i = 1; i <= 3; i++) {
            String row = lines.get(i);
            assertTrue(row.endsWith(",,,,,"), "Label columns must be empty in row: " + row);
        }
    }

    @Test
    void exportCreatesParentDirectoryIfAbsent(@TempDir Path tempDir) throws IOException {
        Path outputFile = tempDir.resolve("subdir/nested/labeling.csv");

        new LabelCsvExporter().export(buildMockCandidates(), outputFile);

        assertTrue(Files.exists(outputFile));
    }

    // -----------------------------------------------------------------------
    // helpers
    // -----------------------------------------------------------------------

    private List<CandidateDTO> buildMockCandidates() {
        CandidateDTO alpha = CandidateDTO.builder("com.example.Alpha")
                .baselineFlag(true).sonarFlag(false).jdeodorantFlag(true).build();
        CandidateDTO zeta = CandidateDTO.builder("com.example.Zeta")
                .baselineFlag(false).sonarFlag(true).jdeodorantFlag(false).build();
        CandidateDTO beta = CandidateDTO.builder("com.example.Beta")
                .baselineFlag(true).sonarFlag(true).jdeodorantFlag(true).build();
        // intentionally unsorted to verify sort
        return List.of(zeta, alpha, beta);
    }
}
