package org.example.export;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
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
import org.example.baseline.CandidateDTO;
import org.example.metrics.EvaluationMetrics;

public class ResultExporter {
    private static final Logger LOGGER = Logger.getLogger(ResultExporter.class.getName());
    private static final String RESULTS_CSV = "results.csv";
    private static final String RESULTS_JSON = "results.json";
    private static final String METRICS_JSON = "metrics.json";

    private static final List<String> CSV_HEADER = List.of(
            "fullyQualifiedClassName",
            "baselineFlag",
            "sonarFlag",
            "jdeodorantFlag",
            "wmc",
            "tcc",
            "atfd",
            "cbo",
            "loc",
            "godClass",
            "usedCboFallback",
            "methodCount",
            "fieldCount",
            "dependencyTypeCount",
            "reasons"
    );

    private final ObjectMapper mapper;

    public ResultExporter() {
        this.mapper = createMapper();
    }

    public Path createOutputDir(Path outDir) throws IOException {
        Path resolved = outDir == null ? Path.of("output") : outDir;
        Files.createDirectories(resolved);
        return resolved;
    }

    public List<CandidateDTO> stableSorting(List<CandidateDTO> candidates) {
        List<CandidateDTO> sorted = new ArrayList<>();
        if (candidates != null) {
            for (CandidateDTO candidate : candidates) {
                if (candidate != null) {
                    sorted.add(candidate);
                }
            }
        }
        sorted.sort(Comparator.comparing(ResultExporter::classNameKey)
                .thenComparing(ResultExporter::idKey)
                .thenComparing(ResultExporter::tieBreakerKey));
        return sorted;
    }

    public Path writeCsv(List<CandidateDTO> candidates, Path outDir) throws IOException {
        Path dir = createOutputDir(outDir);
        Path target = dir.resolve(RESULTS_CSV);
        List<CandidateDTO> sorted = stableSorting(candidates);
        LOGGER.info("Export started: " + target);

        StringBuilder builder = new StringBuilder();
        builder.append(String.join(",", CSV_HEADER)).append("\n");
        for (CandidateDTO candidate : sorted) {
            builder.append(CandidateCsvUtil.escapeCsvField(nullToEmpty(candidate.getFullyQualifiedClassName())))
                    .append(",");
            builder.append(candidate.isBaselineFlag()).append(",");
            builder.append(candidate.isSonarFlag()).append(",");
            builder.append(candidate.isJdeodorantFlag()).append(",");
            builder.append(nullToEmpty(candidate.getWmcNullable())).append(",");
            builder.append(nullToEmpty(candidate.getTccNullable())).append(",");
            builder.append(nullToEmpty(candidate.getAtfdNullable())).append(",");
            builder.append(nullToEmpty(candidate.getCboNullable())).append(",");
            builder.append(nullToEmpty(candidate.getLocNullable())).append(",");
            builder.append(candidate.isGodClass()).append(",");
            builder.append(candidate.isUsedCboFallback()).append(",");
            builder.append(candidate.getMethodCount()).append(",");
            builder.append(candidate.getFieldCount()).append(",");
            builder.append(candidate.getDependencyTypeCount()).append(",");
            builder.append(CandidateCsvUtil.escapeCsvField(formatReasons(candidate.getReasons()))).append("\n");
        }

        Files.writeString(
                target,
                builder.toString(),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );
        LOGGER.info("Export finished: " + target + " entries=" + sorted.size());
        return target;
    }

    public Path writeJson(List<CandidateDTO> candidates, Path outDir) throws IOException {
        return writeJson(candidates, null, outDir);
    }

    public Path writeJson(List<CandidateDTO> candidates, EvaluationMetrics metrics, Path outDir) throws IOException {
        Path dir = createOutputDir(outDir);
        Path target = dir.resolve(RESULTS_JSON);
        List<CandidateDTO> sorted = stableSorting(candidates);
        List<CandidateExportRow> rows = toRows(sorted);
        ResultsEnvelope envelope = new ResultsEnvelope(rows, metrics);
        LOGGER.info("Export started: " + target);

        String json = mapper.writeValueAsString(envelope);
        Files.writeString(
                target,
                json + "\n",
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );
        LOGGER.info("Export finished: " + target + " entries=" + rows.size());
        return target;
    }

    public Path writeMetricsJson(EvaluationMetrics metrics, Path outDir) throws IOException {
        if (metrics == null) {
            return null;
        }
        Path dir = createOutputDir(outDir);
        Path target = dir.resolve(METRICS_JSON);
        LOGGER.info("Export started: " + target);
        String json = mapper.writeValueAsString(metrics);
        Files.writeString(
                target,
                json + "\n",
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );
        LOGGER.info("Export finished: " + target);
        return target;
    }

