package library.client.ui;

import dscc.ui.AppShell;
import dscc.ui.Async;
import dscc.ui.LoginDialog;
import dscc.ui.Theme;
import dscc.ui.Toast;
import dscc.ui.UiKit;
import dscc.util.AppConfig;
import library.client.LibraryConnection;
import library.common.LibraryEvent;

import javax.swing.*;

/**
 * Full standalone RMI client of the Library Management System.
 * Sign in as staff (employee account, verified by the RMI server through SOAP),
 * as a student (student ID + PIN) or browse as a guest.
 */
public class LibraryClientApp {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Theme.init();
            start();
        });
    }

    private static void start() {
        String host = AppConfig.get(AppConfig.RMI_HOST);
        int port = AppConfig.getInt(AppConfig.RMI_PORT);
        LibraryConnection conn = new LibraryConnection(host, port);

        String[] options = {"Staff (employee account)", "Student", "Guest (browse only)"};
        int choice = JOptionPane.showOptionDialog(null,
                "<html><b>Library Management System</b><br>RMI server: " + host + ":" + port + "<br><br>How do you want to sign in?</html>",
                "Library - RMI client", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        if (choice < 0) {
            System.exit(0);
            return;
        }
        boolean ok;
        if (choice == 0) {
            ok = new LoginDialog(null, "Staff sign in", "Employee account - verified by the RMI server via SOAP",
                    "Username", "Password", "librarian / admin", (u, p) -> {
                try {
                    if (conn.service() == null) conn.connect();
                    conn.loginStaff(u, p);
                    return null;
                } catch (Exception e) {
                    return Async.message(e);
                }
            }).showDialog();
        } else if (choice == 1) {
            ok = new LoginDialog(null, "Student sign in", "Use your student ID and PIN", "Student ID", "PIN",
                    "S1001 ... S1006 (PIN 1234)", (u, p) -> {
                try {
                    if (conn.service() == null) conn.connect();
                    conn.loginStudent(u, p);
                    return null;
                } catch (Exception e) {
                    return Async.message(e);
                }
            }).showDialog();
        } else {
            try {
                conn.connect();
                ok = true;
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, Async.message(e), "Cannot connect", JOptionPane.ERROR_MESSAGE);
                ok = false;
            }
        }
        if (!ok) {
            conn.close();
            start();
            return;
        }
        openMain(conn);
    }

    private static void openMain(LibraryConnection conn) {
        String who = conn.isStaff() ? conn.getStaff().getName() + "  (" + conn.getStaff().getRole() + ")"
                : conn.isStudent() ? conn.getStudent().getName() + "  (STUDENT)" : "Guest";
        AppShell shell = new AppShell("Library Management System",
                "Java RMI client  ·  rmi://" + conn.getHost() + "/" + "LibraryService", who);

        if (conn.isStudent()) shell.addTab("My library", new MyLibraryPanel(conn));
        shell.addTab("Catalog", new CatalogPanel(conn));
        if (conn.isStaff()) {
            shell.addTab("Loans", new LoansPanel(conn));
            shell.addTab("Students", new StudentsPanel(conn));
            shell.addTab("Reservations", new ReservationsPanel(conn));
        }
        shell.addTab("Statistics", new LibraryStatsPanel(conn));
        shell.addTab("Live events", new EventsPanel(conn));

        UiKit.StatusDot dot = new UiKit.StatusDot("RMI server");
        dot.set(true, "RMI server online");
        shell.addStatusComponent(dot);
        shell.setStatus(conn.hasLiveUpdates() ? "Live updates ON - the server pushes changes through RMI callbacks"
                : "Live updates OFF - press F5 to refresh");
        new Timer(5000, e -> new SwingWorker<Boolean, Void>() {
            protected Boolean doInBackground() {
                return conn.ping();
            }

            protected void done() {
                try {
                    boolean up = get();
                    dot.set(up, up ? "RMI server online" : "RMI server OFFLINE");
                } catch (Exception ignored) {
                    // ignore
                }
            }
        }.execute()).start();

        conn.addListener(ev -> notify(shell, conn, ev));
        shell.onClose(() -> new Thread(conn::close).start());
        shell.setOnSignOut(LibraryClientApp::start);
        shell.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                shell.dispose();
                new Timer(800, x -> System.exit(0)).start();
            }
        });
        shell.open();
    }

    /** Pop-up notifications for pushed events: staff see everything, students see their own. */
    public static void notify(java.awt.Component owner, LibraryConnection conn, LibraryEvent ev) {
        if (conn.isStudent()) {
            if (!conn.getStudent().getStudentId().equals(ev.getStudentId())) return;
            if (ev.getType() == LibraryEvent.Type.RESERVATION_READY) Toast.success(owner, "Good news! " + ev.getMessage());
            else if (ev.getType() == LibraryEvent.Type.OVERDUE) Toast.error(owner, ev.getMessage());
            else Toast.info(owner, ev.getMessage());
        } else if (conn.isStaff()) {
            if (ev.getType() == LibraryEvent.Type.OVERDUE) Toast.warning(owner, ev.getMessage());
            else Toast.info(owner, ev.getMessage());
        }
    }
}
