package org.example.evaluation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Computes pairwise Jaccard agreement between God-Class detection tools
 * and exports the results as {@code tool_agreement.csv}.
 */
public class ToolAgreementCalculator {

    private static final Logger LOGGER = Logger.getLogger(ToolAgreementCalculator.class.getName());

    private static final String OUTPUT_FILE = "tool_agreement.csv";
    private static final String CSV_HEADER = "toolA,toolB,jaccard,both,onlyA,onlyB";

    /**
     * Computes the Jaccard similarity index for two sets of predicted FQCNs.
     * Returns 0.0 when both sets are empty (|A∪B| = 0).
     *
     * @param a set of FQCNs from the first tool (null treated as empty)
     * @param b set of FQCNs from the second tool (null treated as empty)
     * @return |A∩B| / |A∪B|, or 0.0 on empty union
     */
    public double jaccard(Set<String> a, Set<String> b) {
        Set<String> normA = normalize(a);
        Set<String> normB = normalize(b);

        int intersectionSize = countIntersection(normA, normB);
        int unionSize = normA.size() + normB.size() - intersectionSize;

        if (unionSize == 0) {
            return 0.0;
        }
        return (double) intersectionSize / unionSize;
    }

    /**
     * Computes pairwise agreement between all three tools.
     * Returns three {@link OverlapResult} entries in alphabetical order:
     * baseline–jdeodorant, baseline–sonar, jdeodorant–sonar.
     *
     * @param baselinePredictions   FQCNs flagged by the baseline tool
     * @param sonarPredictions      FQCNs flagged by SonarQube
     * @param jdeodorantPredictions FQCNs flagged by JDeodorant
     * @return list of pairwise overlap results
     */
    public List<OverlapResult> computeAll(
            Set<String> baselinePredictions,
            Set<String> sonarPredictions,
            Set<String> jdeodorantPredictions
    ) {
        LOGGER.info("ToolAgreementCalculator.computeAll started");
        List<OverlapResult> results = new ArrayList<>();
        results.add(computePair("baseline", baselinePredictions, "jdeodorant", jdeodorantPredictions));
        results.add(computePair("baseline", baselinePredictions, "sonar", sonarPredictions));
        results.add(computePair("jdeodorant", jdeodorantPredictions, "sonar", sonarPredictions));
        LOGGER.info("ToolAgreementCalculator.computeAll finished: pairs=" + results.size());
        return results;
    }

    /**
     * Writes the pairwise agreement results to {@code tool_agreement.csv} in {@code outputDir}.
     *
     * @param results   list of overlap results to export
     * @param outputDir target directory (created if absent)
     * @return path of the written CSV file
     * @throws IOException on write failure
     */
    public Path exportCsv(List<OverlapResult> results, Path outputDir) throws IOException {
        Files.createDirectories(outputDir);
        Path file = outputDir.resolve(OUTPUT_FILE);

        StringBuilder sb = new StringBuilder();
        sb.append(CSV_HEADER).append("\n");
        for (OverlapResult r : results) {
            sb.append(r.getToolA()).append(",");
            sb.append(r.getToolB()).append(",");
            sb.append(r.getJaccard()).append(",");
            sb.append(r.getBoth()).append(",");
            sb.append(r.getOnlyA()).append(",");
            sb.append(r.getOnlyB()).append("\n");
        }

        Files.writeString(file, sb.toString(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        LOGGER.info("ToolAgreementCalculator.exportCsv written: " + file + " rows=" + results.size());
        return file;
    }

    private OverlapResult computePair(String nameA, Set<String> a, String nameB, Set<String> b) {
        Set<String> normA = normalize(a);
        Set<String> normB = normalize(b);

        int both = countIntersection(normA, normB);
        int onlyA = normA.size() - both;
        int onlyB = normB.size() - both;
        int union = both + onlyA + onlyB;
        double j = union == 0 ? 0.0 : (double) both / union;

        return new OverlapResult(nameA, nameB, j, both, onlyA, onlyB);
    }

    private int countIntersection(Set<String> a, Set<String> b) {
        int count = 0;
        for (String item : a) {
            if (b.contains(item)) {
                count++;
            }
        }
        return count;
    }

    private Set<String> normalize(Set<String> input) {
        if (input == null || input.isEmpty()) {
            return Set.of();
        }
        Set<String> normalized = new HashSet<>();
        for (String value : input) {
            if (value != null) {
                normalized.add(value);
            }
        }
        return normalized;
    }
}
