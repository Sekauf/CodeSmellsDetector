package org.example.export;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.example.baseline.CandidateDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class ExportDeterminismTest {
    @TempDir
    Path tempDir;

    @Test
    public void exportIsDeterministicForSameInput() throws IOException {
        ResultExporter exporter = new ResultExporter();
        List<CandidateDTO> input = List.of(ExportTestData.sampleCandidateB(), ExportTestData.sampleCandidateA());
        Path outDir = tempDir.resolve("run");

        Path csvPath = exporter.writeCsv(input, outDir);
        Path jsonPath = exporter.writeJson(input, outDir);

        byte[] csvFirst = Files.readAllBytes(csvPath);
        byte[] jsonFirst = Files.readAllBytes(jsonPath);

        exporter.writeCsv(input, outDir);
        exporter.writeJson(input, outDir);

        assertArrayEquals(csvFirst, Files.readAllBytes(csvPath));
        assertArrayEquals(jsonFirst, Files.readAllBytes(jsonPath));
    }
}
