package dscc.portal;

import dscc.ui.Refreshable;
import dscc.ui.Theme;
import dscc.ui.Toast;
import dscc.ui.UiKit;
import employee.client.ActivityPoller;
import employee.client.ui.ActivityPanel;
import employee.client.ui.EmployeeStatsPanel;
import employee.client.ui.EmployeesPanel;
import employee.client.ui.ProfilePanel;
import employee.client.ui.UsersPanel;
import employee.model.ActivityEntry;
import library.client.ui.CatalogPanel;
import library.client.ui.EventsPanel;
import library.client.ui.LibraryClientApp;
import library.client.ui.LibraryStatsPanel;
import library.client.ui.LoansPanel;
import library.client.ui.MyLibraryPanel;
import library.client.ui.ReservationsPanel;
import library.client.ui.StudentsPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Portal main window: sidebar navigation that depends on the role, pages created on first use,
 * a top bar (user, theme, sign out) and a status bar with the health of both servers.
 */
class MainFrame extends JFrame {

    private final PortalSession session;
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final JPanel sidebar = new JPanel();
    private final ButtonGroup navGroup = new ButtonGroup();
    private final Map<String, Supplier<JComponent>> factories = new LinkedHashMap<>();
    private final Map<String, JComponent> pages = new LinkedHashMap<>();
    private final Map<String, JToggleButton> navButtons = new LinkedHashMap<>();
    private final ActivityPoller poller;
    private final UiKit.StatusDot soapDot = new UiKit.StatusDot("SOAP");
    private final UiKit.StatusDot rmiDot = new UiKit.StatusDot("RMI");
    private String current;

