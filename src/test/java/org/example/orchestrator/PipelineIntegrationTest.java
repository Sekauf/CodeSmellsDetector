package org.example.orchestrator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.example.baseline.BaselineAnalyzer;
import org.example.baseline.BaselineThresholds;
import org.example.baseline.CandidateCsvUtil;
import org.example.baseline.CandidateDTO;
import org.example.export.ResultExporter;
import org.example.jdeodorant.ProjectConfig;
import org.example.sonar.SonarConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PipelineIntegrationTest {

    private static final String GOD_CLASS_FQCN = "com.example.goldenexample.GodClassExample";
    private static final String REGULAR_CLASS_FQCN = "com.example.goldenexample.RegularClassExample";
    private static final String HELPER_CLASS_FQCN = "com.example.goldenexample.HelperClass";

    @TempDir
    Path tempDir;

    @Test
    void pipeline_goldenToReport_endToEnd_withoutDocker_underTenSeconds() {
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            Path projectRoot = Path.of("src", "test", "resources", "golden", "GoldenExample");
            Path outputDir = tempDir.resolve("pipeline-e2e");
            BaselineThresholds thresholds = new BaselineThresholds(40, 999);

            AnalysisOrchestrator orchestrator = new AnalysisOrchestrator(
                    (root, t) -> new BaselineAnalyzer().analyze(root, t),
                    (root, config) -> mockSonarCandidates(),
                    config -> mockJdeodorantCandidates(),
                    new ResultExporter()
            );

            SonarConfig sonarConfig = SonarConfig.builder()
                    .hostUrl("http://mock-sonar.local")
                    .projectKey("golden-e2e")
                    .build();
            ProjectConfig jdeodorantConfig = ProjectConfig.forJdeodorantCsv("mock-jdeo.csv");

            orchestrator.run(projectRoot, thresholds, sonarConfig, jdeodorantConfig, outputDir);
            assertRunOutputs(outputDir);

            Path labelsFile = createAnnotatedLabelsFromLabelingInput(
                    outputDir.resolve("labeling_input.csv"),
                    outputDir.resolve("labels.csv")
            );
            orchestrator.evaluate(labelsFile, null, outputDir);

            assertEvaluateOutputs(outputDir);
            assertEvaluationJsonPlausible(outputDir.resolve("evaluation_per_tool.json"));
            assertErrorAnalysisPlausible(outputDir);
            assertReportPlausible(outputDir.resolve("report.md"));
        });
    }

    private static List<CandidateDTO> mockSonarCandidates() {
        return List.of(
                mockCandidate(GOD_CLASS_FQCN, false, true, false, 30, 15, 4),
                mockCandidate(REGULAR_CLASS_FQCN, false, true, false, 5, 2, 1)
        );
    }

    private static List<CandidateDTO> mockJdeodorantCandidates() {
        return List.of(mockCandidate(HELPER_CLASS_FQCN, false, false, true, 3, 1, 1));
    }

    private static CandidateDTO mockCandidate(
            String fqcn,
            boolean baselineFlag,
            boolean sonarFlag,
            boolean jdeodorantFlag,
            int methodCount,
            int fieldCount,
            int dependencyTypeCount
    ) {
        return new CandidateDTO(
                fqcn,
                baselineFlag,
                sonarFlag,
                jdeodorantFlag,
                null,
                null,
                null,
                null,
                null,
                methodCount,
                fieldCount,
                dependencyTypeCount,
                List.of()
        );
    }

    private void assertRunOutputs(Path outputDir) throws IOException {
        assertTrue(Files.exists(outputDir.resolve("results.csv")), "results.csv must exist");
        assertTrue(Files.exists(outputDir.resolve("results.json")), "results.json must exist");
        assertTrue(Files.exists(outputDir.resolve("labeling_input.csv")), "labeling_input.csv must exist");

        ObjectMapper mapper = new ObjectMapper();
        JsonNode runRoot = mapper.readTree(Files.readString(outputDir.resolve("results.json"), StandardCharsets.UTF_8));
        assertTrue(runRoot.has("candidates"), "results.json must contain candidates");
        assertTrue(runRoot.get("candidates").isArray(), "results.json.candidates must be an array");
        assertEquals(3, runRoot.get("candidates").size(), "Merged pipeline should contain 3 candidates");
    }

    private Path createAnnotatedLabelsFromLabelingInput(Path labelingInput, Path labelsOut) throws IOException {
        List<String> lines = Files.readAllLines(labelingInput, StandardCharsets.UTF_8);
        List<String> header = CandidateCsvUtil.parseCsvLine(lines.get(0));
        Map<String, Integer> idx = buildIndex(header);

        List<String> outLines = new ArrayList<>();
        outLines.add(lines.get(0));
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.trim().isEmpty()) {
                continue;
            }
            List<String> fields = ensureSize(CandidateCsvUtil.parseCsvLine(line), header.size());
            String fqcn = fields.get(idx.get("fullyQualifiedClassName"));
            boolean isGodClass = GOD_CLASS_FQCN.equals(fqcn);
            setLabelColumns(fields, idx, isGodClass);
            outLines.add(toCsvLine(fields));
        }

        Files.write(labelsOut, outLines, StandardCharsets.UTF_8);
        return labelsOut;
    }

    private static Map<String, Integer> buildIndex(List<String> header) {
        Map<String, Integer> idx = new HashMap<>();
        for (int i = 0; i < header.size(); i++) {
            idx.put(header.get(i), i);
        }
        return idx;
    }

    private static List<String> ensureSize(List<String> fields, int expectedSize) {
        List<String> copy = new ArrayList<>(fields);
        while (copy.size() < expectedSize) {
            copy.add("");
        }
        return copy;
    }

    private static void setLabelColumns(List<String> fields, Map<String, Integer> idx, boolean isGodClass) {
        String positive = Boolean.TRUE.toString();
        String negative = Boolean.FALSE.toString();
        fields.set(idx.get("k1"), isGodClass ? positive : negative);
        fields.set(idx.get("k2"), isGodClass ? positive : negative);
        fields.set(idx.get("k3"), isGodClass ? positive : negative);
        fields.set(idx.get("k4"), negative);
        fields.set(idx.get("comment"), "e2e");
        fields.set(idx.get("finalLabel"), isGodClass ? "GOD_CLASS" : "NO");
    }

    private static String toCsvLine(List<String> fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(CandidateCsvUtil.escapeCsvField(fields.get(i)));
        }
        return sb.toString();
    }

    private void assertEvaluateOutputs(Path outputDir) {
        assertTrue(Files.exists(outputDir.resolve("evaluation_per_tool.json")),
                "evaluation_per_tool.json must exist");
        assertTrue(Files.exists(outputDir.resolve("tool_agreement.csv")),
                "tool_agreement.csv must exist");
        assertTrue(Files.exists(outputDir.resolve("fp_fn_baseline.csv")),
                "fp_fn_baseline.csv must exist");
        assertTrue(Files.exists(outputDir.resolve("fp_fn_sonar.csv")),
                "fp_fn_sonar.csv must exist");
        assertTrue(Files.exists(outputDir.resolve("fp_fn_jdeodorant.csv")),
                "fp_fn_jdeodorant.csv must exist");
        assertTrue(Files.exists(outputDir.resolve("metrics_summary.csv")),
                "metrics_summary.csv must exist");
        assertTrue(Files.exists(outputDir.resolve("report.md")),
                "report.md must exist");
    }

    private void assertEvaluationJsonPlausible(Path evaluationJson) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(Files.readString(evaluationJson, StandardCharsets.UTF_8));
        assertToolMetrics(root.get("baseline"), 1, 0, 0, 2);
        assertToolMetrics(root.get("sonar"), 1, 1, 0, 1);
        assertToolMetrics(root.get("jdeodorant"), 0, 1, 1, 1);
    }

    private void assertToolMetrics(JsonNode node, int tp, int fp, int fn, int tn) {
        assertEquals(tp, node.get("truePositives").asInt());
        assertEquals(fp, node.get("falsePositives").asInt());
        assertEquals(fn, node.get("falseNegatives").asInt());
        assertEquals(tn, node.get("trueNegatives").asInt());
        assertEquals(3, tp + fp + fn + tn, "Confusion matrix values must sum to total rows");
    }

    private void assertErrorAnalysisPlausible(Path outputDir) throws IOException {
        String baseline = Files.readString(outputDir.resolve("fp_fn_baseline.csv"), StandardCharsets.UTF_8);
        String sonar = Files.readString(outputDir.resolve("fp_fn_sonar.csv"), StandardCharsets.UTF_8);
        String jdeodorant = Files.readString(outputDir.resolve("fp_fn_jdeodorant.csv"), StandardCharsets.UTF_8);

        assertEquals(1, baseline.lines().count(), "Baseline should have no FP/FN rows");
        assertTrue(sonar.contains(REGULAR_CLASS_FQCN) && sonar.contains("FP"),
                "Sonar should contain a false-positive row for RegularClassExample");
        assertTrue(jdeodorant.contains(HELPER_CLASS_FQCN) && jdeodorant.contains("FP"),
                "JDeodorant should contain a false-positive row for HelperClass");
        assertTrue(jdeodorant.contains(GOD_CLASS_FQCN) && jdeodorant.contains("FN"),
                "JDeodorant should contain a false-negative row for GodClassExample");
    }

    private void assertReportPlausible(Path reportFile) throws IOException {
        String report = Files.readString(reportFile, StandardCharsets.UTF_8);
        assertTrue(report.contains("# God-Class Detection Report"));
        assertTrue(report.contains("## Metrics per Tool"));
        assertTrue(report.contains("## Confusion Matrix per Tool"));
    }
}
