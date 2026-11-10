package employee.client.ui;

import dscc.ui.AppShell;
import dscc.ui.LoginDialog;
import dscc.ui.Theme;
import dscc.ui.Toast;
import dscc.ui.UiKit;
import dscc.util.AppConfig;
import employee.client.ActivityPoller;
import employee.client.SoapClient;
import employee.model.ActivityEntry;
import employee.model.LoginResult;

import javax.swing.*;

/**
 * Full standalone SOAP client of the Employee Management System.
 * ADMIN: employees, statistics, user accounts, live activity log, profile.
 * Other roles: their own profile.
 */
public class EmployeeClientApp {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Theme.init();
            showLogin();
        });
    }

    private static void showLogin() {
        SoapClient client = new SoapClient(AppConfig.soapBaseUrl());
        LoginDialog login = new LoginDialog(null, "Employee Management",
                "SOAP client  ·  server " + client.getBaseUrl(), "Username", "Password", "admin / librarian / employee",
                (user, pass) -> {
                    try {
                        LoginResult r = client.login(user, pass);
                        return r.isSuccess() ? null : r.getMessage();
                    } catch (Exception e) {
                        return dscc.ui.Async.message(e);
                    }
                });
        if (!login.showDialog()) {
            System.exit(0);
            return;
        }
        openMain(client);
    }

    private static void openMain(SoapClient client) {
        LoginResult s = client.getSession();
        AppShell shell = new AppShell("Employee Management System", "JAX-WS SOAP client  ·  " + client.getBaseUrl(),
                s.getFullName() + "  (" + s.getRole() + ")");
        ActivityPoller poller = new ActivityPoller(client, 3000);

        if (client.isAdmin()) {
            shell.addTab("Employees", new EmployeesPanel(client, poller));
            shell.addTab("Statistics", new EmployeeStatsPanel(client, poller));
            shell.addTab("User accounts", new UsersPanel(client));
            shell.addTab("Activity log", new ActivityPanel(poller));
        }
        shell.addTab("My profile", new ProfilePanel(client));

        UiKit.StatusDot dot = new UiKit.StatusDot("SOAP server");
        dot.set(true, "SOAP server online");
        shell.addStatusComponent(dot);
        shell.setStatus("Signed in as " + s.getUsername() + "  ·  token in SOAP header: " + s.getToken().substring(0, 8) + "…");
        new Timer(5000, e -> new SwingWorker<Boolean, Void>() {
            protected Boolean doInBackground() {
                return client.ping();
            }

            protected void done() {
                try {
                    boolean ok = get();
                    dot.set(ok, ok ? "SOAP server online" : "SOAP server OFFLINE");
                } catch (Exception ignored) {
                    // ignore
                }
            }
        }.execute()).start();

        // pop-up notification when ANOTHER user changes something
        poller.addListener((entries, initial) -> {
            if (initial) return;
            for (ActivityEntry a : entries) {
                if (!s.getUsername().equals(a.getUsername()) && !a.getAction().startsWith("LOG")) {
                    Toast.info(shell, a.getUsername() + ": " + a.getAction().replace('_', ' ').toLowerCase() + " - " + a.getDetails());
                }
            }
        });
        shell.onClose(() -> {
            poller.stop();
            new Thread(client::logout).start();
        });
        shell.setOnSignOut(EmployeeClientApp::showLogin);
        shell.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        shell.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                shell.dispose();
                new Timer(800, x -> System.exit(0)).start();
            }
        });
        shell.open();
        poller.start();
    }
}
