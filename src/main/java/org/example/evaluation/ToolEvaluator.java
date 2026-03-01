package org.example.evaluation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.example.metrics.EvaluationMetrics;
import org.example.metrics.MetricsEngine;

/**
 * Evaluates God-Class detection tool predictions against a ground truth.
 * Uses {@link MetricsEngine} to compute precision, recall, F1, MCC and specificity.
 */
public class ToolEvaluator {

    private static final Logger LOGGER = Logger.getLogger(ToolEvaluator.class.getName());

    private static final String OUTPUT_FILE = "evaluation_per_tool.json";
    private static final String KEY_BASELINE = "baseline";
    private static final String KEY_SONAR = "sonar";
    private static final String KEY_JDEODORANT = "jdeodorant";

    private final ObjectMapper objectMapper;

    /** Creates a ToolEvaluator with pretty-printed JSON output. */
    public ToolEvaluator() {
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * Evaluates a single tool's predictions against the ground truth.
     *
     * @param groundTruth map of FQCN → true (God Class) / false (not God Class)
     * @param predictions set of FQCNs predicted as God Classes by the tool
     * @param total       total number of candidate classes
     * @return computed evaluation metrics
     */
    public EvaluationMetrics evaluate(
            Map<String, Boolean> groundTruth,
            Set<String> predictions,
            int total
    ) {
        Set<String> actualPositives = extractPositives(groundTruth);
        LOGGER.info("ToolEvaluator.evaluate: actual=" + actualPositives.size()
                + " predicted=" + (predictions == null ? 0 : predictions.size())
                + " total=" + total);
        return MetricsEngine.computeMetrics(predictions, actualPositives, total);
    }

    /**
     * Evaluates all three tools against the ground truth in a single call.
     *
     * @param groundTruth           map of FQCN → true/false ground-truth labels
     * @param baselinePredictions   FQCNs flagged by the baseline tool
     * @param sonarPredictions      FQCNs flagged by SonarQube
     * @param jdeodorantPredictions FQCNs flagged by JDeodorant
     * @param total                 total number of candidate classes
     * @return ordered map: "baseline", "sonar", "jdeodorant" → EvaluationMetrics
     */
    public Map<String, EvaluationMetrics> evaluateAll(
            Map<String, Boolean> groundTruth,
            Set<String> baselinePredictions,
            Set<String> sonarPredictions,
            Set<String> jdeodorantPredictions,
            int total
    ) {
        LOGGER.info("ToolEvaluator.evaluateAll started: total=" + total);
        Map<String, EvaluationMetrics> results = new LinkedHashMap<>();
        results.put(KEY_BASELINE, evaluate(groundTruth, baselinePredictions, total));
        results.put(KEY_SONAR, evaluate(groundTruth, sonarPredictions, total));
        results.put(KEY_JDEODORANT, evaluate(groundTruth, jdeodorantPredictions, total));
        LOGGER.info("ToolEvaluator.evaluateAll finished: tools=" + results.size());
        return results;
    }

    /**
     * Writes the per-tool evaluation results to {@code evaluation_per_tool.json}
     * inside {@code outputDir}.
     *
     * @param results   map of tool name → EvaluationMetrics
     * @param outputDir target directory (created if absent)
     * @return path of the written file
     * @throws IOException on write failure
     */
    public Path exportJson(Map<String, EvaluationMetrics> results, Path outputDir) throws IOException {
        Files.createDirectories(outputDir);
        Path file = outputDir.resolve(OUTPUT_FILE);
        objectMapper.writeValue(file.toFile(), results);
        LOGGER.info("ToolEvaluator.exportJson written: " + file + " tools=" + results.size());
        return file;
    }

    private Set<String> extractPositives(Map<String, Boolean> groundTruth) {
        if (groundTruth == null || groundTruth.isEmpty()) {
            return Set.of();
        }
        Set<String> positives = new HashSet<>();
        for (Map.Entry<String, Boolean> entry : groundTruth.entrySet()) {
            if (Boolean.TRUE.equals(entry.getValue())) {
                positives.add(entry.getKey());
            }
        }
        return positives;
    }
}
