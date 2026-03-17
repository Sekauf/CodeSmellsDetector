package org.example.sonar;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.example.baseline.CandidateDTO;
import org.junit.Assert;
import org.junit.Test;

public class SonarAnalyzerTest {
    @Test
    public void runSonarAndFetchResultsParsesIssues() throws Exception {
        SonarScannerRunner runner = new SonarScannerRunner() {
            @Override
            public SonarScanResult runScan(Path projectRoot, String projectKey, String sonarUrl, String token) {
                return new SonarScanResult(0, "OK", null);
            }
        };
        SonarIssuesClient issuesClient = new SonarIssuesClient() {
            @Override
            public List<SonarIssue> searchIssues(
                    String hostUrl,
                    String token,
                    String projectKey,
                    String ruleKey,
                    int pageSize
            ) throws IOException {
                String json = readFixture("sonar/issues_s6539_page1.json");
                return parseIssues(json);
            }

            private String readFixture(String name) throws IOException {
                return java.nio.file.Files.readString(
                        java.nio.file.Path.of("src/test/resources").resolve(name)
                );
            }
        };
        SonarHealthClient healthClient = new SonarHealthClient() {
            @Override
            public boolean isHealthy(String hostUrl, String token, java.time.Duration timeout) {
                return true;
            }
        };

        SonarAnalyzer analyzer = new SonarAnalyzer(
                runner,
                issuesClient,
                new SonarS6539Mapper(),
                healthClient
        );
        analyzer.fallbackWaitMillis = 0;

        List<CandidateDTO> candidates = analyzer.runSonarAndFetchResults("C:\\tmp\\demo-project");
        Assert.assertEquals(1, candidates.size());
        Assert.assertEquals("com.example.mini.one.BigGod", candidates.get(0).getFullyQualifiedClassName());
    }

    @Test
    public void runSonarAndFetchResultsUnreachableThrows() {
        SonarAnalyzer analyzer = new SonarAnalyzer(
                new SonarScannerRunner(),
                new SonarIssuesClient(),
                new SonarS6539Mapper(),
                new SonarHealthClient() {
                    @Override
                    public boolean isHealthy(String hostUrl, String token, java.time.Duration timeout) {
                        return false;
                    }
                }
        );

        try {
            analyzer.runSonarAndFetchResults("C:\\tmp\\demo-project");
            Assert.fail("Expected IOException for unreachable SonarQube.");
        } catch (IOException ex) {
            Assert.assertTrue(ex.getMessage().contains("unreachable"));
        } catch (InterruptedException ex) {
            Assert.fail("Unexpected interruption.");
        }
    }

    @Test
    public void runSonarAndFetchResultsNoFindingsReturnsEmpty() throws Exception {
        SonarScannerRunner runner = new SonarScannerRunner() {
            @Override
            public SonarScanResult runScan(Path projectRoot, String projectKey, String sonarUrl, String token) {
                return new SonarScanResult(0, "OK", null);
            }
        };
        SonarIssuesClient issuesClient = new SonarIssuesClient() {
            @Override
            public List<SonarIssue> searchIssues(
                    String hostUrl,
                    String token,
                    String projectKey,
                    String ruleKey,
                    int pageSize
            ) {
                return java.util.Collections.emptyList();
            }
        };
        SonarHealthClient healthClient = new SonarHealthClient() {
            @Override
            public boolean isHealthy(String hostUrl, String token, java.time.Duration timeout) {
                return true;
            }
        };

        SonarAnalyzer analyzer = new SonarAnalyzer(
                runner,
                issuesClient,
                new SonarS6539Mapper(),
                healthClient
        );
        analyzer.fallbackWaitMillis = 0;

        List<CandidateDTO> candidates = analyzer.runSonarAndFetchResults("C:\\tmp\\demo-project");
        Assert.assertTrue(candidates.isEmpty());
    }
}
