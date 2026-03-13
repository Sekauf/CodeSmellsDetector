package org.example.gui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.example.baseline.CandidateDTO;
import org.example.export.ResultExporter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class ExportServiceTest {

    private static final String PROJECT = "myproj";

    private static CandidateDTO dto(String fqcn) {
        return new CandidateDTO(fqcn, false, false, false,
                null, null, null, null, null, 0, 0, 0, List.of());
    }

    /** Creates and returns a named output directory inside {@code base}. */
    private static Path outputDir(Path base) throws IOException {
        Path dir = base.resolve(PROJECT);
        Files.createDirectories(dir);
        return dir;
    }

    // -------------------------------------------------------------------------
    // findExisting
    // -------------------------------------------------------------------------

    @Test
    void findExisting_emptyDir_returnsEmpty(@TempDir Path dir) throws IOException {
        List<Path> existing = new ExportService().findExisting(outputDir(dir), true, true, true);
        assertTrue(existing.isEmpty());
    }

    @Test
    void findExisting_csvFilePresent_detected(@TempDir Path dir) throws IOException {
        Path out = outputDir(dir);
        Files.writeString(out.resolve(ResultExporter.csvFileName(PROJECT)), "header\n");
        List<Path> existing = new ExportService().findExisting(out, true, false, false);
        assertEquals(1, existing.size());
        assertEquals(ResultExporter.csvFileName(PROJECT), existing.get(0).getFileName().toString());
    }

    @Test
    void findExisting_fileExistsButNotSelected_notReported(@TempDir Path dir) throws IOException {
        Path out = outputDir(dir);
        Files.writeString(out.resolve(ResultExporter.csvFileName(PROJECT)), "x");
        // csv=false → not selected, so should not appear
        List<Path> existing = new ExportService().findExisting(out, false, true, false);
        assertTrue(existing.isEmpty());
    }

    @Test
    void findExisting_multiplePresent_allReported(@TempDir Path dir) throws IOException {
        Path out = outputDir(dir);
        Files.writeString(out.resolve(ResultExporter.csvFileName(PROJECT)),  "x");
        Files.writeString(out.resolve(ResultExporter.jsonFileName(PROJECT)), "x");
        List<Path> existing = new ExportService().findExisting(out, true, true, true);
        assertEquals(2, existing.size());
    }

    // -------------------------------------------------------------------------
    // runExport
    // -------------------------------------------------------------------------

    @Test
    void runExport_csv_writesResultsCsv(@TempDir Path dir) throws IOException {
        Path out = outputDir(dir);
        List<Path> written = new ExportService().runExport(out, List.of(dto("com.A")), true, false, false);
        assertEquals(1, written.size());
        assertTrue(Files.exists(out.resolve(ResultExporter.csvFileName(PROJECT))));
    }

    @Test
    void runExport_json_writesResultsJson(@TempDir Path dir) throws IOException {
        Path out = outputDir(dir);
        List<Path> written = new ExportService().runExport(out, List.of(dto("com.B")), false, true, false);
        assertEquals(1, written.size());
        assertTrue(Files.exists(out.resolve(ResultExporter.jsonFileName(PROJECT))));
    }

    @Test
    void runExport_labeling_writesLabelingCsv(@TempDir Path dir) throws IOException {
        Path out = outputDir(dir);
        List<Path> written = new ExportService().runExport(out, List.of(dto("com.C")), false, false, true);
        assertEquals(1, written.size());
        assertTrue(Files.exists(out.resolve(ResultExporter.labelingFileName(PROJECT))));
    }

    @Test
    void runExport_allSelected_writesThreeFiles(@TempDir Path dir) throws IOException {
        Path out = outputDir(dir);
        List<CandidateDTO> candidates = List.of(dto("com.X"), dto("com.Y"));
        List<Path> written = new ExportService().runExport(out, candidates, true, true, true);
        assertEquals(3, written.size());
    }

    @Test
    void runExport_noneSelected_writesNothing(@TempDir Path dir) throws IOException {
        Path out = outputDir(dir);
        List<Path> written = new ExportService().runExport(out, List.of(), false, false, false);
        assertTrue(written.isEmpty());
    }
}
