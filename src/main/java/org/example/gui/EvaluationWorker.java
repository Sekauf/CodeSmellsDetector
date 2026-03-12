package org.example.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Logger;
import javax.swing.SwingWorker;
import org.example.baseline.CandidateDTO;
import org.example.evaluation.ErrorAnalysisExporter;
import org.example.evaluation.OverlapResult;
import org.example.evaluation.ToolAgreementCalculator;
import org.example.evaluation.ToolEvaluator;
import org.example.labeling.LabelCsvImporter;
import org.example.labeling.LabelDTO;
import org.example.labeling.SecondReviewEvaluator;
import org.example.metrics.EvaluationMetrics;
import org.example.metrics.ReliabilityMetrics;
import java.nio.file.Path;

/**
 * SwingWorker that runs the evaluation pipeline on in-memory candidate and label data.
 * Calls evaluation classes directly, avoiding a round-trip through the CSV file system.
 */
public class EvaluationWorker extends SwingWorker<EvaluationWorker.EvaluationResult, Void> {

    private static final Logger LOGGER = Logger.getLogger(EvaluationWorker.class.getName());

    private final CandidateTableModel        model;
    private final Path                       outputDir;
    private final Path                       secondReviewFile;
    private final Consumer<EvaluationResult> onComplete;
    private final Consumer<Throwable>        onFail;

    /**
     * Creates an EvaluationWorker.
     *
     * @param model            source of candidate and label data
     * @param outputDir        directory for output files (evaluation JSONs, CSVs)
     * @param secondReviewFile optional second-reviewer CSV (may be null)
     * @param onComplete       called on EDT with result when done
     * @param onFail           called on EDT with exception on failure
     */
    public EvaluationWorker(CandidateTableModel model, Path outputDir, Path secondReviewFile,
            Consumer<EvaluationResult> onComplete, Consumer<Throwable> onFail) {
        this.model            = model;
        this.outputDir        = outputDir;
        this.secondReviewFile = secondReviewFile;
        this.onComplete       = onComplete;
        this.onFail           = onFail;
    }

    @Override
    protected EvaluationResult doInBackground() throws Exception {
        LOGGER.info("EvaluationWorker started");
        List<LabelDTO> labels       = model.getAllLabels();
        Map<String, CandidateDTO> candidateMap = buildCandidateMap();
        Map<String, Boolean>      groundTruth  = buildGroundTruth(labels);
        int total = groundTruth.size();

        Set<String> bl = buildPredSet(groundTruth, candidateMap, CandidateDTO::isBaselineFlag);
        Set<String> so = buildPredSet(groundTruth, candidateMap, CandidateDTO::isSonarFlag);
        Set<String> jd = buildPredSet(groundTruth, candidateMap, CandidateDTO::isJdeodorantFlag);

        Map<String, EvaluationMetrics> metrics   = runToolEvaluation(groundTruth, bl, so, jd, total);
        List<OverlapResult>            agreement  = runAgreement(bl, so, jd);
        exportErrorAnalysis(groundTruth, bl, so, jd, candidateMap);

        Set<String> actualPos = extractPositives(groundTruth);
        Map<String, List<String>> fpMap = buildListMap(bl, so, jd, p -> fpList(p, actualPos));
        Map<String, List<String>> fnMap = buildListMap(bl, so, jd, p -> fnList(p, actualPos));

        ReliabilityMetrics reliability = runReliability(labels);
        LOGGER.info("EvaluationWorker finished: labeled=" + total);
        return new EvaluationResult(metrics, agreement, fpMap, fnMap, reliability);
    }

