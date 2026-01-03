package org.example.baseline;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class CandidateRoundtripTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void exportImport_roundtrip_preservesAllFields() throws IOException {
        List<CandidateDTO> original = new ArrayList<>();
        original.add(CandidateDTO.builder("com.example.FlagsOnly")
                .baselineFlag(true)
                .sonarFlag(false)
                .jdeodorantFlag(true)
                .build());
        original.add(CandidateDTO.builder("com.example.SomeMetrics")
                .wmc(12)
                .tcc(0.5)
                .atfd(3)
                .build());
        original.add(new CandidateDTO(
                "com.example.AllMetrics",
                false,
                true,
                false,
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

        Path outputDir = temporaryFolder.newFolder("roundtrip").toPath();
        Path csvPath = outputDir.resolve("candidates.csv");
        BaselineCandidateExporter exporter = new BaselineCandidateExporter();
        exporter.exportToCsv(csvPath, original);

        BaselineCandidateImporter importer = new BaselineCandidateImporter();
        List<CandidateDTO> imported = importer.importFromCsv(csvPath);

        List<CandidateDTO> sortedOriginal = new ArrayList<>(original);
        Comparator<CandidateDTO> byFqn = Comparator.comparing(
                CandidateDTO::getFullyQualifiedClassName,
                Comparator.nullsFirst(String::compareTo)
        );
        sortedOriginal.sort(byFqn);
        imported.sort(byFqn);

        assertEquals(sortedOriginal, imported);
    }

    @Test
    public void exportImport_roundtrip_escapesCommaAndQuotes() throws IOException {
        List<CandidateDTO> original = new ArrayList<>();
        original.add(CandidateDTO.builder("com.example.Foo,Bar").baselineFlag(true).build());
        original.add(CandidateDTO.builder("com.example.\"Quoted\"").sonarFlag(true).build());

        Path outputDir = temporaryFolder.newFolder("escape").toPath();
        Path csvPath = outputDir.resolve("candidates.csv");
        BaselineCandidateExporter exporter = new BaselineCandidateExporter();
        exporter.exportToCsv(csvPath, original);

        BaselineCandidateImporter importer = new BaselineCandidateImporter();
        List<CandidateDTO> imported = importer.importFromCsv(csvPath);

        List<CandidateDTO> sortedOriginal = new ArrayList<>(original);
        Comparator<CandidateDTO> byFqn = Comparator.comparing(
                CandidateDTO::getFullyQualifiedClassName,
                Comparator.nullsFirst(String::compareTo)
        );
        sortedOriginal.sort(byFqn);
        imported.sort(byFqn);

        assertEquals(sortedOriginal, imported);
    }
}
