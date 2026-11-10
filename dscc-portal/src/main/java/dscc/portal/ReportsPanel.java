package dscc.portal;

import dscc.ui.Async;
import dscc.ui.Refreshable;
import dscc.ui.SearchableTable;
import dscc.ui.TableExport;
import dscc.ui.Theme;
import dscc.ui.UiKit;
import employee.model.DepartmentStat;
import employee.model.Employee;
import employee.model.EmployeeStats;
import library.common.LibraryStats;
import library.common.Loan;
import library.common.Student;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reports, each exportable to CSV or printable (PDF via "Microsoft Print to PDF").
 * "Loans by employee" is a cross-system report: loan counts come from the RMI server
 * and employee names/departments from the SOAP server, joined in the client.
 */
class ReportsPanel extends JPanel implements Refreshable {

    private final PortalSession session;
    private final JTabbedPane tabs = new JTabbedPane();
    private final DefaultTableModel payroll = model("Department", "Employees", "Total salaries", "Average salary", "Share of payroll %");
    private final DefaultTableModel byEmployee = model("Employee ID", "Name (SOAP)", "Department (SOAP)", "Position (SOAP)", "Loans issued (RMI)");
    private final DefaultTableModel topBooks = model("Rank", "Title", "Times borrowed");
    private final DefaultTableModel overdue = model("Loan", "Title", "Student", "Due date", "Days late", "Fine so far", "Issued by");
    private final DefaultTableModel fines = model("Student ID", "Name", "Email", "Books on loan", "Fines to pay");

    ReportsPanel(PortalSession session) {
        super(new BorderLayout());
        this.session = session;
        if (session.isAdmin()) {
            tabs.addTab("Payroll by department", reportTab(payroll, "Payroll by department", "SOAP getStatistics"));
            tabs.addTab("Loans by employee", reportTab(byEmployee, "Loans issued by employee",
                    "JOIN in the client: RMI getStatistics (loans per employee id) + SOAP listEmployees (names)"));
        }
        tabs.addTab("Most borrowed books", reportTab(topBooks, "Most borrowed books", "RMI getStatistics"));
        tabs.addTab("Overdue loans", reportTab(overdue, "Overdue loans", "RMI listLoans (active, past due date)"));
        tabs.addTab("Unpaid fines", reportTab(fines, "Students with fines to pay", "RMI listStudents"));
        add(UiKit.page(UiKit.header("Reports", "Export to CSV (Excel) or print / save as PDF"), tabs), BorderLayout.CENTER);
    }

    private static DefaultTableModel model(String... cols) {
        return new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int c) {
                return getRowCount() > 0 && getValueAt(0, c) != null ? getValueAt(0, c).getClass() : Object.class;
            }
        };
    }

    private JComponent reportTab(DefaultTableModel m, String title, String source) {
        SearchableTable t = new SearchableTable(m);
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        JLabel src = new JLabel("Source: " + source);
        src.putClientProperty("FlatLaf.style", "font: -1; foreground: $Label.disabledForeground");
        p.add(UiKit.toolbar(t.getSearchField(), null,
                UiKit.button("Export CSV", e -> TableExport.toCsv(this, t.getTable(), title.toLowerCase().replace(' ', '-'))),
                UiKit.button("Print / PDF", e -> TableExport.print(this, t.getTable(), title)),
                UiKit.button("Refresh", e -> refresh())), BorderLayout.NORTH);
        p.add(t, BorderLayout.CENTER);
        p.add(src, BorderLayout.SOUTH);
        return p;
    }

    @Override
    public void refresh() {
        Async.run(this, () -> {
            Object[] r = new Object[5];
            if (session.isAdmin()) {
                r[0] = session.soap.employees().getStatistics();
                r[1] = session.soap.employees().listEmployees();
            }
            if (session.library != null) {
                r[2] = session.library.service().getStatistics();
                r[3] = session.library.service().listLoans(session.library.staffToken(), true);
                r[4] = session.library.service().listStudents(session.library.staffToken());
            }
            return r;
        }, this::fill);
    }

    @SuppressWarnings("unchecked")
    private void fill(Object[] r) {
        if (r[0] != null) {
            EmployeeStats es = (EmployeeStats) r[0];
            payroll.setRowCount(0);
            for (DepartmentStat d : es.getDepartments()) {
                payroll.addRow(new Object[]{d.getDepartment(), d.getCount(), d.getTotalSalary(), d.getAverageSalary(),
                        Math.round(1000 * d.getTotalSalary() / Math.max(1, es.getTotalSalary())) / 10.0});
            }
            payroll.addRow(new Object[]{"TOTAL", es.getCount(), es.getTotalSalary(), es.getAverageSalary(), 100.0});
        }
        if (r[1] != null && r[2] != null) {
            Map<Integer, Employee> byId = new HashMap<>();
            for (Employee e : (List<Employee>) r[1]) byId.put(e.getId(), e);
            byEmployee.setRowCount(0);
            for (Map.Entry<Integer, Integer> e : ((LibraryStats) r[2]).loansPerEmployeeId.entrySet()) {
                Employee emp = byId.get(e.getKey());
                byEmployee.addRow(new Object[]{e.getKey(), emp == null ? "(no longer employed)" : emp.getName(),
                        emp == null ? "-" : emp.getDepartment(), emp == null ? "-" : emp.getPosition(), e.getValue()});
            }
        }
        if (r[2] != null) {
            topBooks.setRowCount(0);
            int rank = 1;
            for (Map.Entry<String, Integer> e : ((LibraryStats) r[2]).topBooks.entrySet()) {
                topBooks.addRow(new Object[]{rank++, e.getKey(), e.getValue()});
            }
        }
        if (r[3] != null) {
            overdue.setRowCount(0);
            double perDay = dscc.util.AppConfig.getDouble(dscc.util.AppConfig.FINE_PER_DAY);
            for (Loan l : (List<Loan>) r[3]) {
                if (!l.isOverdue()) continue;
                overdue.addRow(new Object[]{l.getId(), l.getBookTitle(), l.getStudentName() + " (" + l.getStudentId() + ")",
                        l.getDueDate().toString(), (int) l.daysLate(), l.daysLate() * perDay, l.getIssuedByName()});
            }
        }
        if (r[4] != null) {
            fines.setRowCount(0);
            for (Student s : (List<Student>) r[4]) {
                if (s.getUnpaidFines() <= 0) continue;
                fines.addRow(new Object[]{s.getStudentId(), s.getName(), s.getEmail(), s.getActiveLoans(), s.getUnpaidFines()});
            }
        }
        if (overdue.getRowCount() > 0) tabs.setForegroundAt(tabs.indexOfTab("Overdue loans"), Theme.DANGER);
    }
}
