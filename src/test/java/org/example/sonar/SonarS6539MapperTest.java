package org.example.sonar;

import java.nio.file.Path;
import java.util.List;
import org.example.baseline.CandidateDTO;
import org.junit.Assert;
import org.junit.Test;

public class SonarS6539MapperTest {
    @Test
    public void mapsFileComponentToFqn() throws Exception {
        SonarS6539Mapper mapper = new SonarS6539Mapper();
        List<SonarIssue> issues = List.of(
                new SonarIssue("demo:src/main/java/com/example/mini/one/BigGod.java", "java:S6539", null, 12),
                new SonarIssue("demo:src/main/java/com/example/mini/two/nested/DeepThing.java", "java:S6539", null, 3),
                new SonarIssue("demo:src/main/java/com/example/mini/one/BigGod.java", "java:S6539", null, 8),
                new SonarIssue("demo:com.example.mini.one.BigGod", "java:S6539", null, null),
                new SonarIssue("demo:src/main/java/com/example/mini/odd/NoExt", "java:S6539", null, 1)
        );

        List<CandidateDTO> candidates = mapper.mapIssues(Path.of("C:\\tmp\\project"), issues, "demo");

        Assert.assertEquals(2, candidates.size());
        Assert.assertEquals("com.example.mini.one.BigGod", candidates.get(0).getFullyQualifiedClassName());
        Assert.assertEquals("com.example.mini.two.nested.DeepThing", candidates.get(1).getFullyQualifiedClassName());
        Assert.assertTrue(candidates.get(0).isSonarFlag());
        Assert.assertTrue(candidates.get(1).isSonarFlag());
    }
}
