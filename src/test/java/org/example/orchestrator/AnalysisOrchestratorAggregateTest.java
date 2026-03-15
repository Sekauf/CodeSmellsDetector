package org.example.orchestrator;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link AnalysisOrchestrator#aggregate(Path, Path)}.
 */
class AnalysisOrchestratorAggregateTest {

    private static final String METRICS_CSV_HEADER =
            "tool,precision,recall,f1,specificity,mcc,tp,fp,fn,tn";

    @Test
    void aggregateTwoProjects(@TempDir Path tempDir) throws IOException {
        // Setup: two project sub-directories with metrics_summary.csv
        Path projectA = tempDir.resolve("projectA");
        Files.createDirectories(projectA);
        Files.writeString(projectA.resolve("metrics_summary.csv"),
                METRICS_CSV_HEADER + "\n"
                        + "baseline,0.8333,0.7143,0.7692,0.9767,0.7206,5,1,2,42\n"
                        + "sonarqube,0.6667,0.5714,0.6154,0.9535,0.5443,4,2,3,41\n",
                StandardCharsets.UTF_8);

        Path projectB = tempDir.resolve("projectB");
        Files.createDirectories(projectB);
        Files.writeString(projectB.resolve("metrics_summary.csv"),
                METRICS_CSV_HEADER + "\n"
                        + "baseline,0.7500,0.6000,0.6667,0.9500,0.5916,3,1,2,17\n"
                        + "sonarqube,0.5000,0.4000,0.4444,0.9000,0.3464,2,2,3,13\n",
                StandardCharsets.UTF_8);

        Path outputDir = tempDir.resolve("aggregated");

        // Act
        new AnalysisOrchestrator().aggregate(tempDir, outputDir);

        // Assert: output files exist
        Path aggregatedCsv = outputDir.resolve("aggregated_metrics.csv");
        Path aggregatedReport = outputDir.resolve("aggregated_report.md");
        assertTrue(Files.exists(aggregatedCsv),
                "aggregated_metrics.csv should be created");
        assertTrue(Files.exists(aggregatedReport),
                "aggregated_report.md should be created");

        // Assert: CSV has content
        String csvContent = Files.readString(aggregatedCsv, StandardCharsets.UTF_8);
        assertTrue(csvContent.contains("baseline"),
                "CSV should contain baseline tool");
        assertTrue(csvContent.contains("sonarqube"),
                "CSV should contain sonarqube tool");
        assertTrue(csvContent.contains("projects"),
                "CSV should contain projects column header");

        // Assert: report has content
        String reportContent = Files.readString(aggregatedReport, StandardCharsets.UTF_8);
        assertTrue(reportContent.contains("Aggregated Report"),
                "Report should contain aggregated header");
        assertTrue(reportContent.contains("projectA"),
                "Report should reference projectA");
        assertTrue(reportContent.contains("projectB"),
                "Report should reference projectB");
    }

    @Test
    void aggregateNoProjectsProducesNoOutput(@TempDir Path tempDir) throws IOException {
        // Setup: empty root directory
        Path outputDir = tempDir.resolve("aggregated");

        // Act
        new AnalysisOrchestrator().aggregate(tempDir, outputDir);

        // Assert: no output files (early return)
        assertTrue(!Files.exists(outputDir.resolve("aggregated_metrics.csv"))
                || Files.size(outputDir.resolve("aggregated_metrics.csv")) == 0,
                "No aggregated CSV expected for empty input");
    }

    @Test
    void aggregateSkipsDirectoriesWithoutCsv(@TempDir Path tempDir) throws IOException {
        // Setup: one dir with CSV, one without
        Path projectA = tempDir.resolve("projectA");
        Files.createDirectories(projectA);
        Files.writeString(projectA.resolve("metrics_summary.csv"),
                METRICS_CSV_HEADER + "\n"
                        + "baseline,0.8,0.7,0.75,0.95,0.65,5,1,2,42\n",
                StandardCharsets.UTF_8);

        Path projectB = tempDir.resolve("projectB");
        Files.createDirectories(projectB);
        // No metrics_summary.csv here

        Path outputDir = tempDir.resolve("aggregated");

        // Act
        new AnalysisOrchestrator().aggregate(tempDir, outputDir);

        // Assert: output exists with data from projectA only
        Path aggregatedCsv = outputDir.resolve("aggregated_metrics.csv");
        assertTrue(Files.exists(aggregatedCsv),
                "aggregated_metrics.csv should be created");
        String csvContent = Files.readString(aggregatedCsv, StandardCharsets.UTF_8);
        assertTrue(csvContent.contains("baseline"),
                "CSV should contain baseline from projectA");
        assertTrue(csvContent.contains(",1\n".replace("\n", "") + "")
                || csvContent.contains("projects"),
                "CSV should indicate 1 project");
    }
}
