package org.example.baseline;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BaselineCandidateImporter {
    /**
     * Imports a CSV that is expected to include a header line.
     * A header-only file yields an empty list.
     */
    public List<CandidateDTO> importFromCsv(Path file) throws IOException {
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("CSV file is empty: " + file);
        }

        List<String> headerFields = CandidateCsvUtil.parseCsvLine(lines.get(0));
        Map<String, Integer> headerMap = buildHeaderMap(headerFields);
        validateHeader(headerMap, lines.get(0));

        List<CandidateDTO> candidates = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.trim().isEmpty()) {
                continue;
            }
            List<String> fields = CandidateCsvUtil.parseCsvLine(line);
            int lineNumber = i + 1;
            candidates.add(parseCandidate(fields, headerMap, lineNumber));
        }
        return candidates;
    }

    private Map<String, Integer> buildHeaderMap(List<String> headerFields) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < headerFields.size(); i++) {
            map.put(headerFields.get(i), i);
        }
        return map;
    }

    private void validateHeader(Map<String, Integer> headerMap, String headerLine) {
        for (String required : CandidateCsvUtil.HEADER) {
            if (!headerMap.containsKey(required)) {
                throw new IllegalArgumentException("Missing required header '" + required + "' in: " + headerLine);
            }
        }
    }

    private CandidateDTO parseCandidate(List<String> fields, Map<String, Integer> headerMap, int lineNumber) {
        String fqn = value(fields, headerMap, "fullyQualifiedClassName");
        if (fqn == null || fqn.isEmpty()) {
            throw new IllegalArgumentException("Invalid value at line " + lineNumber
                    + ", column 'fullyQualifiedClassName': value is empty, expected non-empty String.");
        }

        boolean baselineFlag = parseBoolean(value(fields, headerMap, "baselineFlag"),
                "baselineFlag", lineNumber);
        boolean sonarFlag = parseBoolean(value(fields, headerMap, "sonarFlag"),
                "sonarFlag", lineNumber);
        boolean jdeodorantFlag = parseBoolean(value(fields, headerMap, "jdeodorantFlag"),
                "jdeodorantFlag", lineNumber);

        Integer wmc = parseInteger(value(fields, headerMap, "wmc"), "wmc", lineNumber);
        Double tcc = parseDouble(value(fields, headerMap, "tcc"), "tcc", lineNumber);
        Integer atfd = parseInteger(value(fields, headerMap, "atfd"), "atfd", lineNumber);
        Integer cbo = parseInteger(value(fields, headerMap, "cbo"), "cbo", lineNumber);
        Integer loc = parseInteger(value(fields, headerMap, "loc"), "loc", lineNumber);

        int methodCount = parseIntDefault(value(fields, headerMap, "methodCount"), "methodCount", lineNumber);
        int fieldCount = parseIntDefault(value(fields, headerMap, "fieldCount"), "fieldCount", lineNumber);
        int dependencyTypeCount = parseIntDefault(value(fields, headerMap, "dependencyTypeCount"),
                "dependencyTypeCount", lineNumber);

        List<String> reasons = parseReasons(value(fields, headerMap, "reasons"));

        return new CandidateDTO(
                fqn,
                baselineFlag,
                sonarFlag,
                jdeodorantFlag,
                wmc,
                tcc,
                atfd,
                cbo,
                loc,
                methodCount,
                fieldCount,
                dependencyTypeCount,
                reasons
        );
    }

    private String value(List<String> fields, Map<String, Integer> headerMap, String key) {
        Integer index = headerMap.get(key);
        if (index == null) {
            return "";
        }
        if (index >= fields.size()) {
            return "";
        }
        return fields.get(index);
    }

    private boolean parseBoolean(String value, String column, int lineNumber) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        throw new IllegalArgumentException("Invalid value at line " + lineNumber
                + ", column '" + column + "': '" + value + "', expected boolean.");
    }

    private Integer parseInteger(String value, String column, int lineNumber) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid value at line " + lineNumber
                    + ", column '" + column + "': '" + value + "', expected integer.", ex);
        }
    }

    private int parseIntDefault(String value, String column, int lineNumber) {
        if (value == null || value.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid value at line " + lineNumber
                    + ", column '" + column + "': '" + value + "', expected integer.", ex);
        }
    }

    private Double parseDouble(String value, String column, int lineNumber) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid value at line " + lineNumber
                    + ", column '" + column + "': '" + value + "', expected double.", ex);
        }
    }

    private List<String> parseReasons(String value) {
        List<String> reasons = new ArrayList<>();
        if (value == null || value.isEmpty()) {
            return reasons;
        }
        for (String part : value.split("\\|")) {
            if (!part.isEmpty()) {
                reasons.add(part);
            }
        }
        return reasons;
    }
}
