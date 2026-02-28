package org.example.labeling;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.example.baseline.CandidateCsvUtil;
import org.example.labeling.LabelDTO.FinalLabel;

/**
 * Reads a manually annotated labeling CSV and reconstructs {@link LabelDTO} objects.
 * The CSV format is produced by {@link LabelCsvExporter}.
 */
public class LabelCsvImporter {

    private static final Logger LOGGER = Logger.getLogger(LabelCsvImporter.class.getName());

    private static final List<String> REQUIRED_COLUMNS = List.of(
            "fullyQualifiedClassName", "k1", "k2", "k3", "k4", "comment", "finalLabel");

    /**
     * Reads a manually annotated labeling CSV and returns one LabelDTO per class.
     *
     * @param file path to the CSV file
     * @return map from fullyQualifiedClassName → LabelDTO (insertion order preserved)
     * @throws IOException if the file cannot be read
     */
    public Map<String, LabelDTO> importLabels(Path file) throws IOException {
        LOGGER.info("LabelCsvImporter started: " + file);
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("CSV file is empty: " + file);
        }

        List<String> headerFields = CandidateCsvUtil.parseCsvLine(lines.get(0));
        Map<String, Integer> headerMap = buildHeaderMap(headerFields);
        validateRequiredColumns(headerMap, lines.get(0));

        Map<String, LabelDTO> result = new LinkedHashMap<>();
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.trim().isEmpty()) {
                continue;
            }
            List<String> fields = CandidateCsvUtil.parseCsvLine(line);
            int lineNumber = i + 1;
            LabelDTO dto = parseLabelRow(fields, headerMap, lineNumber);
            if (dto != null) {
                result.put(dto.getFullyQualifiedClassName(), dto);
            }
        }

        LOGGER.info("LabelCsvImporter finished: " + file + " entries=" + result.size());
        return result;
    }

    private Map<String, Integer> buildHeaderMap(List<String> headerFields) {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (int i = 0; i < headerFields.size(); i++) {
            map.put(headerFields.get(i), i);
        }
        return map;
    }

    private void validateRequiredColumns(Map<String, Integer> headerMap, String headerLine) {
        for (String required : REQUIRED_COLUMNS) {
            if (!headerMap.containsKey(required)) {
                throw new IllegalArgumentException(
                        "Missing required header '" + required + "' in: " + headerLine);
            }
        }
    }

    private LabelDTO parseLabelRow(List<String> fields, Map<String, Integer> headerMap, int lineNumber) {
        String fqcn = value(fields, headerMap, "fullyQualifiedClassName");
        Boolean k1 = parseLenientBoolean(value(fields, headerMap, "k1"), lineNumber, "k1");
        Boolean k2 = parseLenientBoolean(value(fields, headerMap, "k2"), lineNumber, "k2");
        Boolean k3 = parseLenientBoolean(value(fields, headerMap, "k3"), lineNumber, "k3");
        Boolean k4 = parseLenientBoolean(value(fields, headerMap, "k4"), lineNumber, "k4");
        String comment = value(fields, headerMap, "comment");
        String finalLabelRaw = value(fields, headerMap, "finalLabel");

        FinalLabel finalLabel;
        if (finalLabelRaw == null || finalLabelRaw.isEmpty()) {
            finalLabel = LabelDTO.deriveLabel(k1, k2, k3, k4);
        } else {
            try {
                finalLabel = FinalLabel.valueOf(finalLabelRaw.toUpperCase());
            } catch (IllegalArgumentException e) {
                LOGGER.warning("Unknown finalLabel '" + finalLabelRaw + "' at line " + lineNumber + ", skipping row.");
                return null;
            }
        }

        return new LabelDTO(fqcn, k1, k2, k3, k4, comment, finalLabel);
    }

    private String value(List<String> fields, Map<String, Integer> headerMap, String key) {
        Integer index = headerMap.get(key);
        if (index == null || index >= fields.size()) {
            return "";
        }
        return fields.get(index);
    }

    /**
     * Parses a boolean value leniently, accepting multiple representations.
     *
     * @param raw       raw string from CSV
     * @param lineNumber current line number for logging
     * @param column    column name for logging
     * @return TRUE, FALSE, or null if blank/unrecognized
     */
    private Boolean parseLenientBoolean(String raw, int lineNumber, String column) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        String normalized = raw.trim().toLowerCase();
        switch (normalized) {
            case "true": case "yes": case "ja": case "1":
                return Boolean.TRUE;
            case "false": case "no": case "nein": case "0":
                return Boolean.FALSE;
            default:
                LOGGER.warning("Unrecognized boolean value '" + raw + "' at line " + lineNumber
                        + ", column '" + column + "', treating as null.");
                return null;
        }
    }
}
