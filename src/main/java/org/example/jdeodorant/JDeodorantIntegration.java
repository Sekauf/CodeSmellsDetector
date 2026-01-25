package org.example.jdeodorant;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import org.example.baseline.CandidateDTO;

public class JDeodorantIntegration {
    private static final Logger LOGGER = Logger.getLogger(JDeodorantIntegration.class.getName());

    private final JDeodorantImporter importer;
    private final JDeodorantHeadlessRunner headlessRunner;

    public JDeodorantIntegration() {
        this(new JDeodorantImporter(), null);
    }

    public JDeodorantIntegration(JDeodorantImporter importer, JDeodorantHeadlessRunner headlessRunner) {
        this.importer = Objects.requireNonNull(importer, "importer");
        this.headlessRunner = headlessRunner;
    }

    public List<CandidateDTO> getJDeodorantCandidates(ProjectConfig cfg) throws IOException, InterruptedException {
        Objects.requireNonNull(cfg, "cfg");
        LOGGER.info("JDeodorant candidate fetch started.");
        if (headlessRunner != null && cfg.isHeadlessEnabled()) {
            LOGGER.info("JDeodorant headless path used.");
            List<CandidateDTO> result = headlessRunner.run(cfg);
            LOGGER.info("JDeodorant candidate fetch finished. Candidates=" + result.size());
            return result;
        }

        String csvPath = cfg.getJdeodorantCsvPath();
        if (csvPath == null || csvPath.isBlank()) {
            LOGGER.warning("JDeodorant CSV path not set; returning empty list.");
            LOGGER.info("JDeodorant candidate fetch finished. Candidates=0");
            return List.of();
        }

        LOGGER.info("JDeodorant manual CSV path used.");
        List<CandidateDTO> result = importer.importJDeodorantCsv(csvPath);
        LOGGER.info("JDeodorant candidate fetch finished. Candidates=" + result.size());
        return result;
    }

    public interface JDeodorantHeadlessRunner {
        List<CandidateDTO> run(ProjectConfig cfg) throws IOException, InterruptedException;
    }
}
