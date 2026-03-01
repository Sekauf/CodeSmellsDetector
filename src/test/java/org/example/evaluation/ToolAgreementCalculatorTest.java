package org.example.evaluation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class ToolAgreementCalculatorTest {

    @TempDir
    Path tempDir;

    // baseline={A,B,C}, sonar={A,B,D}, jdeo={A,E}
    private static final Set<String> BASELINE = Set.of("A", "B", "C");
    private static final Set<String> SONAR    = Set.of("A", "B", "D");
    private static final Set<String> JDEO     = Set.of("A", "E");

    // ── jaccard() ─────────────────────────────────────────────────────────────

    @Test
    void testJaccard_knownValues() {
        ToolAgreementCalculator calc = new ToolAgreementCalculator();

        // baseline ∩ sonar = {A,B}, union = {A,B,C,D} → 2/4 = 0.5
        assertEquals(0.5, calc.jaccard(BASELINE, SONAR), 1e-9);

        // baseline ∩ jdeo = {A}, union = {A,B,C,E} → 1/4 = 0.25
        assertEquals(0.25, calc.jaccard(BASELINE, JDEO), 1e-9);

        // sonar ∩ jdeo = {A}, union = {A,B,D,E} → 1/4 = 0.25
        assertEquals(0.25, calc.jaccard(SONAR, JDEO), 1e-9);
    }

    @Test
    void testJaccard_identicalSets_returnsOne() {
        assertEquals(1.0, new ToolAgreementCalculator().jaccard(Set.of("X", "Y"), Set.of("X", "Y")), 1e-9);
    }

    @Test
    void testJaccard_disjointSets_returnsZero() {
        assertEquals(0.0, new ToolAgreementCalculator().jaccard(Set.of("A"), Set.of("B")), 1e-9);
    }

    @Test
    void testJaccard_bothEmpty_returnsZero() {
        assertEquals(0.0, new ToolAgreementCalculator().jaccard(Set.of(), Set.of()), 1e-9);
    }

    @Test
    void testJaccard_nullTreatedAsEmpty() {
        assertEquals(0.0, new ToolAgreementCalculator().jaccard(null, null), 1e-9);
        assertEquals(0.0, new ToolAgreementCalculator().jaccard(Set.of("A"), null), 1e-9);
    }

    // ── computeAll() ──────────────────────────────────────────────────────────

    @Test
    void testComputeAll_returnThreePairs() {
        List<OverlapResult> results = new ToolAgreementCalculator()
                .computeAll(BASELINE, SONAR, JDEO);

        assertEquals(3, results.size());
    }

    @Test
    void testComputeAll_alphabeticalOrder() {
        List<OverlapResult> results = new ToolAgreementCalculator()
                .computeAll(BASELINE, SONAR, JDEO);

        // expected: baseline-jdeodorant, baseline-sonar, jdeodorant-sonar
        assertEquals("baseline",   results.get(0).getToolA());
        assertEquals("jdeodorant", results.get(0).getToolB());
        assertEquals("baseline",   results.get(1).getToolA());
        assertEquals("sonar",      results.get(1).getToolB());
        assertEquals("jdeodorant", results.get(2).getToolA());
        assertEquals("sonar",      results.get(2).getToolB());
    }

    @Test
    void testComputeAll_overlapCounts() {
        List<OverlapResult> results = new ToolAgreementCalculator()
                .computeAll(BASELINE, SONAR, JDEO);

        // baseline–jdeodorant: both={A}=1, onlyBaseline={B,C}=2, onlyJdeo={E}=1
        OverlapResult bj = results.get(0);
        assertEquals(1,    bj.getBoth());
        assertEquals(2,    bj.getOnlyA());
        assertEquals(1,    bj.getOnlyB());
        assertEquals(0.25, bj.getJaccard(), 1e-9);

        // baseline–sonar: both={A,B}=2, onlyBaseline={C}=1, onlySonar={D}=1
        OverlapResult bs = results.get(1);
        assertEquals(2,   bs.getBoth());
        assertEquals(1,   bs.getOnlyA());
        assertEquals(1,   bs.getOnlyB());
        assertEquals(0.5, bs.getJaccard(), 1e-9);

        // jdeodorant–sonar: both={A}=1, onlyJdeo={E}=1, onlySonar={B,D}=2
        OverlapResult js = results.get(2);
        assertEquals(1,    js.getBoth());
        assertEquals(1,    js.getOnlyA());
        assertEquals(2,    js.getOnlyB());
        assertEquals(0.25, js.getJaccard(), 1e-9);
    }

    // ── exportCsv() ───────────────────────────────────────────────────────────

    @Test
    void testExportCsv_fileContent() throws IOException {
        ToolAgreementCalculator calc = new ToolAgreementCalculator();
        List<OverlapResult> results = calc.computeAll(BASELINE, SONAR, JDEO);
        Path file = calc.exportCsv(results, tempDir);

        assertTrue(Files.exists(file));
        assertEquals("tool_agreement.csv", file.getFileName().toString());

        String content = Files.readString(file);
        assertTrue(content.startsWith("toolA,toolB,jaccard,both,onlyA,onlyB"));
        assertTrue(content.contains("baseline"));
        assertTrue(content.contains("sonar"));
        assertTrue(content.contains("jdeodorant"));

        // 1 header + 3 data rows
        long nonBlankLines = content.lines().filter(l -> !l.isBlank()).count();
        assertEquals(4, nonBlankLines);
    }
}