    private static ObjectMapper createMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return objectMapper;
    }

    private static List<CandidateExportRow> toRows(List<CandidateDTO> candidates) {
        List<CandidateExportRow> rows = new ArrayList<>();
        for (CandidateDTO candidate : candidates) {
            rows.add(new CandidateExportRow(candidate));
        }
        return rows;
    }

    private static String classNameKey(CandidateDTO candidate) {
        String name = candidate.getFullyQualifiedClassName();
        return name == null ? "" : name;
    }

    private static String tieBreakerKey(CandidateDTO candidate) {
        return candidate.toString();
    }

    private static String idKey(CandidateDTO candidate) {
        try {
            java.lang.reflect.Method method = candidate.getClass().getMethod("getId");
            Object value = method.invoke(candidate);
            return value == null ? "" : String.valueOf(value);
        } catch (ReflectiveOperationException ex) {
            return "";
        }
    }

    private static String nullToEmpty(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static String formatReasons(List<String> reasons) {
        if (reasons == null || reasons.isEmpty()) {
            return "";
        }
        return String.join(";", reasons);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder({"candidates", "metrics"})
    public static class ResultsEnvelope {
        private final List<CandidateExportRow> candidates;
        private final EvaluationMetrics metrics;

        public ResultsEnvelope(List<CandidateExportRow> candidates, EvaluationMetrics metrics) {
            this.candidates = candidates == null ? List.of() : candidates;
            this.metrics = metrics;
        }

        public List<CandidateExportRow> getCandidates() {
            return candidates;
        }

        public EvaluationMetrics getMetrics() {
            return metrics;
        }
    }

    @JsonPropertyOrder({
            "fullyQualifiedClassName",
            "baselineFlag",
            "sonarFlag",
            "jdeodorantFlag",
            "wmc",
            "tcc",
            "atfd",
            "cbo",
            "loc",
            "godClass",
            "usedCboFallback",
            "methodCount",
            "fieldCount",
            "dependencyTypeCount",
            "reasons"
    })
    public static class CandidateExportRow {
        private final String fullyQualifiedClassName;
        private final boolean baselineFlag;
        private final boolean sonarFlag;
        private final boolean jdeodorantFlag;
        private final Integer wmc;
        private final Double tcc;
        private final Integer atfd;
        private final Integer cbo;
        private final Integer loc;
        private final boolean godClass;
        private final boolean usedCboFallback;
        private final int methodCount;
        private final int fieldCount;
        private final int dependencyTypeCount;
        private final List<String> reasons;

        public CandidateExportRow(CandidateDTO candidate) {
            this.fullyQualifiedClassName = candidate.getFullyQualifiedClassName();
            this.baselineFlag = candidate.isBaselineFlag();
            this.sonarFlag = candidate.isSonarFlag();
            this.jdeodorantFlag = candidate.isJdeodorantFlag();
            this.wmc = candidate.getWmcNullable();
            this.tcc = candidate.getTccNullable();
            this.atfd = candidate.getAtfdNullable();
            this.cbo = candidate.getCboNullable();
            this.loc = candidate.getLocNullable();
            this.godClass = candidate.isGodClass();
            this.usedCboFallback = candidate.isUsedCboFallback();
            this.methodCount = candidate.getMethodCount();
            this.fieldCount = candidate.getFieldCount();
            this.dependencyTypeCount = candidate.getDependencyTypeCount();
            this.reasons = candidate.getReasons();
        }

        public String getFullyQualifiedClassName() {
            return fullyQualifiedClassName;
        }

        public boolean isBaselineFlag() {
            return baselineFlag;
        }

        public boolean isSonarFlag() {
            return sonarFlag;
        }

        public boolean isJdeodorantFlag() {
            return jdeodorantFlag;
        }

        public Integer getWmc() {
            return wmc;
        }

        public Double getTcc() {
            return tcc;
        }

        public Integer getAtfd() {
            return atfd;
        }

        public Integer getCbo() {
            return cbo;
        }

        public Integer getLoc() {
            return loc;
        }

        public boolean isGodClass() {
            return godClass;
        }

        public boolean isUsedCboFallback() {
            return usedCboFallback;
        }

        public int getMethodCount() {
            return methodCount;
        }

        public int getFieldCount() {
            return fieldCount;
        }

        public int getDependencyTypeCount() {
            return dependencyTypeCount;
        }

        public List<String> getReasons() {
            return reasons;
        }
    }
}
