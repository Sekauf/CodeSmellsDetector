package org.example.labeling;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import org.example.baseline.CandidateCsvUtil;

/**
 * Exports a blind second-review sample from a set of primary-labeled classes.
 * The output CSV omits primary label values so a second reviewer can label independently.
 * The CSV format matches {@link LabelCsvImporter} requirements.
 */
public class SecondReviewExporter {

    private static final Logger LOGGER = Logger.getLogger(SecondReviewExporter.class.getName());

    /** CSV header for the blind second-review file (compatible with LabelCsvImporter). */
    public static final String CSV_HEADER = "fullyQualifiedClassName,k1,k2,k3,k4,comment,finalLabel";

    /** Number of empty label columns after the FQCN. */
    private static final int EMPTY_LABEL_COLUMN_COUNT = 6;

    private static final double DEFAULT_FRACTION = 0.2;

    /**
     * Exports a 20% blind sample of the given primary labels to {@code outputFile}.
     * Labels are sorted alphabetically by FQCN before sampling; all label columns are left empty.
     *
     * @param primaryLabels list of primary-labeled DTOs (null-safe)
     * @param outputFile    target CSV file (parent directory is created if absent)
     * @return the written file path
     * @throws IOException on write failure
     */
    public Path export(List<LabelDTO> primaryLabels, Path outputFile) throws IOException {
        List<LabelDTO> sorted = sortedByFqcn(primaryLabels);
        int sampleSize = (int) Math.round(sorted.size() * DEFAULT_FRACTION);
        List<LabelDTO> sample = sorted.subList(0, sampleSize);

        LOGGER.info("SecondReviewExporter started: total=" + sorted.size()
                + " sample=" + sample.size() + " output=" + outputFile);

        ensureParentDirectoryExists(outputFile);
        String content = buildCsvContent(sample);
        Files.writeString(outputFile, content, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        LOGGER.info("SecondReviewExporter finished: " + outputFile + " rows=" + sample.size());
        return outputFile;
    }

    private List<LabelDTO> sortedByFqcn(List<LabelDTO> labels) {
        List<LabelDTO> list = new ArrayList<>();
        if (labels != null) {
            for (LabelDTO dto : labels) {
                if (dto != null) {
                    list.add(dto);
                }
            }
        }
        list.sort(Comparator.comparing(LabelDTO::getFullyQualifiedClassName));
        return list;
    }

    private String buildCsvContent(List<LabelDTO> sample) {
        StringBuilder sb = new StringBuilder();
        sb.append(CSV_HEADER).append("\n");
        for (LabelDTO dto : sample) {
            appendBlindRow(sb, dto);
        }
        return sb.toString();
    }

    private void appendBlindRow(StringBuilder sb, LabelDTO dto) {
        sb.append(CandidateCsvUtil.escapeCsvField(dto.getFullyQualifiedClassName()));
        for (int i = 0; i < EMPTY_LABEL_COLUMN_COUNT; i++) {
            sb.append(",");
        }
        sb.append("\n");
    }

    private static void ensureParentDirectoryExists(Path file) throws IOException {
        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }
}
