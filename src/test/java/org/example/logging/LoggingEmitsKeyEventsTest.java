package org.example.logging;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.example.baseline.BaselineThresholds;
import org.example.baseline.CandidateDTO;
import org.example.export.ResultExporter;
import org.example.jdeodorant.ProjectConfig;
import org.example.orchestrator.AnalysisOrchestrator;
import org.example.sonar.SonarConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class LoggingEmitsKeyEventsTest {
    @TempDir
    Path tempDir;

    @Test
    public void orchestratorEmitsStartEndAndCandidateCount() throws Exception {
        Logger logger = Logger.getLogger(AnalysisOrchestrator.class.getName());
        InMemoryHandler handler = new InMemoryHandler();
        Level previousLevel = logger.getLevel();
        boolean previousUseParent = logger.getUseParentHandlers();

        logger.setUseParentHandlers(false);
        logger.setLevel(Level.INFO);
        handler.setLevel(Level.INFO);
        logger.addHandler(handler);
        try {
            AnalysisOrchestrator orchestrator = new AnalysisOrchestrator(
                    (project, thresholds) -> List.of(ExportTestCandidate.sample("A", true, false, false)),
                    (project, config) -> List.of(ExportTestCandidate.sample("B", false, true, false)),
                    config -> List.of(ExportTestCandidate.sample("C", false, false, true)),
                    new ResultExporter()
            );
            BaselineThresholds thresholds = new BaselineThresholds(40, 5);
            SonarConfig sonarConfig = SonarConfig.builder().projectKey("demo").build();
            ProjectConfig jdeodorantConfig = ProjectConfig.forJdeodorantCsv("dummy.csv");

            orchestrator.run(Path.of("src", "test", "resources", "mini-project"), thresholds, sonarConfig,
                    jdeodorantConfig, tempDir);
        } finally {
            logger.removeHandler(handler);
            logger.setUseParentHandlers(previousUseParent);
            logger.setLevel(previousLevel);
        }

        assertTrue(handler.containsMessage("Orchestrator started."));
        assertTrue(handler.containsMessage("BaselineAnalyzer started."));
        assertTrue(handler.containsMessage("BaselineAnalyzer finished. candidates=1"));
        assertTrue(handler.containsMessage("SonarAnalyzer started."));
        assertTrue(handler.containsMessage("SonarAnalyzer finished. candidates=1"));
        assertTrue(handler.containsMessage("JDeodorantIntegration started."));
        assertTrue(handler.containsMessage("JDeodorantIntegration finished. candidates=1"));
        assertTrue(handler.containsMessage("Orchestrator finished. candidates=3"));
    }

    private static class InMemoryHandler extends Handler {
        private final List<String> messages = new ArrayList<>();

        @Override
        public void publish(LogRecord record) {
            if (record != null) {
                messages.add(record.getMessage());
            }
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }

        boolean containsMessage(String fragment) {
            for (String message : messages) {
                if (message != null && message.contains(fragment)) {
                    return true;
                }
            }
            return false;
        }
    }

    private static final class ExportTestCandidate {
        private ExportTestCandidate() {
        }

        static CandidateDTO sample(String name, boolean baseline, boolean sonar, boolean jdeodorant) {
            return new CandidateDTO(
                    "com.example." + name,
                    baseline,
                    sonar,
                    jdeodorant,
                    10,
                    0.5,
                    1,
                    2,
                    100,
                    1,
                    1,
                    1,
                    List.of()
            );
        }
    }
}
