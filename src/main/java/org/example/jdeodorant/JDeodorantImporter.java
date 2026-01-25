package org.example.jdeodorant;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import org.example.baseline.CandidateDTO;

public class JDeodorantImporter {
    private static final Logger LOGGER = Logger.getLogger(JDeodorantImporter.class.getName());
    private static final List<String> CLASS_HEADER_VARIANTS = List.of(
            "class",
            "classname",
            "type",
            "entity",
            "qualifiedname",
            "fullyqualifiedname",
            "fullyqualifiedclassname"
    );
    private static final List<String> SMELL_HEADER_VARIANTS = List.of(
            "smell",
            "smelltype",
            "codesmell",
            "smell type"
    );

    /**
     * Imports a JDeodorant CSV export (manual fallback) and maps God Class entries to {@link CandidateDTO}.
     * <p>
     * Header handling:
     * - Supported class column variants (case-insensitive, whitespace collapsed): Class, ClassName, Type, Entity,
     *   QualifiedName, FullyQualifiedName.
     * - Supported smell column variants (case-insensitive, whitespace collapsed): Smell, SmellType, CodeSmell,
     *   Smell Type.
     * <p>
     * Parsing rules:
     * - Delimiter auto-detected from header among comma, semicolon, or tab.
     * - RFC4180-ish quoting supported (delimiters inside quotes are preserved).
     * <p>
     * Filtering:
     * - Accepts smell labels containing "God Class", "GodClass", "Blob", or "Monster Class".
     */
    public List<CandidateDTO> importJDeodorantCsv(String filePath) throws IOException {
        return importJDeodorantCsv(filePath, "God Class");
    }

    /**
     * Imports a JDeodorant CSV export (manual fallback) and filters by a target smell type.
     */
    public List<CandidateDTO> importJDeodorantCsv(String filePath, String smellType) throws IOException {
        Objects.requireNonNull(filePath, "filePath");
        Path path = Path.of(filePath);
        LOGGER.info("JDeodorant manual CSV import used: " + path);
        if (!Files.exists(path)) {
            LOGGER.warning("JDeodorant CSV file not found: " + path);
            LOGGER.info("JDeodorant CSV import finished. Candidates=0");
            return List.of();
        }

        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        if (lines.isEmpty()) {
            LOGGER.info("JDeodorant CSV import finished. Candidates=0");
            return List.of();
        }

        int headerIndex = firstNonEmptyLineIndex(lines);
        if (headerIndex < 0) {
            LOGGER.info("JDeodorant CSV import finished. Candidates=0");
            return List.of();
        }

        char delimiter = detectDelimiter(lines.get(headerIndex));
        List<String> headerFields = parseCsvLine(lines.get(headerIndex), delimiter);
        stripBomFromFirstHeaderCell(headerFields);
        Map<String, Integer> headerMap = buildHeaderMap(headerFields);
        Integer classIndex = findClassColumn(headerMap);
        Integer smellIndex = findSmellColumn(headerMap);
        if (classIndex == null) {
            throw new IllegalArgumentException("Missing class column in JDeodorant CSV header. "
                    + "Found: " + formatHeaderFields(headerFields) + ". "
                    + "Expected one of: " + String.join(", ", CLASS_HEADER_VARIANTS));
        }
        if (smellIndex == null) {
            throw new IllegalArgumentException("Missing smell column in JDeodorant CSV header. "
                    + "Found: " + formatHeaderFields(headerFields) + ". "
                    + "Expected one of: " + String.join(", ", SMELL_HEADER_VARIANTS));
        }

        Map<String, CandidateDTO> candidates = new HashMap<>();
        for (int i = headerIndex + 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.trim().isEmpty()) {
                continue;
            }
            List<String> fields = parseCsvLine(line, delimiter);
            String fqn = value(fields, classIndex);
            if (fqn == null || fqn.isBlank()) {
                continue;
            }
            String smell = value(fields, smellIndex);
            if (smellIndex != null && !isTargetSmell(smell, smellType)) {
                continue;
            }
            candidates.putIfAbsent(fqn, CandidateDTO.builder(fqn).jdeodorantFlag(true).build());
        }

