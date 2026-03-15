package org.example.orchestrator;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.example.export.ResultExporter;
import org.example.labeling.LabelDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the label-threshold feature in AnalysisOrchestrator (US-20).
 */
class AnalysisOrchestratorThresholdTest {

    private final AnalysisOrchestrator orchestrator = new AnalysisOrchestrator(
            (p, t) -> List.of(),
            (p, c) -> List.of(),
            c -> List.of(),
            new ResultExporter()
    );

    private Map<String, LabelDTO> buildLabels() {
        Map<String, LabelDTO> labels = new LinkedHashMap<>();
        // 4/4 true
        labels.put("com.AllTrue", new LabelDTO("com.AllTrue",
                true, true, true, true, null, LabelDTO.FinalLabel.GOD_CLASS));
        // 3/4 true
        labels.put("com.ThreeTrue", new LabelDTO("com.ThreeTrue",
                true, true, true, false, null, LabelDTO.FinalLabel.GOD_CLASS));
        // 2/4 true
        labels.put("com.TwoTrue", new LabelDTO("com.TwoTrue",
                true, true, false, false, null, LabelDTO.FinalLabel.UNCERTAIN));
        // 1/4 true
        labels.put("com.OneTrue", new LabelDTO("com.OneTrue",
                true, false, false, false, null, LabelDTO.FinalLabel.NO));
        // 0/4 true
        labels.put("com.NoneTrue", new LabelDTO("com.NoneTrue",
                false, false, false, false, null, LabelDTO.FinalLabel.NO));
        return labels;
    }

    @Test
    void thresholdFour_onlyAllTrueIsPositive() {
        Map<String, Boolean> truth = orchestrator.buildGroundTruth(buildLabels(), 4);
        assertEquals(5, truth.size());
        assertTrue(truth.get("com.AllTrue"));
        assertFalse(truth.get("com.ThreeTrue"));
        assertFalse(truth.get("com.TwoTrue"));
        assertFalse(truth.get("com.OneTrue"));
        assertFalse(truth.get("com.NoneTrue"));
    }

    @Test
    void thresholdThree_defaultBehavior() {
        Map<String, Boolean> truth = orchestrator.buildGroundTruth(buildLabels(), 3);
        assertTrue(truth.get("com.AllTrue"));
        assertTrue(truth.get("com.ThreeTrue"));
        assertFalse(truth.get("com.TwoTrue"));
        assertFalse(truth.get("com.OneTrue"));
        assertFalse(truth.get("com.NoneTrue"));
    }

    @Test
    void thresholdTwo_morePositives() {
        Map<String, Boolean> truth = orchestrator.buildGroundTruth(buildLabels(), 2);
        assertTrue(truth.get("com.AllTrue"));
        assertTrue(truth.get("com.ThreeTrue"));
        assertTrue(truth.get("com.TwoTrue"));
        assertFalse(truth.get("com.OneTrue"));
        assertFalse(truth.get("com.NoneTrue"));
    }

    @Test
    void thresholdOne_mostPermissive() {
        Map<String, Boolean> truth = orchestrator.buildGroundTruth(buildLabels(), 1);
        assertTrue(truth.get("com.AllTrue"));
        assertTrue(truth.get("com.ThreeTrue"));
        assertTrue(truth.get("com.TwoTrue"));
        assertTrue(truth.get("com.OneTrue"));
        assertFalse(truth.get("com.NoneTrue"));
    }

    @Test
    void invalidThresholdZero_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> orchestrator.evaluate(null, null, null, 0));
    }

    @Test
    void invalidThresholdFive_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> orchestrator.evaluate(null, null, null, 5));
    }

    @Test
    void nullCriteriaCountsAsFalse() {
        Map<String, LabelDTO> labels = new LinkedHashMap<>();
        labels.put("com.NullK", new LabelDTO("com.NullK",
                null, null, true, true, null, LabelDTO.FinalLabel.UNCERTAIN));
        Map<String, Boolean> truth = orchestrator.buildGroundTruth(labels, 2);
        assertTrue(truth.get("com.NullK"));
    }

    @Test
    void emptyLabels_returnsEmptyMap() {
        Map<String, Boolean> truth = orchestrator.buildGroundTruth(Map.of(), 3);
        assertTrue(truth.isEmpty());
    }
}
