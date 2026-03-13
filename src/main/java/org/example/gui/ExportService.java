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

    private final ResultExporter   resultExporter   = new ResultExporter();
    private final LabelCsvExporter labelCsvExporter = new LabelCsvExporter();

    /** Derives the project name from the last component of {@code outputDir}. */
    private static String projectNameOf(Path outputDir) {
        Path fn = outputDir != null ? outputDir.getFileName() : null;
        return fn != null ? fn.toString() : "";
    }

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
        String pn = projectNameOf(outputDir);
        List<Path> existing = new ArrayList<>();
        if (csv      && Files.exists(outputDir.resolve(ResultExporter.csvFileName(pn))))      { existing.add(outputDir.resolve(ResultExporter.csvFileName(pn))); }
        if (json     && Files.exists(outputDir.resolve(ResultExporter.jsonFileName(pn))))     { existing.add(outputDir.resolve(ResultExporter.jsonFileName(pn))); }
        if (labeling && Files.exists(outputDir.resolve(ResultExporter.labelingFileName(pn)))) { existing.add(outputDir.resolve(ResultExporter.labelingFileName(pn))); }
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
        String pn = projectNameOf(outputDir);
        List<Path> written = new ArrayList<>();
        if (csv)      { written.add(resultExporter.writeCsv(candidates, outputDir, ResultExporter.csvFileName(pn))); }
        if (json)     { written.add(resultExporter.writeJson(candidates, outputDir, ResultExporter.jsonFileName(pn))); }
        if (labeling) { written.add(labelCsvExporter.export(candidates, outputDir.resolve(ResultExporter.labelingFileName(pn)))); }
        return written;
    }
}
