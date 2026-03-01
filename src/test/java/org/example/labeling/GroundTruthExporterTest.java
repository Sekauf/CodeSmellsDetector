package org.example.labeling;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.example.labeling.LabelDTO.FinalLabel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class GroundTruthExporterTest {

    @TempDir
    Path tempDir;

    private GroundTruthExporter exporter;
    private Map<String, LabelDTO> labels;

    @BeforeEach
    void setUp() throws IOException {
        exporter = new GroundTruthExporter();
        // Keys intentionally non-alphabetical to verify sorting
        labels = new LinkedHashMap<>();
        labels.put("org.example.ZClass",
                new LabelDTO("org.example.ZClass", true, true, true, false, "big class", FinalLabel.GOD_CLASS));
        labels.put("org.example.AClass",
                new LabelDTO("org.example.AClass", false, false, false, false, null, FinalLabel.NO));
        labels.put("org.example.MClass",
                new LabelDTO("org.example.MClass", true, true, false, null, null, FinalLabel.UNCERTAIN));
        exporter.export(labels, tempDir);
    }

    @Test
    void bothOutputFilesCreated() {
        assertTrue(Files.exists(tempDir.resolve("ground_truth.csv")), "CSV file should exist");
        assertTrue(Files.exists(tempDir.resolve("ground_truth.json")), "JSON file should exist");
    }

    @Test
    void csvHeaderIsCorrect() throws IOException {
        List<String> lines = readCsvLines();
        assertEquals("fullyQualifiedClassName,k1,k2,k3,k4,comment,finalLabel", lines.get(0));
    }

    @Test
    void csvRowsAreSortedAlphabetically() throws IOException {
        List<String> lines = readCsvLines();
        // lines: index 0 = header, 1 = AClass, 2 = MClass, 3 = ZClass
        assertTrue(lines.get(1).startsWith("org.example.AClass,"), "Row 1 should be AClass");
        assertTrue(lines.get(2).startsWith("org.example.MClass,"), "Row 2 should be MClass");
        assertTrue(lines.get(3).startsWith("org.example.ZClass,"), "Row 3 should be ZClass");
    }

    @Test
    void csvContainsCorrectData() throws IOException {
        List<String> lines = readCsvLines();
        // ZClass row: true,true,true,false,big class,GOD_CLASS
        String zRow = lines.get(3);
        assertTrue(zRow.contains("true,true,true,false"), "k1-k4 values should be correct");
        assertTrue(zRow.contains("GOD_CLASS"), "finalLabel should be GOD_CLASS");
        assertTrue(zRow.contains("big class"), "comment should be present");
    }

    @Test
    void jsonMetadataCountsAreCorrect() throws IOException {
        JsonNode root = readJson();
        JsonNode meta = root.get("metadata");
        assertEquals(3, meta.get("total").asInt());
        assertEquals(1, meta.get("godClassCount").asInt());
        assertEquals(1, meta.get("noCount").asInt());
        assertEquals(1, meta.get("uncertainCount").asInt());
    }

    @Test
    void jsonDateFieldExists() throws IOException {
        JsonNode root = readJson();
        String date = root.get("metadata").get("date").asText();
        assertNotNull(date);
        assertFalse(date.isEmpty(), "date field should not be empty");
    }

    @Test
    void jsonLabelsAreSortedAlphabetically() throws IOException {
        JsonNode root = readJson();
        JsonNode firstLabel = root.get("labels").get(0);
        assertEquals("org.example.AClass", firstLabel.get("fullyQualifiedClassName").asText());
    }

    private List<String> readCsvLines() throws IOException {
        Path csv = tempDir.resolve("ground_truth.csv");
        return Files.readAllLines(csv, StandardCharsets.UTF_8)
                .stream()
                .filter(l -> !l.isEmpty())
                .toList();
    }

    private JsonNode readJson() throws IOException {
        Path json = tempDir.resolve("ground_truth.json");
        return new ObjectMapper().readTree(json.toFile());
    }
}
