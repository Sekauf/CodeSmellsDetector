package org.example.gui;

import java.util.List;
import javax.swing.table.AbstractTableModel;
import org.example.baseline.CandidateDTO;

/**
 * Table model backed by a list of {@link CandidateDTO} instances.
 * Columns: FQCN, Baseline, SonarQube, JDeodorant, WMC, TCC, ATFD/CBO, LOC, Methods, Fields, DepTypes.
 */
public class CandidateTableModel extends AbstractTableModel {

    private static final String[] COLUMNS = {
        "Class Name", "Baseline", "SonarQube", "JDeodorant",
        "WMC", "TCC", "ATFD/CBO", "LOC", "Methods", "Fields", "DepTypes"
    };

    static final int COL_FQCN     = 0;
    static final int COL_BASELINE = 1;
    static final int COL_SONAR    = 2;
    static final int COL_JDEO     = 3;
    static final int COL_WMC      = 4;
    static final int COL_TCC      = 5;
    static final int COL_ATFD_CBO = 6;
    static final int COL_LOC      = 7;
    static final int COL_METHODS  = 8;
    static final int COL_FIELDS   = 9;
    static final int COL_DEPTYPES = 10;

    private List<CandidateDTO> data = List.of();

    /**
     * Replaces all rows with the given candidate list and fires a table-data-changed event.
     *
     * @param candidates candidates to display; {@code null} clears the table
     */
    public void setData(List<CandidateDTO> candidates) {
        this.data = (candidates == null) ? List.of() : List.copyOf(candidates);
        fireTableDataChanged();
    }

    /**
     * Returns the {@link CandidateDTO} at the given model row index.
     *
     * @param modelRow model row (not view row)
     * @return the candidate DTO
     */
    public CandidateDTO getRow(int modelRow) {
        return data.get(modelRow);
    }

    @Override
    public int getRowCount() {
        return data.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int col) {
        return COLUMNS[col];
    }

    @Override
    public Class<?> getColumnClass(int col) {
        return switch (col) {
            case COL_BASELINE, COL_SONAR, COL_JDEO -> Boolean.class;
            case COL_WMC, COL_ATFD_CBO, COL_LOC, COL_METHODS, COL_FIELDS, COL_DEPTYPES -> Integer.class;
            case COL_TCC -> Double.class;
            default -> String.class;
        };
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        return false;
    }

    @Override
    public Object getValueAt(int row, int col) {
        CandidateDTO dto = data.get(row);
        return switch (col) {
            case COL_FQCN     -> dto.getFullyQualifiedClassName();
            case COL_BASELINE -> dto.isBaselineFlag();
            case COL_SONAR    -> dto.isSonarFlag();
            case COL_JDEO     -> dto.isJdeodorantFlag();
            case COL_WMC      -> dto.getWmcNullable();
            case COL_TCC      -> dto.getTccNullable();
            case COL_ATFD_CBO -> dto.isUsedCboFallback() ? dto.getCboNullable() : dto.getAtfdNullable();
            case COL_LOC      -> dto.getLocNullable();
            case COL_METHODS  -> dto.getMethodCount();
            case COL_FIELDS   -> dto.getFieldCount();
            case COL_DEPTYPES -> dto.getDependencyTypeCount();
            default -> "";
        };
    }
}
