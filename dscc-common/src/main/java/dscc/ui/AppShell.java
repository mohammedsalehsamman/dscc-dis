package dscc.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Main window used by the standalone clients: a top bar (title, user, theme switch, sign out),
 * tabs in the middle (each tab refreshes when selected) and a status bar.
 */
public class AppShell extends JFrame {

    private final JTabbedPane tabs = new JTabbedPane();
    private final JLabel status = new JLabel(" ");
    private final JPanel statusRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
    private Runnable onSignOut;

    public AppShell(String title, String subtitle, String userText) {
        super(title);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel top = new JPanel(new BorderLayout());
        top.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(0, 0, 1, 0, Theme.border()), new EmptyBorder(10, 18, 10, 14)));
        JLabel t = new JLabel(title);
        t.putClientProperty("FlatLaf.style", "font: +5 bold");
        JLabel s = new JLabel(subtitle);
        s.putClientProperty("FlatLaf.style", "font: -1; foreground: $Label.disabledForeground");
        JPanel titles = new JPanel(new GridLayout(2, 1));
        titles.setOpaque(false);
        titles.add(t);
        titles.add(s);
        top.add(titles, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        right.setOpaque(false);
        JLabel user = new JLabel(userText);
        user.putClientProperty("FlatLaf.style", "font: bold");
        right.add(user);
        JButton theme = new JButton(Theme.isDark() ? "☀ Light" : "☾ Dark");
        theme.addActionListener(e -> {
            Theme.toggle();
            theme.setText(Theme.isDark() ? "☀ Light" : "☾ Dark");
        });
        right.add(theme);
        JButton signOut = new JButton("Sign out");
        signOut.addActionListener(e -> {
            dispose();
            if (onSignOut != null) onSignOut.run();
        });
        right.add(signOut);
        top.add(right, BorderLayout.EAST);

        tabs.putClientProperty("JTabbedPane.tabHeight", 36);
        tabs.addChangeListener(e -> refreshSelected());

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(1, 0, 0, 0, Theme.border()), new EmptyBorder(4, 14, 4, 10)));
        status.putClientProperty("FlatLaf.style", "font: -1");
        bottom.add(status, BorderLayout.WEST);
        statusRight.setOpaque(false);
        bottom.add(statusRight, BorderLayout.EAST);

        getContentPane().add(top, BorderLayout.NORTH);
        getContentPane().add(tabs, BorderLayout.CENTER);
        getContentPane().add(bottom, BorderLayout.SOUTH);
        setSize(1180, 760);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);
    }

    public void addTab(String title, JComponent panel) {
        tabs.addTab(title, panel);
    }

    public void refreshSelected() {
        Component c = tabs.getSelectedComponent();
        if (c instanceof Refreshable) ((Refreshable) c).refresh();
    }

    public void setStatus(String text) {
        status.setText(text);
    }

    public void addStatusComponent(JComponent c) {
        statusRight.add(c);
    }

    /** Runs when the user presses "Sign out" (e.g. show the login dialog again). */
    public void setOnSignOut(Runnable r) {
        this.onSignOut = r;
    }

    /** Runs when the window is closed in any way (logout, unregister callbacks...). */
    public void onClose(Runnable r) {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                r.run();
            }
        });
    }

    /** Shows the window and refreshes the first tab. */
    public void open() {
        setVisible(true);
        refreshSelected();
    }
}