        List<CandidateDTO> result = new ArrayList<>(candidates.values());
        result.sort(Comparator.comparing(CandidateDTO::getFullyQualifiedClassName));
        LOGGER.info("JDeodorant CSV import finished. Candidates=" + result.size());
        return result;
    }

    private int firstNonEmptyLineIndex(List<String> lines) {
        for (int i = 0; i < lines.size(); i++) {
            if (!lines.get(i).trim().isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    private Map<String, Integer> buildHeaderMap(List<String> headerFields) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < headerFields.size(); i++) {
            String normalized = normalizeHeader(headerFields.get(i));
            if (!normalized.isEmpty()) {
                map.putIfAbsent(normalized, i);
            }
        }
        return map;
    }

    private Integer findClassColumn(Map<String, Integer> headerMap) {
        return findHeaderIndex(headerMap, CLASS_HEADER_VARIANTS);
    }

    private Integer findSmellColumn(Map<String, Integer> headerMap) {
        return findHeaderIndex(headerMap, SMELL_HEADER_VARIANTS);
    }

    private Integer findHeaderIndex(Map<String, Integer> headerMap, List<String> variants) {
        for (String variant : variants) {
            String normalized = normalizeHeader(variant);
            Integer index = headerMap.get(normalized);
            if (index != null) {
                return index;
            }
        }
        return null;
    }

    private String value(List<String> fields, Integer index) {
        if (index == null || index < 0 || index >= fields.size()) {
            return "";
        }
        return fields.get(index).trim();
    }

    private boolean isTargetSmell(String smell, String smellType) {
        if (smell == null) {
            return false;
        }
        String normalizedSmell = normalizeHeader(smell);
        String resolvedSmellType = (smellType == null || smellType.isBlank()) ? "God Class" : smellType;
        String normalizedTarget = normalizeHeader(resolvedSmellType);

        if ("god class".equals(normalizedTarget) || "godclass".equals(normalizedTarget)) {
            return normalizedSmell.contains("god class")
                    || normalizedSmell.contains("godclass")
                    || normalizedSmell.contains("monster class")
                    || normalizedSmell.contains("blob");
        }

        if (normalizedSmell.contains(normalizedTarget)) {
            return true;
        }
        String noSpaceTarget = normalizedTarget.replace(" ", "");
        String noSpaceSmell = normalizedSmell.replace(" ", "");
        return !noSpaceTarget.isEmpty() && noSpaceSmell.contains(noSpaceTarget);
    }

    private String normalizeHeader(String header) {
        if (header == null) {
            return "";
        }
        String trimmed = trimSurroundingQuotes(header.trim());
        String collapsed = collapseWhitespace(trimmed).toLowerCase();
        StringBuilder normalized = new StringBuilder();
        for (int i = 0; i < collapsed.length(); i++) {
            char c = collapsed.charAt(i);
            if (c >= 'a' && c <= 'z') {
                normalized.append(c);
            } else if (c >= '0' && c <= '9') {
                normalized.append(c);
            } else if (c == ' ') {
                normalized.append(' ');
            }
        }
        return normalized.toString();
    }

    private String trimSurroundingQuotes(String value) {
        if (value == null || value.length() < 2) {
            return value == null ? "" : value;
        }
        if (value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private String collapseWhitespace(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        boolean previousWasSpace = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isWhitespace(c)) {
                if (!previousWasSpace) {
                    result.append(' ');
                    previousWasSpace = true;
                }
            } else {
                result.append(c);
                previousWasSpace = false;
            }
        }
        return result.toString().trim();
    }

    private String formatHeaderFields(List<String> headerFields) {
        if (headerFields == null || headerFields.isEmpty()) {
            return "<empty>";
        }
        List<String> trimmed = new ArrayList<>();
        for (String header : headerFields) {
            String value = header == null ? "" : header.trim();
            trimmed.add(value.isEmpty() ? "<empty>" : value);
        }
        return String.join(", ", trimmed);
    }

    char detectDelimiter(String headerLine) {
        if (headerLine == null) {
            return ',';
        }
        char best = ',';
        int bestCount = -1;
        for (char candidate : new char[]{',', ';', '\t'}) {
            int count = parseCsvLine(headerLine, candidate).size();
            if (count > bestCount) {
                bestCount = count;
                best = candidate;
            }
        }
        return best;
    }

    void stripBomFromFirstHeaderCell(List<String> headerFields) {
        if (headerFields == null || headerFields.isEmpty()) {
            return;
        }
        String first = headerFields.get(0);
        if (first != null && !first.isEmpty() && first.charAt(0) == '\uFEFF') {
            headerFields.set(0, first.substring(1));
        }
    }

    List<String> parseCsvLine(String line, char delimiter) {
        List<String> fields = new ArrayList<>();
        if (line == null || line.isEmpty()) {
            fields.add("");
            return fields;
        }

        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        int i = 0;
        while (i < line.length()) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i += 2;
                        continue;
                    }
                    inQuotes = false;
                    i++;
                    continue;
                }
                current.append(c);
                i++;
            } else {
                if (c == '"') {
                    inQuotes = true;
                    i++;
                } else if (c == delimiter) {
                    fields.add(current.toString());
                    current.setLength(0);
                    i++;
                } else {
                    current.append(c);
                    i++;
                }
            }
        }
        fields.add(current.toString());
        return fields;
    }
}
