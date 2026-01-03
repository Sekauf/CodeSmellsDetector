package org.example.baseline;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BaselineCandidateExporter {
    /**
     * Exports a CSV with a header line even when the candidate list is empty.
     */
    public void exportToCsv(Path outputPath, List<CandidateDTO> candidates) throws IOException {
        List<CandidateDTO> sorted = new ArrayList<>(candidates);
        sorted.sort(Comparator.comparing(
                CandidateDTO::getFullyQualifiedClassName,
                Comparator.nullsFirst(String::compareTo)
        ));

        StringBuilder builder = new StringBuilder();
        builder.append(CandidateCsvUtil.headerLine()).append("\n");
        for (CandidateDTO candidate : sorted) {
            builder.append(CandidateCsvUtil.escapeCsvField(nullToEmpty(candidate.getFullyQualifiedClassName()))).append(",");
            builder.append(candidate.isBaselineFlag()).append(",");
            builder.append(candidate.isSonarFlag()).append(",");
            builder.append(candidate.isJdeodorantFlag()).append(",");
            builder.append(nullToEmpty(candidate.getWmcNullable())).append(",");
            builder.append(nullToEmpty(candidate.getTccNullable())).append(",");
            builder.append(nullToEmpty(candidate.getAtfdNullable())).append(",");
            builder.append(nullToEmpty(candidate.getCboNullable())).append(",");
            builder.append(nullToEmpty(candidate.getLocNullable())).append(",");
            builder.append(candidate.getMethodCount()).append(",");
            builder.append(candidate.getFieldCount()).append(",");
            builder.append(candidate.getDependencyTypeCount()).append(",");
            builder.append(CandidateCsvUtil.escapeCsvField(formatReasons(candidate.getReasons()))).append("\n");
        }

        Files.createDirectories(outputPath.getParent());
        Files.writeString(outputPath, builder.toString(), StandardCharsets.UTF_8);
    }

    public void exportToCsv(List<CandidateDTO> candidates, Path outputPath) throws IOException {
        exportToCsv(outputPath, candidates);
    }

    private String formatReasons(List<String> reasons) {
        if (reasons == null || reasons.isEmpty()) {
            return "";
        }
        return String.join("|", reasons);
    }

    private String nullToEmpty(Object value) {
        return value == null ? "" : value.toString();
    }
}
