package employee.server;

import dscc.util.Passwords;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserDaoTest {

    @TempDir
    Path dir;

    private Database db;
    private UserDao users;

    @BeforeEach
    void setUp() throws SQLException {
        db = new Database(dir.resolve("employees.db").toString());
        users = new UserDao(db);
    }

    @AfterEach
    void tearDown() throws SQLException {
        db.close();
    }

    @Test
    void seededAccountsLogInWithTheirRoleAndEmployee() throws SQLException {
        AuthContext admin = users.checkPassword("admin", "admin123");
        assertNotNull(admin);
        assertEquals("ADMIN", admin.role);
        assertEquals(1, admin.employeeId);
        assertEquals("Ahmad Ali", admin.fullName);

        assertEquals("LIBRARIAN", users.checkPassword("librarian", "lib123").role);
    }

    @Test
    void wrongPasswordOrUnknownUserIsRejected() throws SQLException {
        assertNull(users.checkPassword("admin", "admin124"));
        assertNull(users.checkPassword("nobody", "admin123"));
    }

    @Test
    void passwordsAreStoredAsPbkdf2Hashes() throws SQLException {
        String hash = storedHash("admin");
        assertTrue(hash.startsWith("pbkdf2-sha256$"), hash);
        assertFalse(hash.contains("admin123"));
    }

    @Test
    void legacySha256HashIsUpgradedOnSuccessfulLogin() throws Exception {
        // simulate an account created by the previous version (a single SHA-256)
        String salt = Passwords.newSalt();
        Method legacy = Passwords.class.getDeclaredMethod("legacySha256", String.class, String.class);
        legacy.setAccessible(true);
        String old = (String) legacy.invoke(null, "emp123", salt);
        try (PreparedStatement ps = db.conn().prepareStatement(
                "UPDATE users SET password_hash = ?, salt = ? WHERE username = 'employee'")) {
            ps.setString(1, old);
            ps.setString(2, salt);
            ps.executeUpdate();
        }

        assertNull(users.checkPassword("employee", "wrong"));
        assertEquals(old, storedHash("employee"), "a failed login must not touch the hash");

        assertNotNull(users.checkPassword("employee", "emp123"));
        assertTrue(storedHash("employee").startsWith("pbkdf2-sha256$"), "hash upgraded after login");
        assertNotNull(users.checkPassword("employee", "emp123"), "upgraded hash still verifies");
    }

    @Test
    void sessionsResolveUntilDeleted() throws SQLException {
        String token = users.createSession("librarian");
        AuthContext ctx = users.findSession(token);
        assertNotNull(ctx);
        assertEquals("librarian", ctx.username);
        assertEquals(token, ctx.token);

        users.deleteSession(token);
        assertNull(users.findSession(token));
        assertNull(users.findSession(null));
        assertNull(users.findSession("not-a-token"));
    }

    @Test
    void expiredSessionIsNotAccepted() throws SQLException {
        String token = users.createSession("admin");
        try (PreparedStatement ps = db.conn().prepareStatement("UPDATE sessions SET expires_at = 0 WHERE token = ?")) {
            ps.setString(1, token);
            ps.executeUpdate();
        }
        assertNull(users.findSession(token));
    }

    private String storedHash(String username) throws SQLException {
        try (PreparedStatement ps = db.conn().prepareStatement("SELECT password_hash FROM users WHERE username = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                return rs.getString(1);
            }
        }
    }
}
