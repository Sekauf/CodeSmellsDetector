package org.example.gui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.example.baseline.CandidateDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class ExportServiceTest {

    private static CandidateDTO dto(String fqcn) {
        return new CandidateDTO(fqcn, false, false, false,
                null, null, null, null, null, 0, 0, 0, List.of());
    }

    // -------------------------------------------------------------------------
    // findExisting
    // -------------------------------------------------------------------------

    @Test
    void findExisting_emptyDir_returnsEmpty(@TempDir Path dir) {
        List<Path> existing = new ExportService().findExisting(dir, true, true, true);
        assertTrue(existing.isEmpty());
    }

    @Test
    void findExisting_csvFilePresent_detected(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve(ExportService.FILE_CSV), "header\n");
        List<Path> existing = new ExportService().findExisting(dir, true, false, false);
        assertEquals(1, existing.size());
        assertEquals(ExportService.FILE_CSV, existing.get(0).getFileName().toString());
    }

    @Test
    void findExisting_fileExistsButNotSelected_notReported(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve(ExportService.FILE_CSV), "x");
        // csv=false → not selected, so should not appear
        List<Path> existing = new ExportService().findExisting(dir, false, true, false);
        assertTrue(existing.isEmpty());
    }

    @Test
    void findExisting_multiplePresent_allReported(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve(ExportService.FILE_CSV),  "x");
        Files.writeString(dir.resolve(ExportService.FILE_JSON), "x");
        List<Path> existing = new ExportService().findExisting(dir, true, true, true);
        assertEquals(2, existing.size());
    }

    // -------------------------------------------------------------------------
    // runExport
    // -------------------------------------------------------------------------

    @Test
    void runExport_csv_writesResultsCsv(@TempDir Path dir) throws IOException {
        List<Path> written = new ExportService().runExport(dir, List.of(dto("com.A")), true, false, false);
        assertEquals(1, written.size());
        assertTrue(Files.exists(dir.resolve(ExportService.FILE_CSV)));
    }

    @Test
    void runExport_json_writesResultsJson(@TempDir Path dir) throws IOException {
        List<Path> written = new ExportService().runExport(dir, List.of(dto("com.B")), false, true, false);
        assertEquals(1, written.size());
        assertTrue(Files.exists(dir.resolve(ExportService.FILE_JSON)));
    }

    @Test
    void runExport_labeling_writesLabelingCsv(@TempDir Path dir) throws IOException {
        List<Path> written = new ExportService().runExport(dir, List.of(dto("com.C")), false, false, true);
        assertEquals(1, written.size());
        assertTrue(Files.exists(dir.resolve(ExportService.FILE_LABELING)));
    }

    @Test
    void runExport_allSelected_writesThreeFiles(@TempDir Path dir) throws IOException {
        List<CandidateDTO> candidates = List.of(dto("com.X"), dto("com.Y"));
        List<Path> written = new ExportService().runExport(dir, candidates, true, true, true);
        assertEquals(3, written.size());
    }

    @Test
    void runExport_noneSelected_writesNothing(@TempDir Path dir) throws IOException {
        List<Path> written = new ExportService().runExport(dir, List.of(), false, false, false);
        assertTrue(written.isEmpty());
    }
}
