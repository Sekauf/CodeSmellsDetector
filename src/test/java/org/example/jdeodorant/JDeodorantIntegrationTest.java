package org.example.jdeodorant;

import org.example.baseline.CandidateDTO;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class JDeodorantIntegrationTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void getJDeodorantCandidates_usesManualCsvPath() throws IOException, InterruptedException {
        Path dir = temporaryFolder.newFolder("jdeo-integration").toPath();
        Path csvPath = dir.resolve("jdeo.csv");

        StringBuilder builder = new StringBuilder();
        builder.append("Class,Smell\n");
        builder.append("com.example.Integration,God Class\n");
        builder.append("com.example.Skip,Feature Envy\n");
        Files.writeString(csvPath, builder.toString(), StandardCharsets.UTF_8);

        ProjectConfig cfg = ProjectConfig.forJdeodorantCsv(csvPath.toString());
        JDeodorantIntegration integration = new JDeodorantIntegration();

        List<CandidateDTO> result = integration.getJDeodorantCandidates(cfg);

        assertEquals(1, result.size());
        assertEquals("com.example.Integration", result.get(0).getFullyQualifiedClassName());
        assertEquals(true, result.get(0).isJdeodorantFlag());
    }
}
