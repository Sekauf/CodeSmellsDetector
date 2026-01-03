package org.example.baseline;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class BaselineAnalyzerTest {
    @Test
    public void exportsDeterministicCsvMatchingGoldenFile() throws IOException {
        // Thresholds: size>40 to flag the God Class; dependency>5 to catch the coupled small class.
        BaselineThresholds thresholds = new BaselineThresholds(40, 5);
        Path projectRoot = Path.of("src", "test", "resources", "mini-project");

        BaselineAnalyzer analyzer = new BaselineAnalyzer();
        List<CandidateDTO> candidates = analyzer.analyze(projectRoot, thresholds);

        BaselineCandidateExporter exporter = new BaselineCandidateExporter();
        Path actualPath = Path.of("target", "test-output", "actual_candidates.csv");
        exporter.exportToCsv(candidates, actualPath);

        Path expectedPath = Path.of("src", "test", "resources", "expected", "expected_candidates.csv");
        String actual = Files.readString(actualPath, StandardCharsets.UTF_8);
        String expected = Files.readString(expectedPath, StandardCharsets.UTF_8);

        assertEquals(expected, actual);
    }
}
