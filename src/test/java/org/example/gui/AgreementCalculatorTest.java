package org.example.gui;

import java.util.List;
import org.example.baseline.CandidateDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AgreementCalculatorTest {

    private static int counter = 0;

    private static CandidateDTO dto(boolean b, boolean s, boolean j) {
        return new CandidateDTO(
                "com.example.C" + counter++, b, s, j,
                null, null, null, null, null,
                0, 0, 0, List.of());
    }

    // -------------------------------------------------------------------------
    // Pairs + Jaccard
    // -------------------------------------------------------------------------

    @Test
    void emptyList_returnsZeroPairs() {
        AgreementCalculator calc = new AgreementCalculator();
        List<AgreementCalculator.PairStats> pairs = calc.computePairs(List.of());
        assertEquals(3, pairs.size());
        pairs.forEach(p -> {
            assertEquals(0, p.both());
            assertEquals(0, p.onlyA());
            assertEquals(0, p.onlyB());
            assertEquals(0.0, p.jaccard());
        });
    }

    @Test
    void nullList_returnsZeroPairs() {
        AgreementCalculator calc = new AgreementCalculator();
        List<AgreementCalculator.PairStats> pairs = calc.computePairs(null);
        pairs.forEach(p -> assertEquals(0.0, p.jaccard()));
    }

    @Test
    void pairStats_correctCounts() {
        // B+S=1, B only=1, S only=1, none=1
        List<CandidateDTO> list = List.of(
                dto(true,  true,  false),
                dto(true,  false, false),
                dto(false, true,  false),
                dto(false, false, false)
        );
        AgreementCalculator.PairStats bs = new AgreementCalculator().computePairs(list).get(0);
        assertEquals(1, bs.both());
        assertEquals(1, bs.onlyA());
        assertEquals(1, bs.onlyB());
        assertEquals(1.0 / 3.0, bs.jaccard(), 0.001);
    }

    @Test
    void jaccard_perfectAgreement() {
        List<CandidateDTO> list = List.of(dto(true, true, false), dto(true, true, false));
        AgreementCalculator.PairStats bs = new AgreementCalculator().computePairs(list).get(0);
        assertEquals(1.0, bs.jaccard(), 0.001);
    }

    @Test
    void jaccard_noAgreement() {
        // B only, no S at all — Jaccard = 0/(0+2+0) = 0
        List<CandidateDTO> list = List.of(dto(true, false, false), dto(true, false, false));
        AgreementCalculator.PairStats bs = new AgreementCalculator().computePairs(list).get(0);
        assertEquals(0.0, bs.jaccard(), 0.001);
    }

    // -------------------------------------------------------------------------
    // Venn counts
    // -------------------------------------------------------------------------

    @Test
    void venn_nullList_returnsZeros() {
        AgreementCalculator.VennCounts vc = new AgreementCalculator().computeVenn(null);
        assertEquals(0, vc.allThree());
        assertEquals(0, vc.onlyBaseline());
        assertEquals(0, vc.onlySonar());
        assertEquals(0, vc.onlyJdeo());
    }

    @Test
    void venn_allFlagsSet_countsAllThree() {
        AgreementCalculator.VennCounts vc = new AgreementCalculator()
                .computeVenn(List.of(dto(true, true, true)));
        assertEquals(1, vc.allThree());
        assertEquals(0, vc.onlyBaseline());
        assertEquals(0, vc.baselineSonarOnly());
    }

    @Test
    void venn_exclusiveRegions() {
        List<CandidateDTO> list = List.of(
                dto(true,  false, false),
                dto(false, true,  false),
                dto(false, false, true),
                dto(true,  true,  false),
                dto(true,  false, true),
                dto(false, true,  true),
                dto(true,  true,  true)
        );
        AgreementCalculator.VennCounts vc = new AgreementCalculator().computeVenn(list);
        assertEquals(1, vc.onlyBaseline());
        assertEquals(1, vc.onlySonar());
        assertEquals(1, vc.onlyJdeo());
        assertEquals(1, vc.baselineSonarOnly());
        assertEquals(1, vc.baselineJdeoOnly());
        assertEquals(1, vc.sonarJdeoOnly());
        assertEquals(1, vc.allThree());
    }

    @Test
    void venn_noneSet_allZero() {
        List<CandidateDTO> list = List.of(dto(false, false, false), dto(false, false, false));
        AgreementCalculator.VennCounts vc = new AgreementCalculator().computeVenn(list);
        assertEquals(0, vc.allThree());
        assertEquals(0, vc.onlyBaseline());
        assertEquals(0, vc.onlySonar());
        assertEquals(0, vc.onlyJdeo());
        assertEquals(0, vc.baselineSonarOnly());
        assertEquals(0, vc.baselineJdeoOnly());
        assertEquals(0, vc.sonarJdeoOnly());
    }
}
