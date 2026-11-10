package library.server;

import dscc.util.AppConfig;
import dscc.util.Passwords;
import dscc.util.ServerLog;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;

/**
 * SQLite database of the Library server (file data/library.db).
 * Creates the tables on first start and inserts sample books, students and loan history.
 * One shared connection; the service synchronizes all access.
 */
public class Database implements AutoCloseable {

    private final Connection conn;

    public Database(String path) throws SQLException {
        File f = new File(path);
        if (f.getParentFile() != null) f.getParentFile().mkdirs();
        conn = DriverManager.getConnection("jdbc:sqlite:" + f.getPath());
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
            st.execute("CREATE TABLE IF NOT EXISTS books ("
                    + " book_id TEXT PRIMARY KEY, title TEXT NOT NULL, author TEXT NOT NULL,"
                    + " category TEXT, isbn TEXT, year INTEGER,"
                    + " total_copies INTEGER NOT NULL DEFAULT 1 CHECK (total_copies >= 0),"
                    + " available_copies INTEGER NOT NULL DEFAULT 1 CHECK (available_copies >= 0))");
            st.execute("CREATE TABLE IF NOT EXISTS students ("
                    + " student_id TEXT PRIMARY KEY, name TEXT NOT NULL, email TEXT, phone TEXT,"
                    + " pin_hash TEXT NOT NULL, salt TEXT NOT NULL)");
            // separate data structure for issued book records (loan history)
            st.execute("CREATE TABLE IF NOT EXISTS loans ("
                    + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + " book_id TEXT NOT NULL, book_title TEXT, student_id TEXT NOT NULL,"
                    + " issued_by_employee INTEGER, issued_by_name TEXT,"
                    + " issue_date TEXT NOT NULL, due_date TEXT NOT NULL, return_date TEXT,"
                    + " renew_count INTEGER NOT NULL DEFAULT 0,"
                    + " fine REAL NOT NULL DEFAULT 0, fine_paid INTEGER NOT NULL DEFAULT 0,"
                    + " overdue_notified INTEGER NOT NULL DEFAULT 0)");
            st.execute("CREATE TABLE IF NOT EXISTS reservations ("
                    + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + " book_id TEXT NOT NULL, student_id TEXT NOT NULL,"
                    + " created_at TEXT NOT NULL, status TEXT NOT NULL)");
            st.execute("CREATE TABLE IF NOT EXISTS events ("
                    + " id INTEGER PRIMARY KEY AUTOINCREMENT, time TEXT NOT NULL, type TEXT NOT NULL,"
                    + " message TEXT, book_id TEXT, student_id TEXT)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_loans_book ON loans(book_id)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_loans_student ON loans(student_id)");
        }
    }

    private void seed() throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM books")) {
            if (rs.next() && rs.getInt(1) > 0) return;
        }
        Object[][] books = {
                {"B1", "Clean Code", "Robert C. Martin", "Software Engineering", "9780132350884", 2008, 3},
                {"B2", "Java: The Complete Reference", "Herbert Schildt", "Programming", "9781260463415", 2022, 4},
                {"B3", "Distributed Systems", "Andrew S. Tanenbaum", "Networks & Distributed", "9781543057386", 2017, 2},
                {"B4", "Computer Networks", "Andrew S. Tanenbaum", "Networks & Distributed", "9780136764052", 2021, 3},
                {"B5", "Database System Concepts", "Abraham Silberschatz", "Databases", "9780078022159", 2019, 2},
                {"B6", "Introduction to Algorithms", "Thomas H. Cormen", "Algorithms", "9780262033848", 2009, 3},
                {"B7", "Operating System Concepts", "Abraham Silberschatz", "Operating Systems", "9781118063330", 2018, 2},
                {"B8", "Design Patterns", "Erich Gamma", "Software Engineering", "9780201633610", 1994, 2},
                {"B9", "Head First Java", "Kathy Sierra", "Programming", "9781491910771", 2022, 2},
                {"B10", "Artificial Intelligence: A Modern Approach", "Stuart Russell", "Artificial Intelligence", "9780134610993", 2020, 1},
                {"B11", "The Pragmatic Programmer", "David Thomas", "Software Engineering", "9780135957059", 2019, 2},
                {"B12", "Computer Security: Principles and Practice", "William Stallings", "Security", "9780134794105", 2017, 1},
                {"B13", "Refactoring", "Martin Fowler", "Software Engineering", "9780134757599", 2018, 1},
                {"B14", "Java Network Programming", "Elliotte Rusty Harold", "Networks & Distributed", "9781449357672", 2013, 1},
        };
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO books (book_id,title,author,category,isbn,year,total_copies,available_copies) VALUES (?,?,?,?,?,?,?,?)")) {
            for (Object[] b : books) {
                for (int i = 0; i < b.length; i++) ps.setObject(i + 1, b[i]);
                ps.setObject(8, b[6]);
                ps.executeUpdate();
            }
        }
        Object[][] students = {
                {"S1001", "Ali Hassan", "ali.hassan@student.dscc.edu", "0944000001"},
                {"S1002", "Fatima Omar", "fatima.omar@student.dscc.edu", "0944000002"},
                {"S1003", "Mohammad Saeed", "mohammad.saeed@student.dscc.edu", "0944000003"},
                {"S1004", "Hala Kassem", "hala.kassem@student.dscc.edu", "0944000004"},
                {"S1005", "Tarek Mansour", "tarek.mansour@student.dscc.edu", "0944000005"},
                {"S1006", "Reem Abbas", "reem.abbas@student.dscc.edu", "0944000006"},
        };
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO students (student_id,name,email,phone,pin_hash,salt) VALUES (?,?,?,?,?,?)")) {
            for (Object[] s : students) {
                String salt = Passwords.newSalt();
                for (int i = 0; i < 4; i++) ps.setObject(i + 1, s[i]);
                ps.setString(5, Passwords.hash("1234", salt));
                ps.setString(6, salt);
                ps.executeUpdate();
            }
        }
        // loan history: {book, student, issued N days ago, returned after N days (-1 = still on loan), employee id, employee name}
        Object[][] loans = {
                {"B1", "S1001", 170, 10, 4, "Lina Mahmoud"},
                {"B2", "S1002", 160, 12, 8, "Maya Haddad"},
                {"B1", "S1003", 140, 9, 4, "Lina Mahmoud"},
                {"B6", "S1001", 120, 20, 4, "Lina Mahmoud"},
                {"B3", "S1004", 100, 7, 8, "Maya Haddad"},
                {"B1", "S1005", 90, 14, 4, "Lina Mahmoud"},
                {"B9", "S1002", 75, 5, 8, "Maya Haddad"},
                {"B4", "S1003", 60, 11, 4, "Lina Mahmoud"},
                {"B2", "S1006", 45, 13, 8, "Maya Haddad"},
                {"B8", "S1004", 30, 6, 4, "Lina Mahmoud"},
                {"B6", "S1005", 26, 17, 8, "Maya Haddad"},
                {"B1", "S1002", 20, -1, 4, "Lina Mahmoud"},
                {"B5", "S1001", 10, -1, 8, "Maya Haddad"},
                {"B10", "S1003", 5, -1, 4, "Lina Mahmoud"},
                {"B2", "S1005", 3, -1, 8, "Maya Haddad"},
                {"B6", "S1006", 2, -1, 4, "Lina Mahmoud"},
        };
        int loanDays = AppConfig.getInt(AppConfig.LOAN_DAYS);
        double finePerDay = AppConfig.getDouble(AppConfig.FINE_PER_DAY);
        LocalDate today = LocalDate.now();
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO loans (book_id,book_title,student_id,issued_by_employee,issued_by_name,issue_date,due_date,return_date,fine,fine_paid)"
                        + " VALUES (?,(SELECT title FROM books WHERE book_id=?),?,?,?,?,?,?,?,?)")) {
            for (int i = 0; i < loans.length; i++) {
                Object[] l = loans[i];
                LocalDate issued = today.minusDays((Integer) l[2]);
                LocalDate due = issued.plusDays(loanDays);
                int after = (Integer) l[3];
                LocalDate returned = after < 0 ? null : issued.plusDays(after);
                long late = returned == null ? 0 : Math.max(0, returned.toEpochDay() - due.toEpochDay());
                ps.setString(1, (String) l[0]);
                ps.setString(2, (String) l[0]);
                ps.setString(3, (String) l[1]);
                ps.setInt(4, (Integer) l[4]);
                ps.setString(5, (String) l[5]);
                ps.setString(6, issued.toString());
                ps.setString(7, due.toString());
                ps.setString(8, returned == null ? null : returned.toString());
                ps.setDouble(9, late * finePerDay);
                ps.setInt(10, i == 3 ? 1 : 0); // first late loan is paid, the second one is still unpaid
                ps.executeUpdate();
            }
        }
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("UPDATE books SET available_copies = total_copies -"
                    + " (SELECT COUNT(*) FROM loans WHERE loans.book_id = books.book_id AND return_date IS NULL)");
        }
        ServerLog.info("Sample data inserted (14 books, 6 students - PIN 1234, 16 loans)");
    }
}
