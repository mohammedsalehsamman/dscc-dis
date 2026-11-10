package dscc.ui;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Table model backed by a list of objects; each column is defined by a name, a type and a getter.
 * <pre>
 * new BeanTableModel&lt;Employee&gt;()
 *     .column("ID", Integer.class, Employee::getId)
 *     .column("Name", String.class, Employee::getName);
 * </pre>
 */
public class BeanTableModel<T> extends AbstractTableModel {

    private final List<String> names = new ArrayList<>();
    private final List<Class<?>> types = new ArrayList<>();
    private final List<Function<T, ?>> getters = new ArrayList<>();
    private List<T> rows = new ArrayList<>();

    public BeanTableModel<T> column(String name, Class<?> type, Function<T, ?> getter) {
        names.add(name);
        types.add(type);
        getters.add(getter);
        return this;
    }

    public void setRows(List<T> rows) {
        this.rows = rows == null ? new ArrayList<>() : new ArrayList<>(rows);
        fireTableDataChanged();
    }

    public List<T> getRows() {
        return rows;
    }

    public T getRow(int modelIndex) {
        return modelIndex >= 0 && modelIndex < rows.size() ? rows.get(modelIndex) : null;
    }

    public void addRow(T row) {
        rows.add(row);
        fireTableRowsInserted(rows.size() - 1, rows.size() - 1);
    }

    public void insertRow(int index, T row) {
        rows.add(index, row);
        fireTableRowsInserted(index, index);
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return names.size();
    }

    @Override
    public String getColumnName(int column) {
        return names.get(column);
    }

    @Override
    public Class<?> getColumnClass(int column) {
        return types.get(column);
    }

    @Override
    public Object getValueAt(int row, int column) {
        return getters.get(column).apply(rows.get(row));
    }
}
