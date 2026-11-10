package dscc.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * Small helpers that keep all screens consistent: page headers, toolbar buttons,
 * keyboard shortcuts and status badges.
 */
public final class UiKit {

    private UiKit() {
    }

    /** Page header with a big title and a muted subtitle. */
    public static JPanel header(String title, String subtitle) {
        JPanel p = new JPanel(new BorderLayout(0, 2));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(0, 2, 10, 2));
        JLabel t = new JLabel(title);
        t.putClientProperty("FlatLaf.style", "font: +8 bold");
        p.add(t, BorderLayout.NORTH);
        if (subtitle != null) {
            JLabel s = new JLabel(subtitle);
            s.putClientProperty("FlatLaf.style", "foreground: $Label.disabledForeground");
            p.add(s, BorderLayout.CENTER);
        }
        return p;
    }

    public static JButton button(String text, ActionListener action) {
        JButton b = new JButton(text);
        b.addActionListener(action);
        return b;
    }

    /** Highlighted (primary) button. */
    public static JButton primary(String text, ActionListener action) {
        JButton b = button(text, action);
        b.putClientProperty("FlatLaf.style", "background: #2F6FEB; foreground: #FFFFFF; borderWidth: 0; font: bold");
        return b;
    }

    /** Red button for destructive actions. */
    public static JButton danger(String text, ActionListener action) {
        JButton b = button(text, action);
        b.putClientProperty("FlatLaf.style", "foreground: #DC2626");
        return b;
    }

    public static JToolBar toolbar(Component... items) {
        JToolBar tb = new JToolBar();
        tb.setFloatable(false);
        tb.setBorder(new EmptyBorder(0, 0, 8, 0));
        for (Component c : items) {
            if (c == null) tb.addSeparator();
            else {
                tb.add(c);
                tb.add(Box.createHorizontalStrut(4));
            }
        }
        return tb;
    }

    /** Binds a key (e.g. "ctrl N", "F5", "DELETE") to an action anywhere inside the given component. */
    public static void shortcut(JComponent comp, String key, Runnable action) {
        KeyStroke ks = KeyStroke.getKeyStroke(key);
        String name = "dscc-" + key;
        comp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ks, name);
        comp.getActionMap().put(name, new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (comp.isShowing()) action.run();
            }
        });
    }

    /** Colored pill label, e.g. "Available" in green. */
    public static JLabel badge(String text, Color color) {
        JLabel l = new JLabel(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 40));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        l.setForeground(color);
        l.setBorder(new EmptyBorder(2, 10, 2, 10));
        l.putClientProperty("FlatLaf.style", "font: -1 bold");
        return l;
    }

    /** Wraps content with page padding. */
    public static JPanel page(JComponent header, JComponent content) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(new EmptyBorder(18, 22, 16, 22));
        if (header != null) p.add(header, BorderLayout.NORTH);
        p.add(content, BorderLayout.CENTER);
        return p;
    }

    /** Small green/red dot + text used for server health. */
    public static class StatusDot extends JLabel {
        private Color color = Color.GRAY;

        public StatusDot(String text) {
            super(text);
            setBorder(new EmptyBorder(0, 16, 0, 6));
        }

        public void set(boolean ok, String text) {
            color = ok ? Theme.SUCCESS : Theme.DANGER;
            setText(text);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            int d = 9;
            g2.fillOval(3, (getHeight() - d) / 2, d, d);
            g2.dispose();
        }
    }
}
