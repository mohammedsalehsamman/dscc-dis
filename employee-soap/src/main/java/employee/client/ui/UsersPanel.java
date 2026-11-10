package employee.client.ui;

import dscc.ui.Async;
import dscc.ui.BeanTableModel;
import dscc.ui.FormDialog;
import dscc.ui.FormDialog.Validators;
import dscc.ui.Refreshable;
import dscc.ui.SearchableTable;
import dscc.ui.Theme;
import dscc.ui.Toast;
import dscc.ui.UiKit;
import employee.client.SoapClient;
import employee.model.UserAccount;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

/**
 * User account management (AuthService): create accounts linked to employees,
 * reset passwords and delete accounts. Roles decide what each user can do in both systems.
 */
public class UsersPanel extends JPanel implements Refreshable {

    private final SoapClient client;
    private final BeanTableModel<UserAccount> model = new BeanTableModel<UserAccount>()
            .column("Username", String.class, UserAccount::getUsername)
            .column("Role", String.class, UserAccount::getRole)
            .column("Employee ID", Integer.class, UserAccount::getEmployeeId)
            .column("Employee name", String.class, UserAccount::getEmployeeName);
    private final SearchableTable table = new SearchableTable(model);

    public UsersPanel(SoapClient client) {
        super(new BorderLayout());
        this.client = client;
        table.setRowColorizer(row -> {
            UserAccount u = model.getRow(row);
            if (u == null) return null;
            switch (u.getRole()) {
                case "ADMIN":
                    return Theme.DANGER;
                case "LIBRARIAN":
                    return Theme.ACCENT;
                default:
                    return null;
            }
        });

        JToolBar tb = UiKit.toolbar(table.getSearchField(), null,
                UiKit.primary("+ New account", e -> onCreate()),
                UiKit.button("Reset password", e -> onReset()),
                UiKit.danger("Delete", e -> onDelete()), null,
                UiKit.button("Refresh", e -> refresh()));

        JLabel roles = new JLabel("<html><b>ADMIN</b>: everything  ·  <b>LIBRARIAN</b>: manages the library (verified by the RMI server through SOAP)"
                + "  ·  <b>EMPLOYEE</b>: own profile + catalog</html>");
        roles.putClientProperty("FlatLaf.style", "font: -1");

        JPanel content = new JPanel(new BorderLayout(0, 6));
        content.add(tb, BorderLayout.NORTH);
        content.add(table, BorderLayout.CENTER);
        content.add(roles, BorderLayout.SOUTH);
        add(UiKit.page(UiKit.header("User accounts", "SOAP AuthService: listUsers / createUser / resetPassword / deleteUser"),
                content), BorderLayout.CENTER);
        UiKit.shortcut(this, "F5", this::refresh);
    }

    @Override
    public void refresh() {
        Async.run(this, () -> client.auth().listUsers(client.getToken()), model::setRows);
    }

    private UserAccount selected() {
        UserAccount u = model.getRow(table.selectedModelRow());
        if (u == null) Toast.info(this, "Select an account first");
        return u;
    }

    private void onCreate() {
        Map<String, String> v = new FormDialog(this, "New user account")
                .text("username", "Username", "", s -> s.matches("[A-Za-z0-9_.]{3,30}") ? null : "3-30 letters, digits, _ or .")
                .password("password", "Password", Validators.minLength(4))
                .combo("role", "Role", new String[]{"EMPLOYEE", "LIBRARIAN", "ADMIN"}, "EMPLOYEE", false)
                .text("employeeId", "Employee ID", "", Validators.positiveInt())
                .okText("Create")
                .showDialog();
        if (v == null) return;
        Async.run(this, () -> client.auth().createUser(client.getToken(), v.get("username"), v.get("password"),
                v.get("role"), Integer.parseInt(v.get("employeeId"))), msg -> {
            Toast.result(this, msg);
            refresh();
        });
    }

    private void onReset() {
        UserAccount u = selected();
        if (u == null) return;
        Map<String, String> v = new FormDialog(this, "Reset password of " + u.getUsername())
                .password("password", "New password", Validators.minLength(4))
                .okText("Reset")
                .showDialog();
        if (v == null) return;
        Async.run(this, () -> client.auth().resetPassword(client.getToken(), u.getUsername(), v.get("password")),
                msg -> Toast.result(this, msg));
    }

    private void onDelete() {
        UserAccount u = selected();
        if (u == null) return;
        if (JOptionPane.showConfirmDialog(this, "Delete account " + u.getUsername() + "?", "Confirm",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        Async.run(this, () -> client.auth().deleteUser(client.getToken(), u.getUsername()), msg -> {
            Toast.result(this, msg);
            refresh();
        });
    }
}
