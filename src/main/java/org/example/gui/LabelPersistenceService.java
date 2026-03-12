package org.example.gui;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.example.baseline.CandidateCsvUtil;
import org.example.labeling.LabelCsvImporter;
import org.example.labeling.LabelDTO;

/**
 * Saves and loads inline labeling data as {@code labeling_input.csv}.
 * The CSV format is compatible with {@link LabelCsvImporter}.
 */
public class LabelPersistenceService {

    private static final Logger LOGGER = Logger.getLogger(LabelPersistenceService.class.getName());

    private static final String HEADER =
            "fullyQualifiedClassName,k1,k2,k3,k4,comment,finalLabel";

    /**
     * Writes the given labels to a CSV file at {@code outputFile}.
     * The parent directory is created if absent.
     *
     * @param labels     list of label DTOs (may be empty)
     * @param outputFile target path
     * @return the written file path
     * @throws IOException on write failure
     */
    public Path save(List<LabelDTO> labels, Path outputFile) throws IOException {
        Path parent = outputFile.getParent();
        if (parent != null) { Files.createDirectories(parent); }

        StringBuilder sb = new StringBuilder();
        sb.append(HEADER).append("\n");
        for (LabelDTO dto : labels) {
            appendRow(sb, dto);
        }

        Files.writeString(outputFile, sb.toString(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        LOGGER.info("LabelPersistenceService saved: " + outputFile + " rows=" + labels.size());
        return outputFile;
    }

    private void appendRow(StringBuilder sb, LabelDTO dto) {
        sb.append(CandidateCsvUtil.escapeCsvField(dto.getFullyQualifiedClassName())).append(",");
        sb.append(boolStr(dto.getK1())).append(",");
        sb.append(boolStr(dto.getK2())).append(",");
        sb.append(boolStr(dto.getK3())).append(",");
        sb.append(boolStr(dto.getK4())).append(",");
        sb.append(CandidateCsvUtil.escapeCsvField(dto.getComment())).append(",");
        sb.append(dto.getFinalLabel() != null ? dto.getFinalLabel().name() : "");
        sb.append("\n");
    }

    private String boolStr(Boolean b) {
        return b == null ? "" : b.toString();
    }

    /**
     * Loads labels from an existing labeling CSV.
     *
     * @param file path to the CSV file
     * @return map from FQCN to LabelDTO
     * @throws IOException on read failure
     */
    public Map<String, LabelDTO> load(Path file) throws IOException {
        LOGGER.info("LabelPersistenceService loading: " + file);
        return new LabelCsvImporter().importLabels(file);
    }
}
