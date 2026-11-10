package dscc.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import dscc.util.AppConfig;

import javax.swing.*;
import java.awt.*;

/**
 * FlatLaf look and feel with a light / dark switch that is remembered between runs.
 */
public final class Theme {

    private static final String KEY = "ui.dark";

    public static final Color ACCENT = new Color(0x2F6FEB);
    public static final Color SUCCESS = new Color(0x1F9D55);
    public static final Color WARNING = new Color(0xD97706);
    public static final Color DANGER = new Color(0xDC2626);
    public static final Color PURPLE = new Color(0x7C3AED);
    public static final Color TEAL = new Color(0x0D9488);

    /** Series colors for charts. */
    public static final Color[] PALETTE = {
            new Color(0x2F6FEB), new Color(0x10B981), new Color(0xF59E0B), new Color(0xEF4444),
            new Color(0x8B5CF6), new Color(0x06B6D4), new Color(0xEC4899), new Color(0x84CC16)
    };

    private Theme() {
    }

    public static void init() {
        UIManager.put("Component.arc", 10);
        UIManager.put("Button.arc", 10);
        UIManager.put("TextComponent.arc", 8);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.width", 10);
        UIManager.put("Table.rowHeight", 28);
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.intercellSpacing", new Dimension(0, 1));
        UIManager.put("TabbedPane.showTabSeparators", true);
        UIManager.put("Component.accentColor", ACCENT);
        UIManager.put("Component.focusWidth", 1);
        apply(isDark());
    }

    public static boolean isDark() {
        return AppConfig.prefs().getBoolean(KEY, false);
    }

    public static void toggle() {
        boolean dark = !isDark();
        AppConfig.prefs().putBoolean(KEY, dark);
        apply(dark);
        FlatLaf.updateUI();
    }

    private static void apply(boolean dark) {
        if (dark) FlatDarkLaf.setup();
        else FlatLightLaf.setup();
    }

    /** Subtle text color that works in both themes. */
    public static Color muted() {
        Color c = UIManager.getColor("Label.disabledForeground");
        return c != null ? c : Color.GRAY;
    }

    /** Background for cards. */
    public static Color cardBackground() {
        Color c = UIManager.getColor("Panel.background");
        if (c == null) return Color.WHITE;
        return isDark() ? c.brighter() : Color.WHITE;
    }

    public static Color border() {
        Color c = UIManager.getColor("Component.borderColor");
        return c != null ? c : Color.LIGHT_GRAY;
    }
}
