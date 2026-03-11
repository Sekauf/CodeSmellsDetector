package org.example.gui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.example.baseline.CandidateDTO;
import org.example.export.ResultExporter;
import org.example.labeling.LabelCsvExporter;

/**
 * Performs file-based export operations used by {@link ExportDialog}.
 * Encapsulates {@link ResultExporter} and {@link LabelCsvExporter} calls
 * and exposes overwrite-detection logic that can be unit-tested independently.
 */
public class ExportService {

    static final String FILE_CSV      = "results.csv";
    static final String FILE_JSON     = "results.json";
    static final String FILE_LABELING = "labeling_input.csv";

    private final ResultExporter   resultExporter   = new ResultExporter();
    private final LabelCsvExporter labelCsvExporter = new LabelCsvExporter();

    /**
     * Returns the paths of files that already exist in {@code outputDir}
     * for the selected export options.
     *
     * @param outputDir target directory
     * @param csv       whether results.csv is selected
     * @param json      whether results.json is selected
     * @param labeling  whether labeling_input.csv is selected
     * @return list of existing file paths (never null, may be empty)
     */
    public List<Path> findExisting(Path outputDir, boolean csv, boolean json, boolean labeling) {
        List<Path> existing = new ArrayList<>();
        if (csv      && Files.exists(outputDir.resolve(FILE_CSV)))      { existing.add(outputDir.resolve(FILE_CSV)); }
        if (json     && Files.exists(outputDir.resolve(FILE_JSON)))     { existing.add(outputDir.resolve(FILE_JSON)); }
        if (labeling && Files.exists(outputDir.resolve(FILE_LABELING))) { existing.add(outputDir.resolve(FILE_LABELING)); }
        return existing;
    }

    /**
     * Runs the selected exports and returns the list of written file paths.
     *
     * @param outputDir  target directory (created if absent)
     * @param candidates candidates to export (null-safe)
     * @param csv        write results.csv
     * @param json       write results.json
     * @param labeling   write labeling_input.csv
     * @return paths of all files written during this invocation
     * @throws IOException on write failure
     */
    public List<Path> runExport(Path outputDir, List<CandidateDTO> candidates,
            boolean csv, boolean json, boolean labeling) throws IOException {
        List<Path> written = new ArrayList<>();
        if (csv)      { written.add(resultExporter.writeCsv(candidates, outputDir)); }
        if (json)     { written.add(resultExporter.writeJson(candidates, outputDir)); }
        if (labeling) { written.add(labelCsvExporter.export(candidates, outputDir.resolve(FILE_LABELING))); }
        return written;
    }
}
