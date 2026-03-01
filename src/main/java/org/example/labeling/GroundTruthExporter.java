package org.example.labeling;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.example.baseline.CandidateCsvUtil;
import org.example.labeling.LabelDTO.FinalLabel;

/**
 * Exports a map of {@link LabelDTO} instances as ground-truth artifacts.
 * Produces {@code ground_truth.csv} and {@code ground_truth.json} in the given output directory.
 */
public class GroundTruthExporter {

    private static final Logger LOGGER = Logger.getLogger(GroundTruthExporter.class.getName());

    private static final String CSV_HEADER =
            "fullyQualifiedClassName,k1,k2,k3,k4,comment,finalLabel";
    private static final String CSV_FILE_NAME = "ground_truth.csv";
    private static final String JSON_FILE_NAME = "ground_truth.json";

    private final ObjectMapper objectMapper;

    /** Creates an exporter with pretty-printed JSON output. */
    public GroundTruthExporter() {
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * Exports the given labels to {@code ground_truth.csv} and {@code ground_truth.json}
     * inside {@code outputDir}.
     *
     * @param labels    map from FQCN to LabelDTO (must not be null)
     * @param outputDir directory where the output files are written (created if absent)
     * @throws IOException on write failure
     */
    public void export(Map<String, LabelDTO> labels, Path outputDir) throws IOException {
        LOGGER.info("GroundTruthExporter started: outputDir=" + outputDir
                + " entries=" + labels.size());
        Files.createDirectories(outputDir);

        List<LabelDTO> sorted = sortedLabels(labels);
        exportCsv(sorted, outputDir.resolve(CSV_FILE_NAME));
        exportJson(sorted, outputDir.resolve(JSON_FILE_NAME));

        LOGGER.info("GroundTruthExporter finished: entries=" + sorted.size());
    }

    /** Returns labels sorted alphabetically by FQCN. */
    private List<LabelDTO> sortedLabels(Map<String, LabelDTO> labels) {
        List<LabelDTO> list = new ArrayList<>(labels.values());
        list.sort(Comparator.comparing(LabelDTO::getFullyQualifiedClassName));
        return list;
    }

    /** Writes {@code ground_truth.csv} with header and one row per label. */
    private void exportCsv(List<LabelDTO> sorted, Path file) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(CSV_HEADER).append("\n");
        for (LabelDTO dto : sorted) {
            appendLabelRow(sb, dto);
        }
        Files.writeString(file, sb.toString(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        LOGGER.info("CSV written: " + file + " rows=" + sorted.size());
    }

    /** Appends a single CSV row for the given label. */
    private void appendLabelRow(StringBuilder sb, LabelDTO dto) {
        sb.append(CandidateCsvUtil.escapeCsvField(dto.getFullyQualifiedClassName())).append(",");
        sb.append(booleanToString(dto.getK1())).append(",");
        sb.append(booleanToString(dto.getK2())).append(",");
        sb.append(booleanToString(dto.getK3())).append(",");
        sb.append(booleanToString(dto.getK4())).append(",");
        sb.append(CandidateCsvUtil.escapeCsvField(dto.getComment())).append(",");
        sb.append(dto.getFinalLabel() != null ? dto.getFinalLabel().name() : "");
        sb.append("\n");
    }

    /** Converts a nullable Boolean to its string representation; null → empty string. */
    private String booleanToString(Boolean value) {
        if (value == null) {
            return "";
        }
        return value.toString();
    }

    /** Writes {@code ground_truth.json} with metadata and sorted label entries. */
    private void exportJson(List<LabelDTO> sorted, Path file) throws IOException {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("metadata", buildMetadata(sorted));
        root.put("labels", buildLabelsList(sorted));
        objectMapper.writeValue(file.toFile(), root);
        LOGGER.info("JSON written: " + file);
    }

    /** Builds the metadata map (counts + date). */
    private Map<String, Object> buildMetadata(List<LabelDTO> sorted) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("total", sorted.size());
        meta.put("godClassCount", countByLabel(sorted, FinalLabel.GOD_CLASS));
        meta.put("noCount", countByLabel(sorted, FinalLabel.NO));
        meta.put("uncertainCount", countByLabel(sorted, FinalLabel.UNCERTAIN));
        meta.put("date", LocalDate.now().toString());
        return meta;
    }

    /** Counts label entries with the given {@link FinalLabel}. */
    private long countByLabel(List<LabelDTO> sorted, FinalLabel label) {
        return sorted.stream()
                .filter(dto -> label.equals(dto.getFinalLabel()))
                .count();
    }

    /** Converts each LabelDTO to a map for JSON serialization. */
    private List<Map<String, Object>> buildLabelsList(List<LabelDTO> sorted) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (LabelDTO dto : sorted) {
            list.add(labelToMap(dto));
        }
        return list;
    }

    /** Converts a single LabelDTO to an ordered map for JSON output. */
    private Map<String, Object> labelToMap(LabelDTO dto) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("fullyQualifiedClassName", dto.getFullyQualifiedClassName());
        map.put("k1", dto.getK1());
        map.put("k2", dto.getK2());
        map.put("k3", dto.getK3());
        map.put("k4", dto.getK4());
        map.put("comment", dto.getComment());
        map.put("finalLabel", dto.getFinalLabel() != null ? dto.getFinalLabel().name() : null);
        return map;
    }
}
