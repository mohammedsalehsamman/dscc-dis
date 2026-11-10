package employee.client.ui;

import dscc.ui.Async;
import dscc.ui.BeanTableModel;
import dscc.ui.FormDialog;
import dscc.ui.FormDialog.Validators;
import dscc.ui.Refreshable;
import dscc.ui.SearchableTable;
import dscc.ui.TableExport;
import dscc.ui.Toast;
import dscc.ui.UiKit;
import employee.client.ActivityPoller;
import employee.client.SoapClient;
import employee.model.Employee;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Employee management screen: searchable/sortable table, department filter,
 * add / edit / delete through dialogs with live validation, CSV export and printing.
 * Refreshes by itself when another administrator changes employees (activity polling).
 */
public class EmployeesPanel extends JPanel implements Refreshable {

    private static final String ALL = "All departments";

    private final SoapClient client;
    private final BeanTableModel<Employee> model = new BeanTableModel<Employee>()
            .column("ID", Integer.class, Employee::getId)
            .column("Name", String.class, Employee::getName)
            .column("Department", String.class, Employee::getDepartment)
            .column("Position", String.class, Employee::getPosition)
            .column("Salary", Double.class, Employee::getSalary)
            .column("Email", String.class, Employee::getEmail)
            .column("Phone", String.class, Employee::getPhone)
            .column("Hire date", String.class, Employee::getHireDate);
    private final SearchableTable table = new SearchableTable(model);
    private final JComboBox<String> deptFilter = new JComboBox<>(new String[]{ALL});

