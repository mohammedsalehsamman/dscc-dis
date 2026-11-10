package employee.server;

import dscc.util.Passwords;
import employee.model.UserAccount;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Data access for user accounts and login sessions (tokens).
 */
public class UserDao {

    /** Sessions expire after 8 hours. */
    private static final long SESSION_MILLIS = 8L * 60 * 60 * 1000;

    private final Connection conn;

    public UserDao(Database db) {
        this.conn = db.conn();
    }

    /**
     * Returns the session for valid credentials, or null.
     * A hash stored in an older format is upgraded to the current one on a successful check.
     */
    public AuthContext checkPassword(String username, String password) throws SQLException {
        String storedHash;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT password_hash, salt FROM users WHERE username = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                storedHash = rs.getString("password_hash");
                if (!Passwords.verify(password, rs.getString("salt"), storedHash)) return null;
            }
        }
        if (Passwords.needsRehash(storedHash)) setPassword(username, password);
        return contextFor(username, null);
    }

    public String createSession(String username) throws SQLException {
        try (PreparedStatement del = conn.prepareStatement("DELETE FROM sessions WHERE expires_at < ?")) {
            del.setLong(1, System.currentTimeMillis());
            del.executeUpdate();
        }
        String token = UUID.randomUUID().toString();
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO sessions (token, username, expires_at) VALUES (?,?,?)")) {
            ps.setString(1, token);
            ps.setString(2, username);
            ps.setLong(3, System.currentTimeMillis() + SESSION_MILLIS);
            ps.executeUpdate();
        }
        return token;
    }

    /** Resolves a token to the logged-in user, or null if it is unknown or expired. */
    public AuthContext findSession(String token) throws SQLException {
        if (token == null || token.isEmpty()) return null;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT username FROM sessions WHERE token = ? AND expires_at > ?")) {
            ps.setString(1, token);
            ps.setLong(2, System.currentTimeMillis());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return contextFor(rs.getString(1), token);
            }
        }
    }

    public void deleteSession(String token) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM sessions WHERE token = ?")) {
            ps.setString(1, token);
            ps.executeUpdate();
        }
    }

    private AuthContext contextFor(String username, String token) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT u.username, u.role, COALESCE(u.employee_id,0), COALESCE(e.name, u.username)"
                        + " FROM users u LEFT JOIN employees e ON e.id = u.employee_id WHERE u.username = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new AuthContext(rs.getString(1), rs.getString(2), rs.getInt(3), rs.getString(4), token);
            }
        }
    }

    public boolean exists(String username) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM users WHERE username = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void create(String username, String password, String role, int employeeId) throws SQLException {
        String salt = Passwords.newSalt();
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO users (username,password_hash,salt,role,employee_id) VALUES (?,?,?,?,?)")) {
            ps.setString(1, username);
            ps.setString(2, Passwords.hash(password, salt));
            ps.setString(3, salt);
            ps.setString(4, role);
            ps.setInt(5, employeeId);
            ps.executeUpdate();
        }
    }

    public boolean setPassword(String username, String password) throws SQLException {
        String salt = Passwords.newSalt();
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE users SET password_hash = ?, salt = ? WHERE username = ?")) {
            ps.setString(1, Passwords.hash(password, salt));
            ps.setString(2, salt);
            ps.setString(3, username);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(String username) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM users WHERE username = ?")) {
            ps.setString(1, username);
            return ps.executeUpdate() > 0;
        }
    }

    public List<UserAccount> list() throws SQLException {
        List<UserAccount> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT u.username, u.role, COALESCE(u.employee_id,0), COALESCE(e.name,'-')"
                        + " FROM users u LEFT JOIN employees e ON e.id = u.employee_id ORDER BY u.username");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new UserAccount(rs.getString(1), rs.getString(2), rs.getInt(3), rs.getString(4)));
            }
        }
        return list;
    }
}
