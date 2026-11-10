package dscc.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Dashboard card: a small title, a big number and an optional note, with a colored accent strip.
 */
public class StatCard extends JPanel {

    private final JLabel valueLabel = new JLabel("-");
    private final JLabel noteLabel = new JLabel(" ");
    private final Color accent;

    public StatCard(String title, Color accent) {
        super(new BorderLayout(0, 4));
        this.accent = accent;
        setOpaque(false);
        setBorder(new EmptyBorder(14, 18, 14, 16));

        JLabel titleLabel = new JLabel(title.toUpperCase());
        titleLabel.putClientProperty("FlatLaf.style", "font: -1 bold; foreground: $Label.disabledForeground");
        valueLabel.putClientProperty("FlatLaf.style", "font: +12 bold");
        noteLabel.putClientProperty("FlatLaf.style", "font: -1; foreground: $Label.disabledForeground");

        add(titleLabel, BorderLayout.NORTH);
        add(valueLabel, BorderLayout.CENTER);
        add(noteLabel, BorderLayout.SOUTH);
    }

    public void setValue(String value) {
        valueLabel.setText(value);
    }

    public void setNote(String note) {
        noteLabel.setText(note == null || note.isEmpty() ? " " : note);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Theme.cardBackground());
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
        g2.setColor(Theme.border());
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
        g2.setColor(accent);
        g2.fillRoundRect(0, 10, 5, getHeight() - 20, 5, 5);
        g2.dispose();
        super.paintComponent(g);
    }
}