    MainFrame(PortalSession session) {
        super("DSCC Integrated Portal");
        this.session = session;
        this.poller = session.soap != null && session.isAdmin() ? new ActivityPoller(session.soap, 3000) : null;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(BorderFactory.createCompoundBorder(new MatteBorder(0, 0, 0, 1, Theme.border()),
                new EmptyBorder(14, 10, 14, 10)));
        JLabel brand = new JLabel("DSCC Portal");
        brand.putClientProperty("FlatLaf.style", "font: +6 bold; foreground: #2F6FEB");
        brand.setAlignmentX(LEFT_ALIGNMENT);
        brand.setBorder(new EmptyBorder(0, 8, 14, 0));
        sidebar.add(brand);
        buildNavigation();
        sidebar.add(Box.createVerticalGlue());
        JScrollPane sideScroll = new JScrollPane(sidebar, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sideScroll.setBorder(null);
        sideScroll.setPreferredSize(new Dimension(220, 10));

        getContentPane().add(sideScroll, BorderLayout.WEST);
        JPanel main = new JPanel(new BorderLayout());
        main.add(topBar(), BorderLayout.NORTH);
        main.add(content, BorderLayout.CENTER);
        main.add(statusBar(), BorderLayout.SOUTH);
        getContentPane().add(main, BorderLayout.CENTER);

        setSize(1320, 820);
        setMinimumSize(new Dimension(1000, 640));
        setLocationRelativeTo(null);

        // F5 refreshes the visible page anywhere in the window
        UiKit.shortcut(getRootPane(), "F5", () -> {
            JComponent page = pages.get(current);
            if (page instanceof Refreshable) ((Refreshable) page).refresh();
        });

        // live notifications
        if (session.library != null) {
            session.library.addListener(ev -> LibraryClientApp.notify(this, session.library, ev));
        }
        if (poller != null) {
            poller.addListener((entries, initial) -> {
                if (initial) return;
                for (ActivityEntry a : entries) {
                    if (!session.username.equals(a.getUsername()) && !a.getAction().startsWith("LOG")) {
                        Toast.info(this, "Employees · " + a.getUsername() + ": " + a.getAction().replace('_', ' ').toLowerCase()
                                + " - " + a.getDetails());
                    }
                }
            });
        }

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                shutdown();
                new Timer(800, x -> System.exit(0)).start();
            }
        });
    }

    // ------------------------------------------------------------------ navigation

    private void buildNavigation() {
        boolean lib = session.libraryOnline();
        if (session.isStudent()) {
            section("Library");
            nav("my", "My library", () -> new MyLibraryPanel(session.library));
            nav("catalog", "Catalog & reserve", () -> new CatalogPanel(session.library));
            nav("libstats", "Statistics", () -> new LibraryStatsPanel(session.library));
            nav("events", "Live events", () -> new EventsPanel(session.library));
            return;
        }
        if (session.isAdmin() || session.isLibrarian()) {
            section("Overview");
            nav("dashboard", "Dashboard", () -> new DashboardPanel(session, poller));
            nav("reports", "Reports", () -> new ReportsPanel(session));
        }
        if (session.isAdmin()) {
            section("Employees  ·  SOAP");
            nav("employees", "Employees", () -> new EmployeesPanel(session.soap, poller));
            nav("payroll", "Payroll statistics", () -> new EmployeeStatsPanel(session.soap, poller));
            nav("users", "User accounts", () -> new UsersPanel(session.soap));
            nav("activity", "Activity log", () -> new ActivityPanel(poller));
        }
        section("Library  ·  RMI");
        nav("catalog", "Catalog", () -> lib ? new CatalogPanel(session.library) : offline());
        if (session.isAdmin() || session.isLibrarian()) {
            nav("loans", "Loans", () -> lib ? new LoansPanel(session.library) : offline());
            nav("students", "Students", () -> lib ? new StudentsPanel(session.library) : offline());
            nav("reservations", "Reservations", () -> lib ? new ReservationsPanel(session.library) : offline());
            nav("libstats", "Library statistics", () -> lib ? new LibraryStatsPanel(session.library) : offline());
            nav("events", "Live events", () -> lib ? new EventsPanel(session.library) : offline());
        }
        section("Me");
        nav("profile", "My profile", () -> new ProfilePanel(session.soap));
    }

    private void section(String title) {
        JLabel l = new JLabel(title.toUpperCase());
        l.putClientProperty("FlatLaf.style", "font: -2 bold; foreground: $Label.disabledForeground");
        l.setBorder(new EmptyBorder(12, 10, 4, 0));
        l.setAlignmentX(LEFT_ALIGNMENT);
        sidebar.add(l);
    }

    private void nav(String key, String title, Supplier<JComponent> factory) {
        factories.put(key, factory);
        JToggleButton b = new JToggleButton(title);
        b.putClientProperty("JButton.buttonType", "toolBarButton");
        b.putClientProperty("FlatLaf.style", "selectedBackground: #2F6FEB; selectedForeground: #FFFFFF;"
                + " margin: 7,12,7,12; font: +1");
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setAlignmentX(LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        b.setFocusable(false);
        b.addActionListener(e -> show(key));
        navGroup.add(b);
        navButtons.put(key, b);
        sidebar.add(b);
        sidebar.add(Box.createVerticalStrut(2));
    }

    void show(String key) {
        JComponent page = pages.get(key);
        if (page == null) {
            page = factories.get(key).get();
            pages.put(key, page);
            content.add(page, key);
        }
        current = key;
        cards.show(content, key);
        navButtons.get(key).setSelected(true);
        if (page instanceof Refreshable) ((Refreshable) page).refresh();
    }

    private JComponent offline() {
        JPanel p = new JPanel(new GridBagLayout());
        JLabel l = new JLabel("<html><center><b style='font-size:16px'>The Library RMI server is offline</b><br><br>"
                + "Start the library server (run.bat rmi-server) and sign in again.</center></html>");
        p.add(l);
        return p;
    }

    // ------------------------------------------------------------------ bars

    private JComponent topBar() {
        JPanel top = new JPanel(new BorderLayout());
        top.setBorder(BorderFactory.createCompoundBorder(new MatteBorder(0, 0, 1, 0, Theme.border()),
                new EmptyBorder(8, 18, 8, 14)));
        int hour = LocalTime.now().getHour();
        String greet = hour < 12 ? "Good morning" : hour < 18 ? "Good afternoon" : "Good evening";
        JLabel hello = new JLabel(greet + ", " + session.displayName.split(" ")[0]);
        hello.putClientProperty("FlatLaf.style", "font: +2 bold");
        top.add(hello, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.add(new JLabel(session.displayName));
        Color roleColor = session.isAdmin() ? Theme.DANGER : session.isLibrarian() ? Theme.ACCENT
                : session.isStudent() ? Theme.SUCCESS : Theme.PURPLE;
        right.add(UiKit.badge(session.role, roleColor));
        JButton theme = new JButton(Theme.isDark() ? "☀" : "☾");
        theme.setToolTipText("Switch light / dark theme");
        theme.addActionListener(e -> {
            Theme.toggle();
            theme.setText(Theme.isDark() ? "☀" : "☾");
        });
        right.add(theme);
        right.add(UiKit.button("Sign out", e -> {
            shutdown();
            dispose();
            new LoginFrame().setVisible(true);
        }));
        top.add(right, BorderLayout.EAST);
        return top;
    }

    private JComponent statusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBorder(BorderFactory.createCompoundBorder(new MatteBorder(1, 0, 0, 0, Theme.border()),
                new EmptyBorder(3, 12, 3, 12)));
        JLabel info = new JLabel(session.soap != null
                ? "Single sign-on: SOAP token " + session.soap.getToken().substring(0, 8) + "… also accepted by the RMI server"
                : "Signed in through RMI as student " + session.username);
        info.putClientProperty("FlatLaf.style", "font: -1");
        bar.add(info, BorderLayout.WEST);
        JPanel dots = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        JLabel clock = new JLabel();
        clock.putClientProperty("FlatLaf.style", "font: -1");
        if (session.soap != null) dots.add(soapDot);
        dots.add(rmiDot);
        dots.add(clock);
        bar.add(dots, BorderLayout.EAST);

        Runnable health = () -> new SwingWorker<boolean[], Void>() {
            @Override
            protected boolean[] doInBackground() {
                return new boolean[]{session.soap != null && session.soap.ping(),
                        session.library != null && session.library.ping()};
            }

            @Override
            protected void done() {
                try {
                    boolean[] r = get();
                    soapDot.set(r[0], r[0] ? "SOAP online" : "SOAP offline");
                    String live = session.library != null && session.library.hasLiveUpdates() ? " · live" : "";
                    rmiDot.set(r[1], r[1] ? "RMI online" + live : "RMI offline");
                } catch (Exception ignored) {
                    // ignore
                }
            }
        }.execute();
        health.run();
        new Timer(5000, e -> health.run()).start();
        DateTimeFormatter f = DateTimeFormatter.ofPattern("HH:mm:ss");
        new Timer(1000, e -> clock.setText("   " + LocalTime.now().format(f))).start();
        return bar;
    }

    // ------------------------------------------------------------------ lifecycle

    void open() {
        setVisible(true);
        show(factories.containsKey("dashboard") ? "dashboard" : factories.containsKey("my") ? "my" : "profile");
        if (poller != null) poller.start();
        if (session.libraryOnline() && !session.isStudent() && !session.isAdmin() && !session.isLibrarian()) {
            Toast.info(this, "Your role (" + session.role + ") can view the catalog and your profile");
        }
        if (!session.libraryOnline()) Toast.warning(this, "Library RMI server is offline - library pages are unavailable");
    }

    private void shutdown() {
        if (poller != null) poller.stop();
        new Thread(session::close).start();
    }
}