    @Override
    protected void done() {
        try { onComplete.accept(get()); }
        catch (Exception e) { onFail.accept(e); }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Map<String, CandidateDTO> buildCandidateMap() {
        Map<String, CandidateDTO> map = new LinkedHashMap<>();
        for (int i = 0; i < model.getRowCount(); i++) {
            CandidateDTO c = model.getRow(i);
            map.put(c.getFullyQualifiedClassName(), c);
        }
        return map;
    }

    private Map<String, Boolean> buildGroundTruth(List<LabelDTO> labels) {
        Map<String, Boolean> truth = new LinkedHashMap<>();
        for (LabelDTO l : labels) {
            if (l.getFinalLabel() == null) { continue; }
            truth.put(l.getFullyQualifiedClassName(),
                    LabelDTO.FinalLabel.GOD_CLASS.equals(l.getFinalLabel()));
        }
        return truth;
    }

    private Set<String> buildPredSet(Map<String, Boolean> groundTruth,
            Map<String, CandidateDTO> candidates, Predicate<CandidateDTO> flag) {
        Set<String> set = new HashSet<>();
        for (String fqcn : groundTruth.keySet()) {
            CandidateDTO c = candidates.get(fqcn);
            if (c != null && flag.test(c)) { set.add(fqcn); }
        }
        return set;
    }

    private Map<String, EvaluationMetrics> runToolEvaluation(Map<String, Boolean> gt,
            Set<String> bl, Set<String> so, Set<String> jd, int total) {
        try {
            ToolEvaluator ev = new ToolEvaluator();
            Map<String, EvaluationMetrics> r = ev.evaluateAll(gt, bl, so, jd, total);
            ev.exportJson(r, outputDir);
            return r;
        } catch (Exception e) {
            LOGGER.warning("Tool evaluation failed: " + e.getMessage());
            return Collections.emptyMap();
        }
    }

    private List<OverlapResult> runAgreement(Set<String> bl, Set<String> so, Set<String> jd) {
        try {
            ToolAgreementCalculator calc = new ToolAgreementCalculator();
            List<OverlapResult> r = calc.computeAll(bl, so, jd);
            calc.exportCsv(r, outputDir);
            return r;
        } catch (Exception e) {
            LOGGER.warning("Agreement analysis failed: " + e.getMessage());
            return List.of();
        }
    }

    private void exportErrorAnalysis(Map<String, Boolean> gt, Set<String> bl, Set<String> so,
            Set<String> jd, Map<String, CandidateDTO> cands) {
        try {
            new ErrorAnalysisExporter().exportAll(gt, bl, so, jd, cands, outputDir);
        } catch (Exception e) {
            LOGGER.warning("Error analysis export failed: " + e.getMessage());
        }
    }

    private Set<String> extractPositives(Map<String, Boolean> gt) {
        Set<String> pos = new HashSet<>();
        gt.forEach((k, v) -> { if (Boolean.TRUE.equals(v)) pos.add(k); });
        return pos;
    }

    private Map<String, List<String>> buildListMap(Set<String> bl, Set<String> so, Set<String> jd,
            java.util.function.Function<Set<String>, List<String>> fn) {
        Map<String, List<String>> map = new LinkedHashMap<>();
        map.put("baseline",   fn.apply(bl));
        map.put("sonar",      fn.apply(so));
        map.put("jdeodorant", fn.apply(jd));
        return map;
    }

    private List<String> fpList(Set<String> predicted, Set<String> actualPos) {
        List<String> fp = new ArrayList<>(predicted);
        fp.removeAll(actualPos);
        Collections.sort(fp);
        return fp;
    }

    private List<String> fnList(Set<String> predicted, Set<String> actualPos) {
        List<String> fn = new ArrayList<>(actualPos);
        fn.removeAll(predicted);
        Collections.sort(fn);
        return fn;
    }

    private ReliabilityMetrics runReliability(List<LabelDTO> primaryLabels) {
        if (secondReviewFile == null) { return null; }
        try {
            Map<String, LabelDTO> primary = new LinkedHashMap<>();
            primaryLabels.forEach(l -> primary.put(l.getFullyQualifiedClassName(), l));
            Map<String, LabelDTO> secondary = new LabelCsvImporter().importLabels(secondReviewFile);
            return new SecondReviewEvaluator().evaluate(primary, secondary, outputDir);
        } catch (Exception e) {
            LOGGER.warning("Reliability evaluation failed: " + e.getMessage());
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // Result DTO
    // -------------------------------------------------------------------------

    /** Immutable result returned to the EDT after evaluation completes. */
    public static final class EvaluationResult {
        /** Per-tool evaluation metrics (keys: baseline, sonar, jdeodorant). */
        public final Map<String, EvaluationMetrics> toolMetrics;
        /** Pairwise tool-agreement results. */
        public final List<OverlapResult>             agreement;
        /** False-positive FQCNs per tool. */
        public final Map<String, List<String>>       fpFqcns;
        /** False-negative FQCNs per tool. */
        public final Map<String, List<String>>       fnFqcns;
        /** Reliability metrics; null when no second-review file was provided. */
        public final ReliabilityMetrics              reliability;

        EvaluationResult(Map<String, EvaluationMetrics> toolMetrics, List<OverlapResult> agreement,
                Map<String, List<String>> fpFqcns, Map<String, List<String>> fnFqcns,
                ReliabilityMetrics reliability) {
            this.toolMetrics = toolMetrics;
            this.agreement   = agreement;
            this.fpFqcns     = fpFqcns;
            this.fnFqcns     = fnFqcns;
            this.reliability = reliability;
        }
    }
}
