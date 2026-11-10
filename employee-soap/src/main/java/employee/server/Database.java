package employee.server;

import dscc.util.Passwords;
import dscc.util.ServerLog;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * SQLite database of the Employee server (file data/employees.db).
 * Creates the tables on first start and inserts sample data.
 * One shared connection; callers synchronize on this object.
 */
public class Database implements AutoCloseable {

    private final Connection conn;

    public Database(String path) throws SQLException {
        File f = new File(path);
        if (f.getParentFile() != null) f.getParentFile().mkdirs();
        conn = DriverManager.getConnection("jdbc:sqlite:" + f.getPath());
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
        }
        createSchema();
        seed();
        ServerLog.info("Database ready: " + f.getAbsolutePath());
    }

    public Connection conn() {
        return conn;
    }

    @Override
    public void close() throws SQLException {
        conn.close();
    }

    private void createSchema() throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS employees ("
                    + " id INTEGER PRIMARY KEY,"
                    + " name TEXT NOT NULL,"
                    + " salary REAL NOT NULL CHECK (salary >= 0),"
                    + " department TEXT, position TEXT, email TEXT, phone TEXT, hire_date TEXT)");
            st.execute("CREATE TABLE IF NOT EXISTS users ("
                    + " username TEXT PRIMARY KEY,"
                    + " password_hash TEXT NOT NULL,"
                    + " salt TEXT NOT NULL,"
                    + " role TEXT NOT NULL CHECK (role IN ('ADMIN','LIBRARIAN','EMPLOYEE')),"
                    + " employee_id INTEGER REFERENCES employees(id) ON DELETE SET NULL)");
            st.execute("CREATE TABLE IF NOT EXISTS sessions ("
                    + " token TEXT PRIMARY KEY,"
                    + " username TEXT NOT NULL REFERENCES users(username) ON DELETE CASCADE,"
                    + " expires_at INTEGER NOT NULL)");
            st.execute("CREATE TABLE IF NOT EXISTS activity_log ("
                    + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + " time TEXT NOT NULL, username TEXT, action TEXT NOT NULL, details TEXT)");
        }
    }

    private void seed() throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM employees")) {
            if (rs.next() && rs.getInt(1) > 0) return;
        }
        Object[][] employees = {
                {1, "Ahmad Ali", 2500, "IT", "IT Manager", "ahmad.ali@dscc.edu", "0991000001", "2019-03-01"},
                {2, "Sara Hasan", 1800, "HR", "HR Specialist", "sara.hasan@dscc.edu", "0991000002", "2020-06-15"},
                {3, "Omar Khaled", 2100, "Finance", "Accountant", "omar.khaled@dscc.edu", "0991000003", "2018-09-10"},
                {4, "Lina Mahmoud", 1600, "Library", "Librarian", "lina.mahmoud@dscc.edu", "0991000004", "2021-01-20"},
                {5, "Yousef Ibrahim", 1900, "IT", "Software Developer", "yousef.ibrahim@dscc.edu", "0991000005", "2022-02-01"},
                {6, "Rana Saleh", 1700, "Marketing", "Marketing Officer", "rana.saleh@dscc.edu", "0991000006", "2021-11-05"},
                {7, "Khaled Nasser", 2300, "Finance", "Financial Analyst", "khaled.nasser@dscc.edu", "0991000007", "2017-04-12"},
                {8, "Maya Haddad", 1550, "Library", "Assistant Librarian", "maya.haddad@dscc.edu", "0991000008", "2023-08-01"},
                {9, "Hadi Younes", 2000, "IT", "Network Engineer", "hadi.younes@dscc.edu", "0991000009", "2020-10-18"},
                {10, "Nour Darwish", 1650, "HR", "Recruiter", "nour.darwish@dscc.edu", "0991000010", "2024-01-07"},
        };
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO employees (id,name,salary,department,position,email,phone,hire_date) VALUES (?,?,?,?,?,?,?,?)")) {
            for (Object[] e : employees) {
                for (int i = 0; i < e.length; i++) ps.setObject(i + 1, e[i]);
                ps.executeUpdate();
            }
        }
        Object[][] users = {
                {"admin", "admin123", "ADMIN", 1},
                {"librarian", "lib123", "LIBRARIAN", 4},
                {"maya", "lib123", "LIBRARIAN", 8},
                {"employee", "emp123", "EMPLOYEE", 2},
        };
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO users (username,password_hash,salt,role,employee_id) VALUES (?,?,?,?,?)")) {
            for (Object[] u : users) {
                String salt = Passwords.newSalt();
                ps.setString(1, (String) u[0]);
                ps.setString(2, Passwords.hash((String) u[1], salt));
                ps.setString(3, salt);
                ps.setString(4, (String) u[2]);
                ps.setInt(5, (Integer) u[3]);
                ps.executeUpdate();
            }
        }
        ServerLog.info("Sample data inserted (10 employees, 4 user accounts)");
    }
}
