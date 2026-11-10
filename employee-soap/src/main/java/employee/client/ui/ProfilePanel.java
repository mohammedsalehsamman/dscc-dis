package employee.client.ui;

import dscc.ui.Async;
import dscc.ui.FormDialog;
import dscc.ui.FormDialog.Validators;
import dscc.ui.Refreshable;
import dscc.ui.Theme;
import dscc.ui.Toast;
import dscc.ui.UiKit;
import employee.client.SoapClient;
import employee.model.Employee;
import employee.model.LoginResult;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.Map;

/**
 * "My profile": the logged-in employee's own record (SOAP getEmployee with their own id)
 * and a change-password form (AuthService.changePassword).
 */
public class ProfilePanel extends JPanel implements Refreshable {

    private final SoapClient client;
    private final JPanel details = new JPanel(new GridBagLayout());
    private final JLabel nameLabel = new JLabel();
    private final JLabel roleBadgeHolder = new JLabel();

    public ProfilePanel(SoapClient client) {
        super(new BorderLayout());
        this.client = client;

        nameLabel.putClientProperty("FlatLaf.style", "font: +10 bold");
        JPanel card = new JPanel(new BorderLayout(0, 12));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.border()), new EmptyBorder(18, 22, 18, 22)));
        JPanel head = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        head.add(nameLabel);
        head.add(Box.createHorizontalStrut(12));
        head.add(roleBadgeHolder);
        card.add(head, BorderLayout.NORTH);
        card.add(details, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        actions.add(UiKit.primary("Change password", e -> onChangePassword()));
        card.add(actions, BorderLayout.SOUTH);

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.add(card, BorderLayout.NORTH);
        add(UiKit.page(UiKit.header("My profile", "SOAP: getEmployee(your id) - employees may only read their own record"),
                wrap), BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        LoginResult s = client.getSession();
        if (s == null) return;
        nameLabel.setText(s.getFullName());
        roleBadgeHolder.removeAll();
        roleBadgeHolder.setLayout(new BorderLayout());
        roleBadgeHolder.add(UiKit.badge(s.getRole(), "ADMIN".equals(s.getRole()) ? Theme.DANGER : Theme.ACCENT));
        Async.run(this, () -> s.getEmployeeId() > 0 ? client.employees().getEmployee(s.getEmployeeId()) : null, this::show);
    }

    private void show(Employee e) {
        details.removeAll();
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(4, 0, 4, 24);
        LoginResult s = client.getSession();
        String[][] rows = e == null
                ? new String[][]{{"Username", s.getUsername()}, {"Employee record", "not linked"}}
                : new String[][]{
                {"Username", s.getUsername()},
                {"Employee ID", String.valueOf(e.getId())},
                {"Department", nz(e.getDepartment())},
                {"Position", nz(e.getPosition())},
                {"Salary", new DecimalFormat("#,##0.##").format(e.getSalary())},
                {"Email", nz(e.getEmail())},
                {"Phone", nz(e.getPhone())},
                {"Hire date", nz(e.getHireDate())}};
        for (int i = 0; i < rows.length; i++) {
            JLabel k = new JLabel(rows[i][0]);
            k.putClientProperty("FlatLaf.style", "foreground: $Label.disabledForeground");
            c.gridx = 0;
            c.gridy = i;
            details.add(k, c);
            c.gridx = 1;
            details.add(new JLabel(rows[i][1]), c);
        }
        details.revalidate();
        details.repaint();
    }

    private void onChangePassword() {
        Map<String, String> v = new FormDialog(this, "Change password")
                .password("old", "Current password", Validators.required())
                .password("new", "New password", Validators.minLength(4))
                .password("confirm", "Confirm new password", Validators.minLength(4))
                .okText("Change")
                .showDialog();
        if (v == null) return;
        if (!v.get("new").equals(v.get("confirm"))) {
            Toast.error(this, "The new passwords do not match");
            return;
        }
        Async.run(this, () -> client.auth().changePassword(client.getToken(), v.get("old"), v.get("new")),
                msg -> Toast.result(this, msg));
    }

    private static String nz(String s) {
        return s == null || s.isEmpty() ? "-" : s;
    }
}
