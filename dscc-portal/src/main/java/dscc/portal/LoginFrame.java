package dscc.portal;

import dscc.ui.Async;
import dscc.ui.Theme;
import dscc.ui.UiKit;
import dscc.util.AppConfig;
import employee.client.SoapClient;
import employee.model.LoginResult;
import library.client.LibraryConnection;
import library.common.StudentSession;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Portal sign-in window. Two ways in:
 * <ul>
 *   <li>Employee: SOAP AuthService.login -> token; the token is then verified by the RMI server
 *       for library staff (single sign-on across both systems).</li>
 *   <li>Student: RMI studentLogin (student ID + PIN).</li>
 * </ul>
 */
class LoginFrame extends JFrame {

    private final JTextField empUser = new JTextField(18);
    private final JPasswordField empPass = new JPasswordField(18);
    private final JTextField stuId = new JTextField(18);
    private final JPasswordField stuPin = new JPasswordField(18);
    private final JLabel error = new JLabel(" ");
    private final JTabbedPane tabs = new JTabbedPane();
    private final JButton signIn = UiKit.primary("Sign in", e -> signIn());

    LoginFrame() {
        super("DSCC Integrated Portal - Sign in");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel brand = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0, new Color(0x1E3A8A), getWidth(), getHeight(), new Color(0x7C3AED)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        brand.setPreferredSize(new Dimension(380, 460));
        JLabel title = new JLabel("<html><div style='color:white'>"
                + "<div style='font-size:22px;font-weight:bold'>DSCC Integrated Portal</div><br>"
                + "<div style='font-size:12px'>Employee Management (SOAP)<br>+ Library Management (RMI)</div><br><br>"
                + "<div style='font-size:11px'>&#10003; One sign-in for both systems<br>"
                + "&#10003; Live updates (RMI callbacks + SOAP polling)<br>"
                + "&#10003; SQLite storage, dashboards and reports</div>"
                + "</div></html>");
        brand.add(title);

        empUser.putClientProperty("JTextField.placeholderText", "admin / librarian / employee");
        empPass.putClientProperty("JTextField.showRevealButton", true);
        stuId.putClientProperty("JTextField.placeholderText", "S1001 ... S1006");
        stuPin.putClientProperty("JTextField.placeholderText", "1234");
        stuPin.putClientProperty("JTextField.showRevealButton", true);
        tabs.addTab("Employee", form("Username", empUser, "Password", empPass,
                "Demo: admin/admin123 · librarian/lib123 · employee/emp123"));
        tabs.addTab("Student", form("Student ID", stuId, "PIN", stuPin, "Demo: S1001 with PIN 1234"));
        tabs.addChangeListener(e -> error.setText(" "));

        error.setForeground(Theme.DANGER);
        JPanel right = new JPanel(new BorderLayout(0, 10));
        right.setBorder(new EmptyBorder(36, 34, 24, 34));
        JLabel welcome = new JLabel("Welcome back");
        welcome.putClientProperty("FlatLaf.style", "font: +10 bold");
        JLabel sub = new JLabel("Sign in to continue");
        sub.putClientProperty("FlatLaf.style", "foreground: $Label.disabledForeground");
        JPanel head = new JPanel(new GridLayout(2, 1));
        head.add(welcome);
        head.add(sub);
        right.add(head, BorderLayout.NORTH);
        right.add(tabs, BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout(0, 8));
        south.add(error, BorderLayout.NORTH);
        south.add(signIn, BorderLayout.CENTER);
        JPanel links = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        links.add(UiKit.button("⚙ Servers…", e -> new SettingsDialog(this).setVisible(true)));
        links.add(Box.createHorizontalStrut(6));
        links.add(UiKit.button("Light / Dark", e -> Theme.toggle()));
        south.add(links, BorderLayout.SOUTH);
        right.add(south, BorderLayout.SOUTH);

        getContentPane().add(brand, BorderLayout.WEST);
        getContentPane().add(right, BorderLayout.CENTER);
        getRootPane().setDefaultButton(signIn);
        setSize(860, 500);
        setResizable(false);
        setLocationRelativeTo(null);
    }

    private static JPanel form(String l1, JComponent f1, String l2, JComponent f2, String hint) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(new EmptyBorder(14, 8, 8, 8));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets = new Insets(3, 0, 3, 0);
        c.gridy = 0;
        p.add(new JLabel(l1), c);
        c.gridy = 1;
        p.add(f1, c);
        c.gridy = 2;
        p.add(new JLabel(l2), c);
        c.gridy = 3;
        p.add(f2, c);
        c.gridy = 4;
        JLabel h = new JLabel(hint);
        h.putClientProperty("FlatLaf.style", "font: -1; foreground: $Label.disabledForeground");
        p.add(h, c);
        c.gridy = 5;
        c.weighty = 1;
        p.add(Box.createGlue(), c);
        return p;
    }

    private void signIn() {
        boolean employee = tabs.getSelectedIndex() == 0;
        String user = (employee ? empUser.getText() : stuId.getText()).trim();
        String pass = new String(employee ? empPass.getPassword() : stuPin.getPassword());
        if (user.isEmpty() || pass.isEmpty()) {
            error.setText("Please fill in both fields");
            return;
        }
        signIn.setEnabled(false);
        error.setForeground(UIManager.getColor("Label.foreground"));
        error.setText("Signing in...");
        Async.run(this, () -> employee ? employeeLogin(user, pass) : studentLogin(user, pass), session -> {
            signIn.setEnabled(true);
            dispose();
            new MainFrame(session).open();
        }, ex -> {
            signIn.setEnabled(true);
            error.setForeground(Theme.DANGER);
            error.setText(Async.message(ex));
        });
    }

    /** SOAP login, then single sign-on into the RMI server with the same token. */
    private static PortalSession employeeLogin(String user, String pass) throws Exception {
        SoapClient soap = new SoapClient(AppConfig.soapBaseUrl());
        LoginResult r = soap.login(user, pass);
        if (!r.isSuccess()) throw new IllegalStateException(r.getMessage());
        LibraryConnection lib = new LibraryConnection(AppConfig.get(AppConfig.RMI_HOST), AppConfig.getInt(AppConfig.RMI_PORT));
        try {
            lib.connect();
            if ("ADMIN".equals(r.getRole()) || "LIBRARIAN".equals(r.getRole())) lib.useStaffToken(r.getToken());
        } catch (Exception e) {
            lib.close();
            lib = null; // library server offline: the employee part still works
        }
        return new PortalSession(soap, lib, r.getRole(), r.getFullName(), r.getUsername());
    }

    private static PortalSession studentLogin(String id, String pin) throws Exception {
        LibraryConnection lib = new LibraryConnection(AppConfig.get(AppConfig.RMI_HOST), AppConfig.getInt(AppConfig.RMI_PORT));
        lib.connect();
        try {
            StudentSession s = lib.loginStudent(id, pin);
            return new PortalSession(null, lib, PortalSession.STUDENT, s.getName(), s.getStudentId());
        } catch (Exception e) {
            lib.close();
            throw e;
        }
    }
}
