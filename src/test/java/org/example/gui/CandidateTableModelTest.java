package org.example.gui;

import java.util.List;
import org.example.baseline.CandidateDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CandidateTableModelTest {

    private CandidateTableModel model;
    private CandidateDTO dto;

    @BeforeEach
    void setUp() {
        model = new CandidateTableModel();
        // fqcn, baseline, sonar, jdeo, wmc, tcc, atfd, cbo, loc, methods, fields, deptypes, reasons
        dto = new CandidateDTO(
                "com.example.Foo", true, false, true,
                10, 0.5, 3, null, 200,
                15, 5, 2, List.of("reason1"));
    }

    @Test
    void columnCount() {
        assertEquals(11, model.getColumnCount());
    }

    @Test
    void columnNames() {
        assertEquals("Class Name",   model.getColumnName(CandidateTableModel.COL_FQCN));
        assertEquals("Baseline",     model.getColumnName(CandidateTableModel.COL_BASELINE));
        assertEquals("SonarQube",    model.getColumnName(CandidateTableModel.COL_SONAR));
        assertEquals("JDeodorant",   model.getColumnName(CandidateTableModel.COL_JDEO));
    }

    @Test
    void columnClassForBooleans() {
        assertEquals(Boolean.class, model.getColumnClass(CandidateTableModel.COL_BASELINE));
        assertEquals(Boolean.class, model.getColumnClass(CandidateTableModel.COL_SONAR));
        assertEquals(Boolean.class, model.getColumnClass(CandidateTableModel.COL_JDEO));
    }

    @Test
    void columnClassForNumerics() {
        assertEquals(Integer.class, model.getColumnClass(CandidateTableModel.COL_WMC));
        assertEquals(Double.class,  model.getColumnClass(CandidateTableModel.COL_TCC));
        assertEquals(Integer.class, model.getColumnClass(CandidateTableModel.COL_METHODS));
    }

    @Test
    void initiallyEmpty() {
        assertEquals(0, model.getRowCount());
    }

    @Test
    void setDataUpdatesRowCount() {
        model.setData(List.of(dto));
        assertEquals(1, model.getRowCount());
    }

    @Test
    void getValueAtFqcn() {
        model.setData(List.of(dto));
        assertEquals("com.example.Foo", model.getValueAt(0, CandidateTableModel.COL_FQCN));
    }

    @Test
    void getValueAtFlags() {
        model.setData(List.of(dto));
        assertEquals(Boolean.TRUE,  model.getValueAt(0, CandidateTableModel.COL_BASELINE));
        assertEquals(Boolean.FALSE, model.getValueAt(0, CandidateTableModel.COL_SONAR));
        assertEquals(Boolean.TRUE,  model.getValueAt(0, CandidateTableModel.COL_JDEO));
    }

    @Test
    void getValueAtMetrics() {
        model.setData(List.of(dto));
        assertEquals(10,  model.getValueAt(0, CandidateTableModel.COL_WMC));
        assertEquals(0.5, model.getValueAt(0, CandidateTableModel.COL_TCC));
        assertEquals(15,  model.getValueAt(0, CandidateTableModel.COL_METHODS));
        assertEquals(5,   model.getValueAt(0, CandidateTableModel.COL_FIELDS));
        assertEquals(2,   model.getValueAt(0, CandidateTableModel.COL_DEPTYPES));
    }

    @Test
    void getRow() {
        model.setData(List.of(dto));
        assertSame(dto, model.getRow(0));
    }

    @Test
    void setDataWithNullClearsTable() {
        model.setData(List.of(dto));
        model.setData(null);
        assertEquals(0, model.getRowCount());
    }

    @Test
    void notEditable() {
        model.setData(List.of(dto));
        assertFalse(model.isCellEditable(0, 0));
    }

    @Test
    void rowColorForBaselineOnly() {
        CandidateDTO baselineOnly = new CandidateDTO(
                "com.example.Bar", true, false, false,
                null, null, null, null, null, 0, 0, 0, List.of());
        assertNotNull(RowColorRenderer.backgroundFor(baselineOnly));
        assertEquals(RowColorRenderer.GOD_CLASS_COLOR, RowColorRenderer.backgroundFor(baselineOnly));
    }

    @Test
    void rowColorForTwoFlags() {
        CandidateDTO twoFlags = new CandidateDTO(
                "com.example.Baz", true, true, false,
                null, null, null, null, null, 0, 0, 0, List.of());
        assertEquals(RowColorRenderer.TWO_TOOLS_COLOR, RowColorRenderer.backgroundFor(twoFlags));
    }

    @Test
    void rowColorForNoFlags() {
        CandidateDTO none = new CandidateDTO(
                "com.example.Clean", false, false, false,
                null, null, null, null, null, 0, 0, 0, List.of());
        assertNull(RowColorRenderer.backgroundFor(none));
    }
}
