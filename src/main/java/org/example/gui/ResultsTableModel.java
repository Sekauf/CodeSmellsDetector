package org.example.gui;

import java.util.List;
import javax.swing.table.AbstractTableModel;

/**
 * Table model backed by rows parsed from {@code results.csv}.
 * Columns 1-3 (Baseline, Sonar, JDeodorant) return {@code Boolean.class}
 * so that {@link javax.swing.JTable} renders them as checkboxes.
 */
public class ResultsTableModel extends AbstractTableModel {

    private static final String[] COLUMNS = {
        "Class Name", "Baseline", "Sonar", "JDeodorant", "Methods", "Fields", "DepTypes", "Reasons"
    };

    // results.csv column indices
    private static final int COL_FQN = 0;
    private static final int COL_BASELINE = 1;
    private static final int COL_SONAR = 2;
    private static final int COL_JDEO = 3;
    private static final int COL_METHODS = 11;
    private static final int COL_FIELDS = 12;
    private static final int COL_DEPTYPES = 13;
    private static final int COL_REASONS = 14;

    private final List<String[]> rows;

    /** @param rows each element is a parsed CSV row (array of field values) */
    public ResultsTableModel(List<String[]> rows) {
        this.rows = rows;
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNS[column];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return (columnIndex >= 1 && columnIndex <= 3) ? Boolean.class : String.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        String[] row = rows.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> safeGet(row, COL_FQN);
            case 1 -> parseBoolean(safeGet(row, COL_BASELINE));
            case 2 -> parseBoolean(safeGet(row, COL_SONAR));
            case 3 -> parseBoolean(safeGet(row, COL_JDEO));
            case 4 -> safeGet(row, COL_METHODS);
            case 5 -> safeGet(row, COL_FIELDS);
            case 6 -> safeGet(row, COL_DEPTYPES);
            case 7 -> safeGet(row, COL_REASONS);
            default -> "";
        };
    }

    private static String safeGet(String[] row, int index) {
        if (row == null || index >= row.length) {
            return "";
        }
        return row[index] == null ? "" : row[index];
    }

    private static Boolean parseBoolean(String value) {
        return "true".equalsIgnoreCase(value);
    }
}
