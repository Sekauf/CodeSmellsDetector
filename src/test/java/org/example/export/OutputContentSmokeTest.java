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
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OutputContentSmokeTest {
    @TempDir
    Path tempDir;

    @Test
    public void csvAndJsonContainKnownClassAndFlags() throws IOException {
        ResultExporter exporter = new ResultExporter();
        List<CandidateDTO> input = List.of(ExportTestData.sampleCandidateA(), ExportTestData.sampleCandidateB());
        Path outDir = tempDir.resolve("content");

        Path csvPath = exporter.writeCsv(input, outDir);
        Path jsonPath = exporter.writeJson(input, outDir);

        List<String> csvLines = Files.readAllLines(csvPath, StandardCharsets.UTF_8);
        assertEquals(
                "fullyQualifiedClassName,baselineFlag,sonarFlag,jdeodorantFlag,wmc,tcc,atfd,cbo,loc,"
                        + "godClass,usedCboFallback,methodCount,fieldCount,dependencyTypeCount,reasons",
                csvLines.get(0)
        );
        assertTrue(csvLines.stream().anyMatch(line -> line.contains("com.example.alpha.AlphaService")));

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(Files.readString(jsonPath, StandardCharsets.UTF_8));
        JsonNode first = root.get("candidates").get(0);
        assertEquals("com.example.alpha.AlphaService", first.get("fullyQualifiedClassName").asText());
        assertTrue(first.get("baselineFlag").asBoolean());
    }
}
