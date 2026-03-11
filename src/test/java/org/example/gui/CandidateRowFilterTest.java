package org.example.gui;

import java.util.List;
import javax.swing.RowFilter;
import org.example.baseline.CandidateDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CandidateRowFilterTest {

    private CandidateTableModel model;

    // Candidates: baseline only, sonar only, jdeo only, baseline+sonar, all three, none
    private CandidateDTO baselineOnly;
    private CandidateDTO sonarOnly;
    private CandidateDTO jdeoOnly;
    private CandidateDTO baselineSonar;
    private CandidateDTO allThree;
    private CandidateDTO noFlags;

    @BeforeEach
    void setUp() {
        model = new CandidateTableModel();
        baselineOnly  = dto("com.example.BaselineOnly",  true,  false, false);
        sonarOnly     = dto("com.example.SonarOnly",     false, true,  false);
        jdeoOnly      = dto("com.example.JdeoOnly",      false, false, true);
        baselineSonar = dto("com.example.BaselineSonar", true,  true,  false);
        allThree      = dto("com.example.AllThree",      true,  true,  true);
        noFlags       = dto("com.example.Clean",         false, false, false);
        model.setData(List.of(baselineOnly, sonarOnly, jdeoOnly, baselineSonar, allThree, noFlags));
    }

    @Test
    void emptyFilterAcceptsAll() {
        CandidateRowFilter filter = noRestriction();
        assertFalse(filter.isActive());
        for (int i = 0; i < model.getRowCount(); i++) {
            assertTrue(include(filter, i), "row " + i + " should pass empty filter");
        }
    }

    @Test
    void textFilterMatchesFqcnSubstring() {
        CandidateRowFilter filter = new CandidateRowFilter(
                "Sonar", false, false, false, CandidateRowFilter.QuickMode.NONE);
        assertTrue(include(filter, indexOf(sonarOnly)));
        assertFalse(include(filter, indexOf(baselineOnly)));
    }

    @Test
    void textFilterIsCaseInsensitive() {
        CandidateRowFilter filter = new CandidateRowFilter(
                "sonar", false, false, false, CandidateRowFilter.QuickMode.NONE);
        assertTrue(include(filter, indexOf(sonarOnly)));
    }

    @Test
    void textFilterNoMatchExcludesRow() {
        CandidateRowFilter filter = new CandidateRowFilter(
                "zzz", false, false, false, CandidateRowFilter.QuickMode.NONE);
        for (int i = 0; i < model.getRowCount(); i++) {
            assertFalse(include(filter, i));
        }
    }

    @Test
    void requireBaselineExcludesNonBaseline() {
        CandidateRowFilter filter = new CandidateRowFilter(
                "", true, false, false, CandidateRowFilter.QuickMode.NONE);
        assertTrue(include(filter, indexOf(baselineOnly)));
        assertTrue(include(filter, indexOf(baselineSonar)));
        assertTrue(include(filter, indexOf(allThree)));
        assertFalse(include(filter, indexOf(sonarOnly)));
        assertFalse(include(filter, indexOf(jdeoOnly)));
        assertFalse(include(filter, indexOf(noFlags)));
    }

    @Test
    void requireBaselineAndSonarCombinesWithAnd() {
        CandidateRowFilter filter = new CandidateRowFilter(
                "", true, true, false, CandidateRowFilter.QuickMode.NONE);
        assertTrue(include(filter,  indexOf(baselineSonar)));
        assertTrue(include(filter,  indexOf(allThree)));
        assertFalse(include(filter, indexOf(baselineOnly)));
        assertFalse(include(filter, indexOf(sonarOnly)));
    }

    @Test
    void quickModeAllAgreeRequiresThreeFlags() {
        CandidateRowFilter filter = new CandidateRowFilter(
                "", false, false, false, CandidateRowFilter.QuickMode.ALL_AGREE);
        assertTrue(include(filter,  indexOf(allThree)));
        assertFalse(include(filter, indexOf(baselineSonar)));
        assertFalse(include(filter, indexOf(baselineOnly)));
        assertFalse(include(filter, indexOf(noFlags)));
    }

    @Test
    void quickModeOneToolRequiresExactlyOneFlag() {
        CandidateRowFilter filter = new CandidateRowFilter(
                "", false, false, false, CandidateRowFilter.QuickMode.ONE_TOOL);
        assertTrue(include(filter,  indexOf(baselineOnly)));
        assertTrue(include(filter,  indexOf(sonarOnly)));
        assertTrue(include(filter,  indexOf(jdeoOnly)));
        assertFalse(include(filter, indexOf(baselineSonar)));
        assertFalse(include(filter, indexOf(allThree)));
        assertFalse(include(filter, indexOf(noFlags)));
    }

    @Test
    void quickModeDisagreementRequiresTwoFlags() {
        CandidateRowFilter filter = new CandidateRowFilter(
                "", false, false, false, CandidateRowFilter.QuickMode.DISAGREEMENT);
        assertTrue(include(filter,  indexOf(baselineSonar)));
        assertFalse(include(filter, indexOf(baselineOnly)));
        assertFalse(include(filter, indexOf(allThree)));
    }

    @Test
    void isActiveReturnsFalseForEmptyFilter() {
        assertFalse(noRestriction().isActive());
    }

    @Test
    void isActiveReturnsTrueWhenTextSet() {
        assertTrue(new CandidateRowFilter("foo", false, false, false,
                CandidateRowFilter.QuickMode.NONE).isActive());
    }

    @Test
    void isActiveReturnsTrueWhenFlagRequired() {
        assertTrue(new CandidateRowFilter("", true, false, false,
                CandidateRowFilter.QuickMode.NONE).isActive());
    }

    @Test
    void isActiveReturnsTrueWhenQuickModeSet() {
        assertTrue(new CandidateRowFilter("", false, false, false,
                CandidateRowFilter.QuickMode.ALL_AGREE).isActive());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private boolean include(CandidateRowFilter filter, int modelRow) {
        RowFilter.Entry<CandidateTableModel, Integer> entry = new RowFilter.Entry<>() {
            @Override public CandidateTableModel getModel()      { return model; }
            @Override public int getValueCount()                 { return model.getColumnCount(); }
            @Override public Object getValue(int index)          { return model.getValueAt(modelRow, index); }
            @Override public String getStringValue(int index)    { return String.valueOf(getValue(index)); }
            @Override public Integer getIdentifier()             { return modelRow; }
        };
        return filter.include(entry);
    }

    private int indexOf(CandidateDTO dto) {
        for (int i = 0; i < model.getRowCount(); i++) {
            if (model.getRow(i) == dto) { return i; }
        }
        throw new IllegalArgumentException("DTO not found in model");
    }

    private CandidateRowFilter noRestriction() {
        return new CandidateRowFilter("", false, false, false, CandidateRowFilter.QuickMode.NONE);
    }

    private static CandidateDTO dto(String fqcn, boolean bl, boolean so, boolean jd) {
        return new CandidateDTO(fqcn, bl, so, jd, null, null, null, null, null, 0, 0, 0, List.of());
    }
}
