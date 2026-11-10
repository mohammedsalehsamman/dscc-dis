package dscc.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Donut chart with a legend, drawn with Java2D.
 */
public class PieChartPanel extends JPanel {

    private final String title;
    private Map<String, ? extends Number> data = new LinkedHashMap<>();

    public PieChartPanel(String title) {
        this.title = title;
        setOpaque(false);
        setPreferredSize(new Dimension(360, 260));
    }

    public void setData(Map<String, ? extends Number> data) {
        this.data = data == null ? new LinkedHashMap<>() : data;
        repaint();
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

        double total = 0;
        for (Number n : data.values()) total += n.doubleValue();
        if (total <= 0) {
            g2.setFont(base);
            g2.setColor(Theme.muted());
            g2.drawString("No data", w / 2 - 25, h / 2);
            g2.dispose();
            return;
        }

        int size = Math.max(60, Math.min(h - 70, (w - 32) / 2));
        int cx = 20, cy = 44 + (h - 54 - size) / 2;
        double start = 90;
        int i = 0;
        for (Map.Entry<String, ? extends Number> e : data.entrySet()) {
            double extent = -360.0 * e.getValue().doubleValue() / total;
            g2.setColor(Theme.PALETTE[i % Theme.PALETTE.length]);
            g2.fill(new Arc2D.Double(cx, cy, size, size, start, extent, Arc2D.PIE));
            start += extent;
            i++;
        }
        // donut hole
        int hole = (int) (size * 0.55);
        g2.setColor(Theme.cardBackground());
        g2.fill(new Ellipse2D.Double(cx + (size - hole) / 2.0, cy + (size - hole) / 2.0, hole, hole));
        g2.setColor(getForeground());
        g2.setFont(base.deriveFont(Font.BOLD, base.getSize2D() + 4));
        String t = String.valueOf((long) total);
        FontMetrics fmBig = g2.getFontMetrics();
        g2.drawString(t, cx + (size - fmBig.stringWidth(t)) / 2, cy + size / 2 + fmBig.getAscent() / 2 - 2);

        // legend
        g2.setFont(base.deriveFont(base.getSize2D() - 1));
        FontMetrics fm = g2.getFontMetrics();
        int lx = cx + size + 20, ly = 50;
        i = 0;
        for (Map.Entry<String, ? extends Number> e : data.entrySet()) {
            if (ly > h - 14) break;
            g2.setColor(Theme.PALETTE[i % Theme.PALETTE.length]);
            g2.fillRoundRect(lx, ly - 10, 12, 12, 4, 4);
            g2.setColor(getForeground());
            long pct = Math.round(100 * e.getValue().doubleValue() / total);
            String label = BarChartPanel.fit(e.getKey(), fm, w - lx - 70) + "  " + e.getValue() + " (" + pct + "%)";
            g2.drawString(label, lx + 18, ly);
            ly += 20;
            i++;
        }
        g2.dispose();
    }
}
