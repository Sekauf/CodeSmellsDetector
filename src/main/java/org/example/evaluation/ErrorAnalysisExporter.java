package org.example.evaluation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.example.baseline.CandidateCsvUtil;
import org.example.baseline.CandidateDTO;

/**
 * Exports False Positive and False Negative lists per tool as CSV.
 * Output files are named {@code fp_fn_<toolname>.csv}, sorted by errorType then FQCN.
 */
public class ErrorAnalysisExporter {

    private static final Logger LOGGER = Logger.getLogger(ErrorAnalysisExporter.class.getName());

    private static final String CSV_HEADER = "fqcn,errorType,methodCount,fieldCount,dependencyTypeCount,flags";
    private static final String FILE_PREFIX = "fp_fn_";
    private static final String FILE_SUFFIX = ".csv";
    private static final String ERROR_FP = "FP";
    private static final String ERROR_FN = "FN";

    /**
     * Exports a FP/FN CSV for a single tool's predictions against ground truth.
     *
     * @param toolName    name of the tool (used in output filename)
     * @param groundTruth map of FQCN to true (God Class) / false (not God Class)
     * @param predictions set of FQCNs predicted as God Classes by the tool
     * @param candidates  map of FQCN to CandidateDTO for metric/flag lookup
     * @param outputDir   target directory (created if absent)
     * @return path of the written CSV file
     * @throws IOException on write failure
     */
    public Path exportErrorList(
            String toolName,
            Map<String, Boolean> groundTruth,
            Set<String> predictions,
            Map<String, CandidateDTO> candidates,
            Path outputDir
    ) throws IOException {
        LOGGER.info("ErrorAnalysisExporter.exportErrorList started: tool=" + toolName);
        Files.createDirectories(outputDir);

        Set<String> fps = computeFalsePositives(groundTruth, predictions);
        Set<String> fns = computeFalseNegatives(groundTruth, predictions);

        List<String[]> rows = buildRows(fps, fns, candidates);
        rows.sort((a, b) -> {
            int cmp = a[1].compareTo(b[1]);
            return cmp != 0 ? cmp : a[0].compareTo(b[0]);
        });

        Path file = outputDir.resolve(FILE_PREFIX + toolName + FILE_SUFFIX);
        writeRows(rows, file);

        LOGGER.info("ErrorAnalysisExporter.exportErrorList finished: tool=" + toolName
                + " fp=" + fps.size() + " fn=" + fns.size() + " file=" + file);
        return file;
    }

    /**
     * Exports FP/FN CSVs for all three tools in a single call.
     *
     * @param groundTruth           map of FQCN to ground-truth labels
     * @param baselinePredictions   FQCNs flagged by baseline
     * @param sonarPredictions      FQCNs flagged by SonarQube
     * @param jdeodorantPredictions FQCNs flagged by JDeodorant
     * @param candidates            map of FQCN to CandidateDTO
     * @param outputDir             target directory (created if absent)
     * @return ordered map of tool name to output file path
     * @throws IOException on write failure
     */
    public Map<String, Path> exportAll(
            Map<String, Boolean> groundTruth,
            Set<String> baselinePredictions,
            Set<String> sonarPredictions,
            Set<String> jdeodorantPredictions,
            Map<String, CandidateDTO> candidates,
            Path outputDir
    ) throws IOException {
        LOGGER.info("ErrorAnalysisExporter.exportAll started");
        Map<String, Path> result = new LinkedHashMap<>();
        result.put("baseline",   exportErrorList("baseline",   groundTruth, baselinePredictions,   candidates, outputDir));
        result.put("sonar",      exportErrorList("sonar",      groundTruth, sonarPredictions,      candidates, outputDir));
        result.put("jdeodorant", exportErrorList("jdeodorant", groundTruth, jdeodorantPredictions, candidates, outputDir));
        LOGGER.info("ErrorAnalysisExporter.exportAll finished: files=" + result.size());
        return result;
    }

    private Set<String> computeFalsePositives(Map<String, Boolean> groundTruth, Set<String> predictions) {
        if (predictions == null || predictions.isEmpty()) {
            return Set.of();
        }
        Set<String> fps = new HashSet<>();
        for (String fqcn : predictions) {
            if (fqcn == null) {
                continue;
            }
            Boolean truth = groundTruth == null ? null : groundTruth.get(fqcn);
            if (!Boolean.TRUE.equals(truth)) {
                fps.add(fqcn);
            }
        }
        return fps;
    }

    private Set<String> computeFalseNegatives(Map<String, Boolean> groundTruth, Set<String> predictions) {
        if (groundTruth == null || groundTruth.isEmpty()) {
            return Set.of();
        }
        Set<String> normPred = (predictions == null) ? Set.of() : predictions;
        Set<String> fns = new HashSet<>();
        for (Map.Entry<String, Boolean> entry : groundTruth.entrySet()) {
            if (Boolean.TRUE.equals(entry.getValue()) && !normPred.contains(entry.getKey())) {
                fns.add(entry.getKey());
            }
        }
        return fns;
    }

    private List<String[]> buildRows(Set<String> fps, Set<String> fns, Map<String, CandidateDTO> candidates) {
        List<String[]> rows = new ArrayList<>();
        for (String fqcn : fps) {
            rows.add(toRow(fqcn, ERROR_FP, candidates));
        }
        for (String fqcn : fns) {
            rows.add(toRow(fqcn, ERROR_FN, candidates));
        }
        return rows;
    }

    private String[] toRow(String fqcn, String errorType, Map<String, CandidateDTO> candidates) {
        CandidateDTO dto = (candidates == null) ? null : candidates.get(fqcn);
        int methodCount = dto != null ? dto.getMethodCount() : 0;
        int fieldCount  = dto != null ? dto.getFieldCount()  : 0;
        int depCount    = dto != null ? dto.getDependencyTypeCount() : 0;
        String flags    = dto != null ? formatFlags(dto) : "";
        return new String[]{fqcn, errorType,
                String.valueOf(methodCount),
                String.valueOf(fieldCount),
                String.valueOf(depCount),
                flags};
    }

    private String formatFlags(CandidateDTO dto) {
        return "baseline=" + dto.isBaselineFlag()
                + ";sonar=" + dto.isSonarFlag()
                + ";jdeodorant=" + dto.isJdeodorantFlag();
    }

    private void writeRows(List<String[]> rows, Path file) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(CSV_HEADER).append("\n");
        for (String[] row : rows) {
            for (int i = 0; i < row.length; i++) {
                if (i > 0) {
                    sb.append(",");
                }
                sb.append(CandidateCsvUtil.escapeCsvField(row[i]));
            }
            sb.append("\n");
        }
        Files.writeString(file, sb.toString(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
}
