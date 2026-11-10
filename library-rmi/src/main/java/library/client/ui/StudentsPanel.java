package library.client.ui;

import dscc.ui.Async;
import dscc.ui.BeanTableModel;
import dscc.ui.FormDialog;
import dscc.ui.FormDialog.Validators;
import dscc.ui.Refreshable;
import dscc.ui.SearchableTable;
import dscc.ui.TableExport;
import dscc.ui.Theme;
import dscc.ui.Toast;
import dscc.ui.UiKit;
import library.client.LibraryConnection;
import library.common.Student;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

/**
 * Library members: register, edit (optionally reset PIN), delete; shows active loans and unpaid fines.
 */
public class StudentsPanel extends JPanel implements Refreshable {

    private final LibraryConnection conn;
    private final BeanTableModel<Student> model = new BeanTableModel<Student>()
            .column("Student ID", String.class, Student::getStudentId)
            .column("Name", String.class, Student::getName)
            .column("Email", String.class, Student::getEmail)
            .column("Phone", String.class, Student::getPhone)
            .column("Books on loan", Integer.class, Student::getActiveLoans)
            .column("Unpaid fines", Double.class, Student::getUnpaidFines);
    private final SearchableTable table = new SearchableTable(model);

    public StudentsPanel(LibraryConnection conn) {
        super(new BorderLayout());
        this.conn = conn;
        table.setRowColorizer(row -> {
            Student s = model.getRow(row);
            return s != null && s.getUnpaidFines() > 0 ? Theme.WARNING : null;
        });
        JTable t = table.getTable();
        JToolBar tb = UiKit.toolbar(table.getSearchField(), null,
                UiKit.primary("+ Register student", e -> onAdd()),
                UiKit.button("Edit", e -> onEdit()),
                UiKit.danger("Delete", e -> onDelete()),
                UiKit.button("Issue book…", e -> {
                    Student s = model.getRow(table.selectedModelRow());
                    LibraryActions.issueBook(this, conn, null, s == null ? null : s.getStudentId(), this::refresh);
                }), null,
                UiKit.button("Refresh", e -> refresh()),
                UiKit.button("Export CSV", e -> TableExport.toCsv(this, t, "students")));
        JPanel content = new JPanel(new BorderLayout());
        content.add(tb, BorderLayout.NORTH);
        content.add(table, BorderLayout.CENTER);
        content.add(table.getCountLabel(), BorderLayout.SOUTH);
        add(UiKit.page(UiKit.header("Students", "RMI: registerStudent / updateStudent / deleteStudent / listStudents"),
                content), BorderLayout.CENTER);
        UiKit.shortcut(this, "ctrl N", this::onAdd);
        UiKit.shortcut(this, "F5", this::refresh);
        conn.addListener(ev -> {
            if (isShowing()) refresh();
        });
    }

    @Override
    public void refresh() {
        Async.run(this, () -> conn.service().listStudents(conn.staffToken()), model::setRows);
    }

    private Student selected() {
        Student s = model.getRow(table.selectedModelRow());
        if (s == null) Toast.info(this, "Select a student first");
        return s;
    }

    private void onAdd() {
        int next = 1001;
        for (Student s : model.getRows()) {
            try {
                next = Math.max(next, Integer.parseInt(s.getStudentId().replaceAll("\\D", "")) + 1);
            } catch (NumberFormatException ignored) {
                // non-numeric id
            }
        }
        Map<String, String> v = new FormDialog(this, "Register student")
                .text("id", "Student ID", "S" + next, s -> s.matches("[A-Za-z0-9]{3,20}") ? null : "3-20 letters/digits")
                .text("name", "Full name", "", Validators.required())
                .text("email", "Email", "", Validators.optionalEmail())
                .text("phone", "Phone", "", null)
                .password("pin", "PIN (4-8 digits)", s -> s.matches("\\d{4,8}") ? null : "4-8 digits")
                .okText("Register")
                .showDialog();
        if (v == null) return;
        Student s = new Student(v.get("id"), v.get("name"), v.get("email"), v.get("phone"));
        Async.run(this, () -> conn.service().registerStudent(conn.staffToken(), s, v.get("pin")), msg -> {
            Toast.result(this, msg);
            refresh();
        });
    }

    private void onEdit() {
        Student old = selected();
        if (old == null) return;
        Map<String, String> v = new FormDialog(this, "Edit student " + old.getStudentId())
                .readOnly("id", "Student ID", old.getStudentId())
                .text("name", "Full name", old.getName(), Validators.required())
                .text("email", "Email", old.getEmail() == null ? "" : old.getEmail(), Validators.optionalEmail())
                .text("phone", "Phone", old.getPhone() == null ? "" : old.getPhone(), null)
                .password("pin", "New PIN (leave empty to keep)", s -> s.isEmpty() || s.matches("\\d{4,8}") ? null : "4-8 digits")
                .showDialog();
        if (v == null) return;
        Student s = new Student(old.getStudentId(), v.get("name"), v.get("email"), v.get("phone"));
        Async.run(this, () -> conn.service().updateStudent(conn.staffToken(), s, v.get("pin")), msg -> {
            Toast.result(this, msg);
            refresh();
        });
    }

    private void onDelete() {
        Student s = selected();
        if (s == null) return;
        if (JOptionPane.showConfirmDialog(this, "Delete student " + s.getName() + "?", "Confirm",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        Async.run(this, () -> conn.service().deleteStudent(conn.staffToken(), s.getStudentId()), msg -> {
            Toast.result(this, msg);
            refresh();
        });
    }
}
