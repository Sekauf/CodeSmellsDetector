package org.example.baseline;

import java.util.ArrayList;
import java.util.List;

public final class CandidateCsvUtil {
    /**
     * CSV column order for CandidateDTO exports/imports.
     * Empty fields are allowed for nullable metrics (wmc, tcc, atfd, cbo, loc).
     */
    public static final List<String> HEADER = List.of(
            "fullyQualifiedClassName",
            "baselineFlag",
            "sonarFlag",
            "jdeodorantFlag",
            "wmc",
            "tcc",
            "atfd",
            "cbo",
            "loc",
            "methodCount",
            "fieldCount",
            "dependencyTypeCount",
            "reasons"
    );

    private CandidateCsvUtil() {
    }

    public static String headerLine() {
        return String.join(",", HEADER);
    }

    /**
     * RFC4180-ish escaping:
     * - Quote fields containing comma, quote, or line breaks.
     * - Double quotes inside quoted fields.
     *
     * Example:
     * String value = "com.example.\"Quoted,Name\"";
     * String escaped = CandidateCsvUtil.escapeCsvField(value);
     */
    public static String escapeCsvField(String value) {
        if (value == null) {
            return "";
        }
        boolean needsQuotes = value.indexOf(',') >= 0
                || value.indexOf('"') >= 0
                || value.indexOf('\n') >= 0
                || value.indexOf('\r') >= 0;
        if (!needsQuotes) {
            return value;
        }
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    /**
     * RFC4180-ish parsing for a single line:
     * - Supports quoted fields with doubled quotes.
     * - Commas inside quotes are preserved.
     * - Note: line-based importers do not support embedded newlines in fields.
     */
    public static List<String> parseCsvLine(String line) {
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
                } else if (c == ',') {
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
