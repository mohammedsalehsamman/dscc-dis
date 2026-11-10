package dscc.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.prefs.Preferences;

/**
 * Application settings.
 * Order of lookup: user preferences (saved from the Settings dialog) -> config.properties
 * in the working directory or its parent -> built-in default.
 */
public final class AppConfig {

    public static final String SOAP_URL = "soap.url";
    public static final String RMI_HOST = "rmi.host";
    public static final String RMI_PORT = "rmi.port";
    public static final String LOAN_DAYS = "loan.days";
    public static final String FINE_PER_DAY = "fine.perDay";
    public static final String MAX_LOANS = "loan.maxPerStudent";
    public static final String MAX_RENEWALS = "loan.maxRenewals";
    public static final String OVERDUE_CHECK_SECONDS = "overdue.checkSeconds";

    private static final Properties DEFAULTS = new Properties();
    private static final Properties FILE = new Properties();
    private static final Preferences PREFS = Preferences.userRoot().node("dscc-dis");

    static {
        DEFAULTS.setProperty(SOAP_URL, "http://localhost:8080");
        DEFAULTS.setProperty(RMI_HOST, "localhost");
        DEFAULTS.setProperty(RMI_PORT, "1099");
        DEFAULTS.setProperty(LOAN_DAYS, "14");
        DEFAULTS.setProperty(FINE_PER_DAY, "500");
        DEFAULTS.setProperty(MAX_LOANS, "5");
        DEFAULTS.setProperty(MAX_RENEWALS, "2");
        DEFAULTS.setProperty(OVERDUE_CHECK_SECONDS, "60");
        for (Path p : new Path[]{Paths.get("config.properties"), Paths.get("..", "config.properties")}) {
            if (Files.isRegularFile(p)) {
                try (InputStream in = new FileInputStream(p.toFile())) {
                    FILE.load(in);
                } catch (IOException ignored) {
                    // fall back to defaults
                }
                break;
            }
        }
    }

    private AppConfig() {
    }

    public static String get(String key) {
        String v = System.getProperty(key);
        if (v == null) v = PREFS.get(key, null);
        if (v == null) v = FILE.getProperty(key);
        if (v == null) v = DEFAULTS.getProperty(key);
        return v == null ? null : v.trim();
    }

    public static int getInt(String key) {
        try {
            return Integer.parseInt(get(key));
        } catch (NumberFormatException e) {
            return Integer.parseInt(DEFAULTS.getProperty(key));
        }
    }

    public static double getDouble(String key) {
        try {
            return Double.parseDouble(get(key));
        } catch (NumberFormatException e) {
            return Double.parseDouble(DEFAULTS.getProperty(key));
        }
    }

    /** Saves a user setting (used by the client Settings dialog). */
    public static void save(String key, String value) {
        PREFS.put(key, value.trim());
    }

    public static Preferences prefs() {
        return PREFS;
    }

    /** SOAP base URL without a trailing slash, e.g. http://localhost:8080 */
    public static String soapBaseUrl() {
        String url = get(SOAP_URL);
        while (url.endsWith("/")) url = url.substring(0, url.length() - 1);
        return url;
    }
}
