package org.example.baseline;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class BaselineCandidateImporterTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void roundTripImportExportMatchesGoldenFile() throws IOException {
        Path expectedPath = Path.of("src", "test", "resources", "expected", "expected_candidates.csv");
        BaselineCandidateImporter importer = new BaselineCandidateImporter();
        List<CandidateDTO> candidates = importer.importFromCsv(expectedPath);

        Path outputDir = temporaryFolder.newFolder("csv").toPath();
        Path actualPath = outputDir.resolve("actual_candidates.csv");
        BaselineCandidateExporter exporter = new BaselineCandidateExporter();
        exporter.exportToCsv(actualPath, candidates);

        String expected = Files.readString(expectedPath, StandardCharsets.UTF_8);
        String actual = Files.readString(actualPath, StandardCharsets.UTF_8);

        assertEquals(expected, actual);
    }

    @Test
    public void export_emptyList_writesHeaderOnly_and_importReturnsEmpty() throws IOException {
        Path outputDir = temporaryFolder.newFolder("empty").toPath();
        Path csvPath = outputDir.resolve("empty_candidates.csv");

        BaselineCandidateExporter exporter = new BaselineCandidateExporter();
        exporter.exportToCsv(csvPath, List.of());

        BaselineCandidateImporter importer = new BaselineCandidateImporter();
        List<CandidateDTO> imported = importer.importFromCsv(csvPath);

        assertEquals(0, imported.size());
        String header = Files.readString(csvPath, StandardCharsets.UTF_8).trim();
        assertEquals(CandidateCsvUtil.headerLine(), header);
    }

    @Test
    public void import_invalidNumber_reportsLineColumnAndType() throws IOException {
        Path outputDir = temporaryFolder.newFolder("invalid").toPath();
        Path csvPath = outputDir.resolve("invalid.csv");

        StringBuilder builder = new StringBuilder();
        builder.append(CandidateCsvUtil.headerLine()).append("\n");
        builder.append("com.example.Bad,false,false,false,notint,,,,,1,0,0,\n");
        Files.writeString(csvPath, builder.toString(), StandardCharsets.UTF_8);

        BaselineCandidateImporter importer = new BaselineCandidateImporter();
        try {
            importer.importFromCsv(csvPath);
        } catch (IllegalArgumentException ex) {
            assertEquals(true, ex.getMessage().contains("line 2"));
            assertEquals(true, ex.getMessage().contains("column 'wmc'"));
            assertEquals(true, ex.getMessage().contains("expected integer"));
            return;
        }
        throw new AssertionError("Expected IllegalArgumentException for invalid integer.");
    }

    @Test
    public void import_invalidBoolean_reportsLineColumnAndType() throws IOException {
        Path outputDir = temporaryFolder.newFolder("invalid-bool").toPath();
        Path csvPath = outputDir.resolve("invalid.csv");

        StringBuilder builder = new StringBuilder();
        builder.append(CandidateCsvUtil.headerLine()).append("\n");
        builder.append("com.example.Bad,yes,false,false,,,,,,1,0,0,\n");
        Files.writeString(csvPath, builder.toString(), StandardCharsets.UTF_8);

        BaselineCandidateImporter importer = new BaselineCandidateImporter();
        try {
            importer.importFromCsv(csvPath);
        } catch (IllegalArgumentException ex) {
            assertEquals(true, ex.getMessage().contains("line 2"));
            assertEquals(true, ex.getMessage().contains("column 'baselineFlag'"));
            assertEquals(true, ex.getMessage().contains("expected boolean"));
            return;
        }
        throw new AssertionError("Expected IllegalArgumentException for invalid boolean.");
    }
}
