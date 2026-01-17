package org.example.golden;

import org.example.baseline.BaselineAnalyzer;
import org.example.baseline.BaselineThresholds;
import org.example.baseline.CandidateDTO;
import org.example.baseline.ClassMetrics;
import org.example.baseline.MetricsCalculator;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GoldenExampleBaselineTest {
    @Test
    public void baselineAnalyzerFlagsGodClassExampleOnly() throws Exception {
        Path projectRoot = Path.of("src", "test", "resources", "golden", "GoldenExample");
        BaselineAnalyzer analyzer = new BaselineAnalyzer();
        BaselineThresholds thresholds = new BaselineThresholds(40, 999);

        List<CandidateDTO> candidates = analyzer.analyze(projectRoot, thresholds);

        assertEquals(1, candidates.size());
        assertEquals("com.example.goldenexample.GodClassExample",
                candidates.get(0).getFullyQualifiedClassName());
    }

    @Test
    public void godClassExampleExceedsMethodAndFieldCounts() throws Exception {
        Path sourcePath = Path.of(
                "src", "test", "resources", "golden", "GoldenExample",
                "src", "main", "java", "com", "example", "goldenexample", "GodClassExample.java"
        );
        String source = Files.readString(sourcePath, StandardCharsets.UTF_8);

        MetricsCalculator calculator = new MetricsCalculator();
        ClassMetrics metrics = calculator.calculateFromSource(source);

        assertTrue(metrics.getMethodCount() >= 25);
        assertTrue(metrics.getFieldCount() >= 15);
    }
}
