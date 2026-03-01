package org.example.labeling;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.example.baseline.CandidateCsvUtil;
import org.example.labeling.LabelDTO.FinalLabel;
import org.example.metrics.ReliabilityEvaluator;
import org.example.metrics.ReliabilityMetrics;

/**
 * Evaluates inter-rater reliability between primary and secondary review labels.
 * Produces {@code second_review_conflicts.csv} and {@code reliability.json} in the output directory.
 */
public class SecondReviewEvaluator {

    private static final Logger LOGGER = Logger.getLogger(SecondReviewEvaluator.class.getName());

    private static final String CONFLICT_CSV_FILE = "second_review_conflicts.csv";
    private static final String RELIABILITY_JSON_FILE = "reliability.json";
    private static final String CONFLICT_HEADER =
            "fullyQualifiedClassName,primaryLabel,secondaryLabel,resolvedLabel";

    private final ObjectMapper objectMapper;

    /** Creates an evaluator with pretty-printed JSON output. */
    public SecondReviewEvaluator() {
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * Evaluates reliability between primary and secondary labels.
     * Only classes present in both maps are compared; non-overlapping entries are ignored.
     *
     * @param primary   map of FQCN → primary LabelDTO (must not be null)
     * @param secondary map of FQCN → secondary LabelDTO (must not be null)
     * @param outputDir directory for output files (created if absent)
     * @return the computed reliability metrics
     * @throws IOException on write failure
     */
    public ReliabilityMetrics evaluate(
            Map<String, LabelDTO> primary,
            Map<String, LabelDTO> secondary,
            Path outputDir
    ) throws IOException {
        LOGGER.info("SecondReviewEvaluator started: primary=" + primary.size()
                + " secondary=" + secondary.size() + " outputDir=" + outputDir);
        Files.createDirectories(outputDir);

        List<String> overlap = sortedOverlap(primary, secondary);
        LOGGER.info("Overlap classes: " + overlap.size());

        List<Boolean> primaryVotes = new ArrayList<>();
        List<Boolean> secondaryVotes = new ArrayList<>();
        List<ConflictRow> conflicts = new ArrayList<>();

        for (String fqcn : overlap) {
            FinalLabel primaryLabel = primary.get(fqcn).getFinalLabel();
            FinalLabel secondaryLabel = secondary.get(fqcn).getFinalLabel();
            primaryVotes.add(isGodClass(primaryLabel));
            secondaryVotes.add(isGodClass(secondaryLabel));
            if (primaryLabel != secondaryLabel) {
                conflicts.add(new ConflictRow(fqcn, primaryLabel, secondaryLabel));
            }
        }

        ReliabilityMetrics metrics = ReliabilityEvaluator.computeReliability(primaryVotes, secondaryVotes);
        writeConflictCsv(conflicts, outputDir.resolve(CONFLICT_CSV_FILE));
        writeReliabilityJson(metrics, outputDir.resolve(RELIABILITY_JSON_FILE));

        LOGGER.info("SecondReviewEvaluator finished: conflicts=" + conflicts.size()
                + " agreement=" + metrics.getObservedAgreement());
        return metrics;
    }

    private List<String> sortedOverlap(Map<String, LabelDTO> primary, Map<String, LabelDTO> secondary) {
        List<String> overlap = new ArrayList<>();
        for (String fqcn : primary.keySet()) {
            if (secondary.containsKey(fqcn)) {
                overlap.add(fqcn);
            }
        }
        overlap.sort(Comparator.naturalOrder());
        return overlap;
    }

    private boolean isGodClass(FinalLabel label) {
        return FinalLabel.GOD_CLASS.equals(label);
    }

    private void writeConflictCsv(List<ConflictRow> conflicts, Path file) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(CONFLICT_HEADER).append("\n");
        for (ConflictRow row : conflicts) {
            sb.append(CandidateCsvUtil.escapeCsvField(row.fqcn)).append(",");
            sb.append(labelName(row.primaryLabel)).append(",");
            sb.append(labelName(row.secondaryLabel)).append(",");
            sb.append("\n"); // resolvedLabel is intentionally left empty
        }
        Files.writeString(file, sb.toString(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        LOGGER.info("Conflict CSV written: " + file + " rows=" + conflicts.size());
    }

    private void writeReliabilityJson(ReliabilityMetrics metrics, Path file) throws IOException {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("agreement", metrics.getObservedAgreement());
        json.put("kappa", metrics.getKappa());
        json.put("ac1", metrics.getAc1());
        objectMapper.writeValue(file.toFile(), json);
        LOGGER.info("Reliability JSON written: " + file);
    }

    private String labelName(FinalLabel label) {
        return label != null ? label.name() : "";
    }

    private static class ConflictRow {
        final String fqcn;
        final FinalLabel primaryLabel;
        final FinalLabel secondaryLabel;

        ConflictRow(String fqcn, FinalLabel primaryLabel, FinalLabel secondaryLabel) {
            this.fqcn = fqcn;
            this.primaryLabel = primaryLabel;
            this.secondaryLabel = secondaryLabel;
        }
    }
}