    public EmployeesPanel(SoapClient client, ActivityPoller poller) {
        super(new BorderLayout());
        this.client = client;

        JTable t = table.getTable();
        t.getColumnModel().getColumn(0).setMaxWidth(60);
        t.getColumnModel().getColumn(1).setPreferredWidth(150);
        t.getColumnModel().getColumn(5).setPreferredWidth(190);

        deptFilter.addActionListener(e -> {
            Object sel = deptFilter.getSelectedItem();
            table.setExtraFilter(sel == null || ALL.equals(sel) ? null : new RowFilter<TableModel, Integer>() {
                @Override
                public boolean include(Entry<? extends TableModel, ? extends Integer> entry) {
                    return sel.equals(entry.getValue(2));
                }
            });
        });

        JToolBar tb = UiKit.toolbar(table.getSearchField(), deptFilter, null,
                UiKit.primary("+ Add", e -> onAdd()),
                UiKit.button("Edit", e -> onEdit()),
                UiKit.danger("Delete", e -> onDelete()), null,
                UiKit.button("Refresh", e -> refresh()),
                UiKit.button("Export CSV", e -> TableExport.toCsv(this, t, "employees")),
                UiKit.button("Print / PDF", e -> TableExport.print(this, t, "Employees")));

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(table.getCountLabel(), BorderLayout.WEST);
        JLabel hint = new JLabel("Double-click a row to edit  ·  Ctrl+N new  ·  Del delete  ·  F5 refresh");
        hint.putClientProperty("FlatLaf.style", "font: -1; foreground: $Label.disabledForeground");
        bottom.add(hint, BorderLayout.EAST);

        JPanel content = new JPanel(new BorderLayout());
        content.add(tb, BorderLayout.NORTH);
        content.add(table, BorderLayout.CENTER);
        content.add(bottom, BorderLayout.SOUTH);
        add(UiKit.page(UiKit.header("Employees", "SOAP: listEmployees / addEmployee / updateEmployee / deleteEmployee"),
                content), BorderLayout.CENTER);

        // mouse: double click = edit, right click = menu
        JPopupMenu menu = new JPopupMenu();
        menu.add(new JMenuItem("Edit")).addActionListener(e -> onEdit());
        menu.add(new JMenuItem("Delete")).addActionListener(e -> onDelete());
        t.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) onEdit();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                popup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                popup(e);
            }

            private void popup(MouseEvent e) {
                if (!e.isPopupTrigger()) return;
                int row = t.rowAtPoint(e.getPoint());
                if (row >= 0) t.setRowSelectionInterval(row, row);
                menu.show(t, e.getX(), e.getY());
            }
        });
        t.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("DELETE"), "dscc-delete");
        t.getActionMap().put("dscc-delete", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                onDelete();
            }
        });
        UiKit.shortcut(this, "ctrl N", this::onAdd);
        UiKit.shortcut(this, "ctrl F", () -> table.getSearchField().requestFocusInWindow());
        UiKit.shortcut(this, "F5", this::refresh);

        if (poller != null) {
            poller.addListener((entries, initial) -> {
                if (initial) return;
                boolean changed = entries.stream().anyMatch(a -> a.getAction().contains("EMPLOYEE") || a.getAction().contains("SALARY"));
                if (changed && isShowing()) refresh();
            });
        }
    }

    @Override
    public void refresh() {
        Async.run(this, () -> client.employees().listEmployees(), list -> {
            int selectedId = selected() == null ? -1 : selected().getId();
            model.setRows(list);
            updateDepartments(list);
            if (selectedId >= 0) select(selectedId);
        });
    }

    private void updateDepartments(List<Employee> list) {
        Object current = deptFilter.getSelectedItem();
        TreeSet<String> depts = new TreeSet<>();
        for (Employee e : list) if (e.getDepartment() != null) depts.add(e.getDepartment());
        DefaultComboBoxModel<String> m = new DefaultComboBoxModel<>();
        m.addElement(ALL);
        for (String d : depts) m.addElement(d);
        deptFilter.setModel(m);
        deptFilter.setSelectedItem(current != null && depts.contains(current.toString()) ? current : ALL);
    }

    private Employee selected() {
        return model.getRow(table.selectedModelRow());
    }

    private void select(int id) {
        for (int i = 0; i < model.getRowCount(); i++) {
            if (model.getRow(i).getId() == id) {
                int view = table.getTable().convertRowIndexToView(i);
                if (view >= 0) {
                    table.getTable().setRowSelectionInterval(view, view);
                    table.getTable().scrollRectToVisible(table.getTable().getCellRect(view, 0, true));
                }
                return;
            }
        }
    }

    private void onAdd() {
        int nextId = 1;
        for (Employee e : model.getRows()) nextId = Math.max(nextId, e.getId() + 1);
        Employee e = showForm(null, nextId);
        if (e == null) return;
        Async.run(this, () -> client.employees().addEmployee(e), msg -> {
            Toast.result(this, msg);
            if (msg.startsWith("Success")) {
                refresh();
                SwingUtilities.invokeLater(() -> select(e.getId()));
            }
        });
    }

    private void onEdit() {
        Employee old = selected();
        if (old == null) {
            Toast.info(this, "Select an employee first");
            return;
        }
        Employee e = showForm(old, old.getId());
        if (e == null) return;
        Async.run(this, () -> client.employees().updateEmployee(e), msg -> {
            Toast.result(this, msg);
            if (msg.startsWith("Success")) refresh();
        });
    }

    private void onDelete() {
        Employee e = selected();
        if (e == null) {
            Toast.info(this, "Select an employee first");
            return;
        }
        int ok = JOptionPane.showConfirmDialog(this, "Delete employee #" + e.getId() + " " + e.getName() + "?\n"
                + "Any login account linked to this employee will stop working in the library.",
                "Confirm delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok != JOptionPane.YES_OPTION) return;
        Async.run(this, () -> client.employees().deleteEmployee(e.getId()), msg -> {
            Toast.result(this, msg);
            if (msg.startsWith("Success")) refresh();
        });
    }

    /** Add/edit dialog. Returns the employee or null when cancelled. */
    private Employee showForm(Employee old, int id) {
        List<String> depts = new ArrayList<>();
        for (int i = 1; i < deptFilter.getItemCount(); i++) depts.add(deptFilter.getItemAt(i));
        FormDialog f = new FormDialog(this, old == null ? "Add employee" : "Edit employee #" + id);
        if (old == null) f.text("id", "Employee ID", String.valueOf(id), Validators.positiveInt());
        else f.readOnly("id", "Employee ID", String.valueOf(id));
        f.text("name", "Full name", old == null ? "" : old.getName(), Validators.required())
                .text("salary", "Salary", old == null ? "" : stripZeros(old.getSalary()), Validators.nonNegativeNumber())
                .combo("department", "Department", depts.toArray(new String[0]), old == null ? null : old.getDepartment(), true)
                .text("position", "Position", old == null ? "" : nz(old.getPosition()), null)
                .text("email", "Email", old == null ? "" : nz(old.getEmail()), Validators.optionalEmail())
                .text("phone", "Phone", old == null ? "" : nz(old.getPhone()), null)
                .text("hireDate", "Hire date (yyyy-MM-dd)", old == null ? LocalDate.now().toString() : nz(old.getHireDate()),
                        Validators.optionalDate());
        Map<String, String> v = f.showDialog();
        if (v == null) return null;
        return new Employee(Integer.parseInt(v.get("id")), v.get("name"), Double.parseDouble(v.get("salary")),
                v.get("department"), v.get("position"), v.get("email"), v.get("phone"), v.get("hireDate"));
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static String stripZeros(double d) {
        return d == Math.rint(d) ? String.valueOf((long) d) : String.valueOf(d);
    }
}
