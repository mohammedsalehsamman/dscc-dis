package dscc.ui;

import javax.swing.*;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Simple vertical bar chart drawn with Java2D (no external chart library).
 * Hovering a bar shows its exact value in a tooltip.
 */
public class BarChartPanel extends JPanel {

    private final String title;
    private Map<String, ? extends Number> data = new LinkedHashMap<>();
    private Color color = Theme.ACCENT;
    private DecimalFormat format = new DecimalFormat("#,##0.##");
    private Rectangle[] barBounds = new Rectangle[0];
    private String[] keys = new String[0];

    public BarChartPanel(String title) {
        this.title = title;
        setOpaque(false);
        setPreferredSize(new Dimension(360, 260));
        setToolTipText("");
    }

    public void setData(Map<String, ? extends Number> data) {
        this.data = data == null ? new LinkedHashMap<>() : data;
        repaint();
    }

    public void setColor(Color color) {
        this.color = color;
        repaint();
    }

    public void setFormat(String pattern) {
        this.format = new DecimalFormat(pattern);
    }

    @Override
    public String getToolTipText(java.awt.event.MouseEvent e) {
        for (int i = 0; i < barBounds.length; i++) {
            if (barBounds[i] != null && barBounds[i].contains(e.getPoint())) {
                return keys[i] + ": " + format.format(data.get(keys[i]));
            }
        }
        return null;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();

        g2.setColor(Theme.cardBackground());
        g2.fillRoundRect(0, 0, w - 1, h - 1, 14, 14);
        g2.setColor(Theme.border());
        g2.drawRoundRect(0, 0, w - 1, h - 1, 14, 14);

        Font base = getFont();
        g2.setFont(base.deriveFont(Font.BOLD, base.getSize2D() + 1));
        g2.setColor(getForeground());
        g2.drawString(title, 16, 26);

        if (data.isEmpty()) {
            g2.setFont(base);
            g2.setColor(Theme.muted());
            g2.drawString("No data", w / 2 - 25, h / 2);
            g2.dispose();
            return;
        }

        double max = 0;
        for (Number n : data.values()) max = Math.max(max, n.doubleValue());
        if (max <= 0) max = 1;

        int left = 16, right = 16, top = 46, bottom = 46;
        int chartH = h - top - bottom;
        int n = data.size();
        int slot = (w - left - right) / n;
        int barW = Math.max(8, Math.min(56, (int) (slot * 0.6)));

        // grid lines
        g2.setColor(Theme.border());
        for (int i = 0; i <= 4; i++) {
            int y = top + chartH - chartH * i / 4;
            g2.drawLine(left, y, w - right, y);
        }

        keys = data.keySet().toArray(new String[0]);
        barBounds = new Rectangle[n];
        g2.setFont(base.deriveFont(base.getSize2D() - 1));
        FontMetrics fm = g2.getFontMetrics();
        for (int i = 0; i < n; i++) {
            double v = data.get(keys[i]).doubleValue();
            int bh = (int) Math.round(chartH * v / max);
            int x = left + i * slot + (slot - barW) / 2;
            int y = top + chartH - bh;
            g2.setColor(color);
            g2.fillRoundRect(x, y, barW, Math.max(bh, 2), 6, 6);
            barBounds[i] = new Rectangle(x, y, barW, Math.max(bh, 2));

            String val = format.format(v);
            g2.setColor(getForeground());
            g2.drawString(val, x + (barW - fm.stringWidth(val)) / 2, y - 4);

            String label = fit(keys[i], fm, slot - 4);
            g2.setColor(Theme.muted());
            g2.drawString(label, left + i * slot + (slot - fm.stringWidth(label)) / 2, top + chartH + 18);
        }
        g2.dispose();
    }

    static String fit(String s, FontMetrics fm, int width) {
        if (fm.stringWidth(s) <= width) return s;
        while (s.length() > 1 && fm.stringWidth(s + "…") > width) s = s.substring(0, s.length() - 1);
        return s + "…";
    }
}
