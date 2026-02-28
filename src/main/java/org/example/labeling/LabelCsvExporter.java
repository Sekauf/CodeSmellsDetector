package org.example.labeling;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.logging.Logger;
import org.example.baseline.CandidateCsvUtil;
import org.example.baseline.CandidateDTO;
import org.example.export.ResultExporter;

/**
 * Exports a list of {@link CandidateDTO} candidates as a labeling template CSV.
 * All label columns (k1–k4, comment, finalLabel) are written empty so a human annotator
 * can fill them in.
 */
public class LabelCsvExporter {

    private static final Logger LOGGER = Logger.getLogger(LabelCsvExporter.class.getName());

    /** Column order of the labeling CSV (matches the import contract for US-02). */
    public static final List<String> HEADER = List.of(
            "fullyQualifiedClassName",
            "baselineFlag",
            "sonarFlag",
            "jdeodorantFlag",
            "methodCount",
            "fieldCount",
            "dependencyTypeCount",
            "k1",
            "k2",
            "k3",
            "k4",
            "comment",
            "finalLabel"
    );

    private static final int EMPTY_LABEL_COLUMNS = 6; // k1,k2,k3,k4,comment,finalLabel

    private final ResultExporter resultExporter;

    /** Creates an exporter with a default {@link ResultExporter}. */
    public LabelCsvExporter() {
        this.resultExporter = new ResultExporter();
    }

    /**
     * Writes candidates sorted alphabetically by FQCN to {@code outputFile}.
     * Label columns are left empty for manual annotation.
     *
     * @param candidates list of candidates to export (null-safe)
     * @param outputFile target file path (parent directory is created if absent)
     * @return the written file path
     * @throws IOException on write failure
     */
    public Path export(List<CandidateDTO> candidates, Path outputFile) throws IOException {
        List<CandidateDTO> sorted = resultExporter.stableSorting(candidates);
        LOGGER.info("LabelCsvExporter started: " + outputFile + " candidates=" + sorted.size());

        ensureParentDirectoryExists(outputFile);

        String content = buildCsvContent(sorted);
        Files.writeString(
                outputFile,
                content,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );

        LOGGER.info("LabelCsvExporter finished: " + outputFile + " rows=" + sorted.size());
        return outputFile;
    }

    private String buildCsvContent(List<CandidateDTO> sorted) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", HEADER)).append("\n");
        for (CandidateDTO candidate : sorted) {
            appendCandidateRow(sb, candidate);
        }
        return sb.toString();
    }

    private void appendCandidateRow(StringBuilder sb, CandidateDTO candidate) {
        sb.append(CandidateCsvUtil.escapeCsvField(candidate.getFullyQualifiedClassName())).append(",");
        sb.append(candidate.isBaselineFlag()).append(",");
        sb.append(candidate.isSonarFlag()).append(",");
        sb.append(candidate.isJdeodorantFlag()).append(",");
        sb.append(candidate.getMethodCount()).append(",");
        sb.append(candidate.getFieldCount()).append(",");
        sb.append(candidate.getDependencyTypeCount());
        appendEmptyLabelColumns(sb);
        sb.append("\n");
    }

    private void appendEmptyLabelColumns(StringBuilder sb) {
        for (int i = 0; i < EMPTY_LABEL_COLUMNS; i++) {
            sb.append(",");
        }
    }

    private static void ensureParentDirectoryExists(Path file) throws IOException {
        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }
}
