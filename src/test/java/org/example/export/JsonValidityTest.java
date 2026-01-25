package org.example.export;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.example.baseline.CandidateDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JsonValidityTest {
    @TempDir
    Path tempDir;

    @Test
    public void resultsJsonIsParseableAndHasExpectedFields() throws IOException {
        ResultExporter exporter = new ResultExporter();
        List<CandidateDTO> input = List.of(ExportTestData.sampleCandidateA());
        Path outDir = tempDir.resolve("json");

        Path jsonPath = exporter.writeJson(input, outDir);
        String json = Files.readString(jsonPath, StandardCharsets.UTF_8);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(json);
        JsonNode candidates = root.get("candidates");
        assertNotNull(candidates);
        assertTrue(candidates.isArray());
        assertEquals(1, candidates.size());

        JsonNode first = candidates.get(0);
        assertNotNull(first.get("fullyQualifiedClassName"));
        assertNotNull(first.get("baselineFlag"));
        assertNotNull(first.get("sonarFlag"));
        assertNotNull(first.get("jdeodorantFlag"));
        assertNotNull(first.get("wmc"));
        assertNotNull(first.get("tcc"));
        assertNotNull(first.get("atfd"));
        assertNotNull(first.get("cbo"));
        assertNotNull(first.get("loc"));
        assertNotNull(first.get("godClass"));
        assertNotNull(first.get("usedCboFallback"));
        assertNotNull(first.get("methodCount"));
        assertNotNull(first.get("fieldCount"));
        assertNotNull(first.get("dependencyTypeCount"));
        assertNotNull(first.get("reasons"));
    }
}
