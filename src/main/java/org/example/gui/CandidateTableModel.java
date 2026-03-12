package org.example.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.table.AbstractTableModel;
import org.example.baseline.CandidateDTO;
import org.example.labeling.LabelDTO;

/**
 * Table model backed by a list of {@link CandidateDTO} instances.
 * Columns 0–10: read-only candidate data.
 * Columns 11–16: editable labeling columns (K1–K4, Comment, FinalLabel).
 */
public class CandidateTableModel extends AbstractTableModel {

    private static final String[] COLUMNS = {
        "Class Name", "Baseline", "SonarQube", "JDeodorant",
        "WMC", "TCC", "ATFD/CBO", "LOC", "Methods", "Fields", "DepTypes",
        "K1", "K2", "K3", "K4", "Comment", "FinalLabel"
    };

    static final int COL_FQCN        = 0;
    static final int COL_BASELINE    = 1;
    static final int COL_SONAR       = 2;
    static final int COL_JDEO        = 3;
    static final int COL_WMC         = 4;
    static final int COL_TCC         = 5;
    static final int COL_ATFD_CBO    = 6;
    static final int COL_LOC         = 7;
    static final int COL_METHODS     = 8;
    static final int COL_FIELDS      = 9;
    static final int COL_DEPTYPES    = 10;
    static final int COL_K1          = 11;
    static final int COL_K2          = 12;
    static final int COL_K3          = 13;
    static final int COL_K4          = 14;
    static final int COL_COMMENT     = 15;
    static final int COL_FINAL_LABEL = 16;

    /** Mutable label state for a single candidate row. */
    private static final class LabelRow {
        boolean k1, k2, k3, k4;
        String comment    = "";
        String finalLabel = "";
    }

    private List<CandidateDTO>  data      = List.of();
    private ArrayList<LabelRow> labelRows = new ArrayList<>();
    private boolean             dirty     = false;

    /**
     * Replaces all rows with the given candidate list and fires a table-data-changed event.
     * Label columns are reset to their defaults.
     *
     * @param candidates candidates to display; {@code null} clears the table
     */
    public void setData(List<CandidateDTO> candidates) {
        this.data = (candidates == null) ? List.of() : List.copyOf(candidates);
        this.labelRows = new ArrayList<>(this.data.size());
        for (int i = 0; i < this.data.size(); i++) {
            labelRows.add(new LabelRow());
        }
        this.dirty = false;
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

    /**
     * Returns label DTOs for all rows, suitable for persistence.
     *
     * @return list of LabelDTOs, one per candidate row
     */
    public List<LabelDTO> getAllLabels() {
        List<LabelDTO> result = new ArrayList<>(data.size());
        for (int i = 0; i < data.size(); i++) {
            LabelRow lr = labelRows.get(i);
            LabelDTO.FinalLabel fl = parseFinalLabel(lr.finalLabel);
            result.add(new LabelDTO(data.get(i).getFullyQualifiedClassName(),
                    lr.k1, lr.k2, lr.k3, lr.k4, lr.comment, fl));
        }
        return result;
    }

    /**
     * Merges the given labels into the existing rows by FQCN.
     * Clears the dirty flag and fires a table-data-changed event.
     *
     * @param labels map from FQCN to LabelDTO
     */
    public void loadLabels(Map<String, LabelDTO> labels) {
        for (int i = 0; i < data.size(); i++) {
            LabelDTO dto = labels.get(data.get(i).getFullyQualifiedClassName());
            if (dto == null) { continue; }
            LabelRow lr = labelRows.get(i);
            lr.k1 = Boolean.TRUE.equals(dto.getK1());
            lr.k2 = Boolean.TRUE.equals(dto.getK2());
            lr.k3 = Boolean.TRUE.equals(dto.getK3());
            lr.k4 = Boolean.TRUE.equals(dto.getK4());
            lr.comment    = dto.getComment()    != null ? dto.getComment()            : "";
            lr.finalLabel = dto.getFinalLabel()  != null ? dto.getFinalLabel().name() : "";
        }
        dirty = false;
        fireTableDataChanged();
    }

    /**
     * Sets each row's finalLabel using {@link LabelDTO#deriveLabel(Boolean, Boolean, Boolean, Boolean)}.
     * Sets the dirty flag when any row exists.
     */
    public void applyAutoLabels() {
        for (LabelRow lr : labelRows) {
            lr.finalLabel = LabelDTO.deriveLabel(lr.k1, lr.k2, lr.k3, lr.k4).name();
        }
        dirty = !labelRows.isEmpty();
        fireTableDataChanged();
    }

    /** @return {@code true} if label data has been modified since the last save or load */
    public boolean isDirty() {
        return dirty;
    }

    /** Clears the dirty flag (called after a successful save). */
    public void clearDirty() {
        dirty = false;
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
            case COL_BASELINE, COL_SONAR, COL_JDEO,
                 COL_K1, COL_K2, COL_K3, COL_K4 -> Boolean.class;
            case COL_WMC, COL_ATFD_CBO, COL_LOC,
                 COL_METHODS, COL_FIELDS, COL_DEPTYPES -> Integer.class;
            case COL_TCC -> Double.class;
            default -> String.class;
        };
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        return col >= COL_K1;
    }

    @Override
    public Object getValueAt(int row, int col) {
        CandidateDTO dto = data.get(row);
        LabelRow     lr  = labelRows.get(row);
        return switch (col) {
            case COL_FQCN        -> dto.getFullyQualifiedClassName();
            case COL_BASELINE    -> dto.isBaselineFlag();
            case COL_SONAR       -> dto.isSonarFlag();
            case COL_JDEO        -> dto.isJdeodorantFlag();
            case COL_WMC         -> dto.getWmcNullable();
            case COL_TCC         -> dto.getTccNullable();
            case COL_ATFD_CBO    -> dto.isUsedCboFallback() ? dto.getCboNullable() : dto.getAtfdNullable();
            case COL_LOC         -> dto.getLocNullable();
            case COL_METHODS     -> dto.getMethodCount();
            case COL_FIELDS      -> dto.getFieldCount();
            case COL_DEPTYPES    -> dto.getDependencyTypeCount();
            case COL_K1          -> lr.k1;
            case COL_K2          -> lr.k2;
            case COL_K3          -> lr.k3;
            case COL_K4          -> lr.k4;
            case COL_COMMENT     -> lr.comment;
            case COL_FINAL_LABEL -> lr.finalLabel;
            default -> "";
        };
    }

    @Override
    public void setValueAt(Object value, int row, int col) {
        if (row < 0 || row >= data.size()) { return; }
        LabelRow lr = labelRows.get(row);
        switch (col) {
            case COL_K1          -> lr.k1 = Boolean.TRUE.equals(value);
            case COL_K2          -> lr.k2 = Boolean.TRUE.equals(value);
            case COL_K3          -> lr.k3 = Boolean.TRUE.equals(value);
            case COL_K4          -> lr.k4 = Boolean.TRUE.equals(value);
            case COL_COMMENT     -> lr.comment    = value != null ? value.toString() : "";
            case COL_FINAL_LABEL -> lr.finalLabel = value != null ? value.toString() : "";
            default -> { return; }
        }
        dirty = true;
        fireTableCellUpdated(row, col);
    }

    private LabelDTO.FinalLabel parseFinalLabel(String s) {
        if (s == null || s.isEmpty()) { return null; }
        try {
            return LabelDTO.FinalLabel.valueOf(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
