package org.example.gui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.example.labeling.LabelDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class LabelPersistenceServiceTest {

    @TempDir
    Path tempDir;

    private final LabelPersistenceService service = new LabelPersistenceService();

    /** Save must create the target file. */
    @Test
    void saveCreatesFile() throws IOException {
        LabelDTO dto = new LabelDTO(
                "com.example.Foo", true, false, true, false, "note", LabelDTO.FinalLabel.GOD_CLASS);
        Path out = tempDir.resolve("labeling_input.csv");
        service.save(List.of(dto), out);
        assertTrue(Files.exists(out));
    }

    /** Round-trip: saved data must be fully recoverable after load. */
    @Test
    void saveAndLoadRoundtrip() throws IOException {
        LabelDTO dto = new LabelDTO(
                "com.example.Foo", true, false, true, false, "comment", LabelDTO.FinalLabel.GOD_CLASS);
        Path out = tempDir.resolve("labeling_input.csv");
        service.save(List.of(dto), out);

        Map<String, LabelDTO> loaded = service.load(out);
        assertTrue(loaded.containsKey("com.example.Foo"));
        LabelDTO result = loaded.get("com.example.Foo");
        assertEquals(Boolean.TRUE,  result.getK1());
        assertEquals(Boolean.FALSE, result.getK2());
        assertEquals(Boolean.TRUE,  result.getK3());
        assertEquals(Boolean.FALSE, result.getK4());
        assertEquals("comment", result.getComment());
        assertEquals(LabelDTO.FinalLabel.GOD_CLASS, result.getFinalLabel());
    }

    /** Saving an empty list must produce a header-only CSV. */
    @Test
    void saveEmptyListProducesHeaderOnly() throws IOException {
        Path out = tempDir.resolve("empty.csv");
        service.save(List.of(), out);
        assertTrue(Files.exists(out));
        Map<String, LabelDTO> loaded = service.load(out);
        assertTrue(loaded.isEmpty());
    }

    /** Save must create missing parent directories. */
    @Test
    void saveCreatesParentDirectories() throws IOException {
        Path out = tempDir.resolve("nested/subdir/labeling_input.csv");
        service.save(List.of(), out);
        assertTrue(Files.exists(out));
    }

    /** Multiple rows must all survive a round-trip. */
    @Test
    void saveAndLoadMultipleRows() throws IOException {
        LabelDTO a = new LabelDTO(
                "com.example.Alpha", true, true, true, true, "", LabelDTO.FinalLabel.GOD_CLASS);
        LabelDTO b = new LabelDTO(
                "com.example.Beta", false, false, false, false, "no reason", LabelDTO.FinalLabel.NO);
        Path out = tempDir.resolve("multi.csv");
        service.save(List.of(a, b), out);

        Map<String, LabelDTO> loaded = service.load(out);
        assertEquals(2, loaded.size());
        assertEquals(LabelDTO.FinalLabel.GOD_CLASS, loaded.get("com.example.Alpha").getFinalLabel());
        assertEquals(LabelDTO.FinalLabel.NO,        loaded.get("com.example.Beta").getFinalLabel());
    }

    /** A comment containing a comma must be quoted and load back correctly. */
    @Test
    void saveAndLoadCommentWithComma() throws IOException {
        LabelDTO dto = new LabelDTO(
                "com.example.Comma", false, false, false, false, "yes, indeed", LabelDTO.FinalLabel.NO);
        Path out = tempDir.resolve("comma.csv");
        service.save(List.of(dto), out);

        Map<String, LabelDTO> loaded = service.load(out);
        assertEquals("yes, indeed", loaded.get("com.example.Comma").getComment());
    }
}
