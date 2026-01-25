package org.example.export;

import java.util.List;
import org.example.baseline.CandidateDTO;

final class ExportTestData {
    private ExportTestData() {
    }

    static CandidateDTO sampleCandidateA() {
        return new CandidateDTO(
                "com.example.alpha.AlphaService",
                true,
                false,
                true,
                52,
                0.12,
                9,
                21,
                340,
                11,
                4,
                7,
                List.of("METHODS_PLUS_FIELDS>40", "DEPENDENCY_TYPES>5")
        );
    }

    static CandidateDTO sampleCandidateB() {
        return new CandidateDTO(
                "com.example.beta.BetaController",
                false,
                true,
                false,
                30,
                0.45,
                3,
                12,
                200,
                5,
                2,
                1,
                List.of()
        );
    }
}
