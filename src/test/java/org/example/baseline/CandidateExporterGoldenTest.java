package org.example.baseline;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class CandidateExporterGoldenTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void export_matchesGoldenCsv() throws IOException {
        List<CandidateDTO> candidates = new ArrayList<>();
        candidates.add(new CandidateDTO(
                "com.example.Beta",
                true,
                false,
                true,
                47,
                0.25,
                6,
                21,
                200,
                10,
                5,
                8,
                List.of("METHODS_PLUS_FIELDS>40", "DEPENDENCY_TYPES>5")
        ));
        candidates.add(new CandidateDTO(
                "com.example.Alpha",
                false,
                true,
                false,
                null,
                null,
                null,
                null,
                null,
                1,
                0,
                0,
                List.of()
        ));
        candidates.add(new CandidateDTO(
                "com.example.Gamma",
                false,
                false,
                false,
                12,
                0.9,
                1,
                2,
                50,
                2,
                1,
                1,
                List.of("DEPENDENCY_TYPES>5")
        ));

        Path outputDir = temporaryFolder.newFolder("golden").toPath();
        Path actualPath = outputDir.resolve("actual.csv");
        BaselineCandidateExporter exporter = new BaselineCandidateExporter();
        exporter.exportToCsv(actualPath, candidates);

        Path expectedPath = Path.of("src", "test", "resources", "expected", "candidates_expected.csv");
        String expected = Files.readString(expectedPath, StandardCharsets.UTF_8);
        String actual = Files.readString(actualPath, StandardCharsets.UTF_8);

        assertEquals(expected, actual);
    }
}
