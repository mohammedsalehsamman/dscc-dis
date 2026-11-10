package library.server;

import dscc.util.AppConfig;
import dscc.util.Passwords;
import library.common.Student;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data access for students (library members). PINs are stored as salted PBKDF2 hashes (see dscc.util.Passwords).
 */
public class StudentDao {

    private final Connection conn;

    public StudentDao(Database db) {
        this.conn = db.conn();
    }

    public Student find(String studentId) throws SQLException {
        for (Student s : query("WHERE s.student_id = ?", studentId)) return s;
        return null;
    }

    public List<Student> list() throws SQLException {
        return query("", null);
    }

    /** Checks a PIN; a hash stored in an older format is upgraded to the current one on success. */
    public boolean checkPin(String studentId, String pin) throws SQLException {
        String storedHash;
        try (PreparedStatement ps = conn.prepareStatement("SELECT pin_hash, salt FROM students WHERE student_id = ?")) {
            ps.setString(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return false;
                storedHash = rs.getString(1);
                if (!Passwords.verify(pin, rs.getString(2), storedHash)) return false;
            }
        }
        if (Passwords.needsRehash(storedHash)) setPin(studentId, pin);
        return true;
    }

    public void insert(Student s, String pin) throws SQLException {
        String salt = Passwords.newSalt();
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO students (student_id,name,email,phone,pin_hash,salt) VALUES (?,?,?,?,?,?)")) {
            ps.setString(1, s.getStudentId());
            ps.setString(2, s.getName());
            ps.setString(3, s.getEmail());
            ps.setString(4, s.getPhone());
            ps.setString(5, Passwords.hash(pin, salt));
            ps.setString(6, salt);
            ps.executeUpdate();
        }
    }

    public void update(Student s) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE students SET name=?, email=?, phone=? WHERE student_id=?")) {
            ps.setString(1, s.getName());
            ps.setString(2, s.getEmail());
            ps.setString(3, s.getPhone());
            ps.setString(4, s.getStudentId());
            ps.executeUpdate();
        }
    }

    public void setPin(String studentId, String pin) throws SQLException {
        String salt = Passwords.newSalt();
        try (PreparedStatement ps = conn.prepareStatement("UPDATE students SET pin_hash=?, salt=? WHERE student_id=?")) {
            ps.setString(1, Passwords.hash(pin, salt));
            ps.setString(2, salt);
            ps.setString(3, studentId);
            ps.executeUpdate();
        }
    }

    public boolean delete(String studentId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM students WHERE student_id = ?")) {
            ps.setString(1, studentId);
            return ps.executeUpdate() > 0;
        }
    }

    private List<Student> query(String where, String param) throws SQLException {
        double finePerDay = AppConfig.getDouble(AppConfig.FINE_PER_DAY);
        String sql = "SELECT s.student_id, s.name, s.email, s.phone,"
                + " (SELECT COUNT(*) FROM loans l WHERE l.student_id = s.student_id AND l.return_date IS NULL),"
                + " (SELECT COALESCE(SUM(fine),0) FROM loans l WHERE l.student_id = s.student_id AND l.return_date IS NOT NULL AND fine_paid = 0),"
                + " (SELECT COALESCE(SUM(julianday(date('now','localtime')) - julianday(due_date)),0) FROM loans l"
                + "    WHERE l.student_id = s.student_id AND l.return_date IS NULL AND date('now','localtime') > due_date)"
                + " FROM students s " + where + " ORDER BY s.student_id";
        List<Student> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (param != null) ps.setString(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Student s = new Student(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4));
                    s.setActiveLoans(rs.getInt(5));
                    s.setUnpaidFines(rs.getDouble(6) + Math.floor(rs.getDouble(7)) * finePerDay);
                    list.add(s);
                }
            }
        }
        return list;
    }
}
