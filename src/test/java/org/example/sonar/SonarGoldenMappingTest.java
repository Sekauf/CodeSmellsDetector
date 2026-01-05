package org.example.sonar;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.example.baseline.CandidateDTO;
import org.junit.Assert;
import org.junit.Test;

public class SonarGoldenMappingTest {
    @Test
    public void mapsGoldenFixtureDeterministically() throws Exception {
        String json = Files.readString(
                Path.of("src/test/resources/sonar/golden_issues.json"),
                StandardCharsets.UTF_8
        );
        SonarIssuesClient issuesClient = new SonarIssuesClient();
        List<SonarIssue> issues = issuesClient.parseIssues(json);

        SonarS6539Mapper mapper = new SonarS6539Mapper();
        List<CandidateDTO> candidates = mapper.mapIssues(
                Path.of("src/test/resources/golden/mini-project"),
                issues,
                "golden"
        );

        Assert.assertEquals(2, candidates.size());
        Assert.assertEquals("com.example.golden.one.BigGod", candidates.get(0).getFullyQualifiedClassName());
        Assert.assertEquals("com.example.golden.two.CoupledSmall", candidates.get(1).getFullyQualifiedClassName());
    }
}
