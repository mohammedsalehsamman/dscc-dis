package dscc.ui;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * A JTable with sorting by column header and instant search across all columns.
 * Extra filters (e.g. a department combo box) can be added with {@link #setExtraFilter}.
 */
public class SearchableTable extends JPanel {

    private final JTable table;
    private final TableRowSorter<TableModel> sorter;
    private final JTextField searchField = new JTextField(18);
    private final JLabel countLabel = new JLabel();
    private RowFilter<TableModel, Integer> extraFilter;

    public SearchableTable(TableModel model) {
        super(new BorderLayout());
        table = new JTable(model);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(false);
        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);
        table.getTableHeader().setReorderingAllowed(false);

        DecimalFormat money = new DecimalFormat("#,##0.##");
        table.setDefaultRenderer(Double.class, new DefaultTableCellRenderer() {
            {
                setHorizontalAlignment(RIGHT);
            }

            @Override
            protected void setValue(Object value) {
                setText(value == null ? "" : money.format(value));
            }
        });

        searchField.putClientProperty("JTextField.placeholderText", "Search...  (Ctrl+F)");
        searchField.putClientProperty("JTextField.showClearButton", true);
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                applyFilter();
            }

            public void removeUpdate(DocumentEvent e) {
                applyFilter();
            }

            public void changedUpdate(DocumentEvent e) {
                applyFilter();
            }
        });
        model.addTableModelListener(e -> updateCount());

        countLabel.setForeground(Theme.muted());
        add(new JScrollPane(table), BorderLayout.CENTER);
        updateCount();
    }

    public JTable getTable() {
        return table;
    }

    public JTextField getSearchField() {
        return searchField;
    }

    public JLabel getCountLabel() {
        return countLabel;
    }

    public void setExtraFilter(RowFilter<TableModel, Integer> filter) {
        this.extraFilter = filter;
        applyFilter();
    }

    /** Model index of the selected row, or -1. */
    public int selectedModelRow() {
        int view = table.getSelectedRow();
        return view < 0 ? -1 : table.convertRowIndexToModel(view);
    }

    public void applyFilter() {
        List<RowFilter<TableModel, Integer>> filters = new ArrayList<>();
        String text = searchField.getText().trim();
        if (!text.isEmpty()) {
            filters.add(RowFilter.regexFilter("(?i)" + Pattern.quote(text)));
        }
        if (extraFilter != null) filters.add(extraFilter);
        sorter.setRowFilter(filters.isEmpty() ? null : RowFilter.andFilter(filters));
        updateCount();
    }

    private void updateCount() {
        countLabel.setText(table.getRowCount() + " of " + table.getModel().getRowCount() + " rows");
    }

    /** Colors a whole row using a function of the model row (e.g. red for overdue loans). */
    public void setRowColorizer(java.util.function.IntFunction<Color> colorForModelRow) {
        DefaultTableCellRenderer base = new DefaultTableCellRenderer();
        for (Class<?> c : new Class<?>[]{Object.class, String.class, Integer.class, Double.class, Long.class}) {
            javax.swing.table.TableCellRenderer original = table.getDefaultRenderer(c);
            table.setDefaultRenderer(c, (t, value, selected, focus, row, col) -> {
                Component comp = (original != null ? original : base)
                        .getTableCellRendererComponent(t, value, selected, focus, row, col);
                if (!selected) {
                    Color fg = colorForModelRow.apply(t.convertRowIndexToModel(row));
                    comp.setForeground(fg != null ? fg : t.getForeground());
                }
                return comp;
            });
        }
    }
}
