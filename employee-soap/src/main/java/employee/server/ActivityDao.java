package employee.server;

import dscc.util.ServerLog;
import employee.model.ActivityEntry;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Activity log stored in the database. Clients poll it to show live changes.
 */
public class ActivityDao {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final Connection conn;

    public ActivityDao(Database db) {
        this.conn = db.conn();
    }

    public void log(String username, String action, String details) {
        ServerLog.info("[" + (username == null ? "-" : username) + "] " + action + " : " + details);
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO activity_log (time, username, action, details) VALUES (?,?,?,?)")) {
            ps.setString(1, LocalDateTime.now().format(FMT));
            ps.setString(2, username);
            ps.setString(3, action);
            ps.setString(4, details);
            ps.executeUpdate();
        } catch (SQLException e) {
            ServerLog.info("Could not write activity log: " + e.getMessage());
        }
    }

    /** Entries with id greater than lastId (max 200). lastId &lt; 0 returns the latest 50. */
    public List<ActivityEntry> since(long lastId) throws SQLException {
        List<ActivityEntry> list = new ArrayList<>();
        String sql = lastId < 0
                ? "SELECT id,time,username,action,details FROM activity_log ORDER BY id DESC LIMIT 50"
                : "SELECT id,time,username,action,details FROM activity_log WHERE id > ? ORDER BY id LIMIT 200";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (lastId >= 0) ps.setLong(1, lastId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new ActivityEntry(rs.getLong(1), rs.getString(2), rs.getString(3),
                            rs.getString(4), rs.getString(5)));
                }
            }
        }
        if (lastId < 0) Collections.reverse(list);
        return list;
    }

    public long lastId() throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COALESCE(MAX(id),0) FROM activity_log")) {
            return rs.next() ? rs.getLong(1) : 0;
        }
    }
}
