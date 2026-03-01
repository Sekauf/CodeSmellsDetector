package org.example.orchestrator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.example.baseline.BaselineThresholds;
import org.example.baseline.CandidateDTO;
import org.example.export.ResultExporter;
import org.example.labeling.LabelCsvExporter;
import org.example.sonar.SonarConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalysisOrchestratorTest {

    @TempDir
    Path tempDir;

    /**
     * US-03: Verifies that labeling_input.csv is written with detected + blind-negative samples.
     *
     * <p>Setup:
     * - Baseline returns Base01–Base60 (methodCount 1–60) + SonarA/B/C
     * - Sonar returns SonarA, SonarB, SonarC
     * - JDeodorant skipped (config=null)
     *
     * <p>Expected:
     * - SonarA/B/C are detected (sonarFlag=true)
     * - Base01–Base60 are eligible for blind sampling
     * - Top 10% of 60 = ceil(6) = 6 candidates (Base55–Base60)
     * - min(5, 6) = 5 blind negatives sampled
     * - CSV: 1 header + 3 detected + 5 blind = 9 lines
     */
    @Test
    void run_writesLabelingInputCsvWithDetectedAndBlindNegatives() throws IOException, InterruptedException {
        List<CandidateDTO> sonarCandidates = List.of(
                makeSonar("SonarA"),
                makeSonar("SonarB"),
                makeSonar("SonarC")
        );

        List<CandidateDTO> baselineCandidates = new ArrayList<>(sonarCandidates);
        for (int i = 1; i <= 60; i++) {
            String name = String.format("Base%02d", i);
            baselineCandidates.add(makeBase(name, i));
        }

        AnalysisOrchestrator orchestrator = new AnalysisOrchestrator(
                (projectRoot, thresholds) -> baselineCandidates,
                (projectRoot, config) -> sonarCandidates,
                config -> List.of(),
                new ResultExporter()
        );

        SonarConfig sonarConfig = SonarConfig.builder()
                .hostUrl("http://localhost:9000")
                .projectKey("test-project")
                .build();
        BaselineThresholds thresholds = new BaselineThresholds(1, 1);

        orchestrator.run(tempDir, thresholds, sonarConfig, null, tempDir);

        Path labelingCsv = tempDir.resolve("labeling_input.csv");
        assertTrue(Files.exists(labelingCsv), "labeling_input.csv must exist");

        List<String> lines = Files.readAllLines(labelingCsv);
        assertEquals(String.join(",", LabelCsvExporter.HEADER), lines.get(0), "Header row must match");
        assertEquals(9, lines.size(), "Expected 1 header + 3 detected + 5 blind = 9 lines");

        String content = Files.readString(labelingCsv);
        assertTrue(content.contains("SonarA"), "CSV must contain SonarA");
        assertTrue(content.contains("SonarB"), "CSV must contain SonarB");
        assertTrue(content.contains("SonarC"), "CSV must contain SonarC");

        boolean hasHighBase = content.contains("Base55") || content.contains("Base56")
                || content.contains("Base57") || content.contains("Base58")
                || content.contains("Base59") || content.contains("Base60");
        assertTrue(hasHighBase, "CSV must contain at least one of Base55–Base60 as blind negative");
    }

    private static CandidateDTO makeSonar(String fqcn) {
        return new CandidateDTO(fqcn, false, true, false, null, null, null, null, null, 0, 0, 0, List.of());
    }

    private static CandidateDTO makeBase(String fqcn, int methodCount) {
        return new CandidateDTO(fqcn, false, false, false, null, null, null, null, null, methodCount, 0, 0, List.of());
    }
}
