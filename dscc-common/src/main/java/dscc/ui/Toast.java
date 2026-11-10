package dscc.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Small pop-up notification shown in the bottom-right corner of a window; it closes by itself.
 * Several toasts stack on top of each other.
 */
public final class Toast {

    public enum Type {
        INFO(Theme.ACCENT), SUCCESS(Theme.SUCCESS), WARNING(Theme.WARNING), ERROR(Theme.DANGER);
        final Color color;

        Type(Color c) {
            this.color = c;
        }
    }

    private static final List<JWindow> OPEN = new ArrayList<>();

    private Toast() {
    }

    public static void info(Component owner, String msg) {
        show(owner, msg, Type.INFO);
    }

    public static void success(Component owner, String msg) {
        show(owner, msg, Type.SUCCESS);
    }

    public static void warning(Component owner, String msg) {
        show(owner, msg, Type.WARNING);
    }

    public static void error(Component owner, String msg) {
        show(owner, msg, Type.ERROR);
    }

    /** Picks SUCCESS or ERROR depending on a "Success: ..." / "Failure: ..." server message. */
    public static void result(Component owner, String serverMessage) {
        if (serverMessage == null) return;
        show(owner, serverMessage, serverMessage.startsWith("Success") ? Type.SUCCESS : Type.ERROR);
    }

    public static void show(Component owner, String msg, Type type) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> show(owner, msg, type));
            return;
        }
        Window parent = owner == null ? null
                : owner instanceof Window ? (Window) owner : SwingUtilities.getWindowAncestor(owner);
        if (parent == null || !parent.isShowing()) return;

        JWindow w = new JWindow(parent);
        JPanel p = new JPanel(new BorderLayout(10, 0));
        p.setBackground(Theme.cardBackground());
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 5, 0, 0, type.color),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Theme.border()),
                        new EmptyBorder(10, 12, 10, 14))));
        JLabel label = new JLabel("<html><div style='width:280px'>" + escape(msg) + "</div></html>");
        p.add(label, BorderLayout.CENTER);
        w.setContentPane(p);
        w.pack();

        synchronized (OPEN) {
            int offset = 0;
            for (JWindow o : OPEN) offset += o.getHeight() + 8;
            Rectangle b = parent.getBounds();
            w.setLocation(b.x + b.width - w.getWidth() - 20, b.y + b.height - w.getHeight() - 40 - offset);
            OPEN.add(w);
        }
        w.setVisible(true);

        Timer close = new Timer(type == Type.ERROR ? 5000 : 3500, e -> {
            w.dispose();
            synchronized (OPEN) {
                OPEN.remove(w);
            }
        });
        close.setRepeats(false);
        close.start();
        w.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                close.stop();
                w.dispose();
                synchronized (OPEN) {
                    OPEN.remove(w);
                }
            }
        });
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>");
    }
}
