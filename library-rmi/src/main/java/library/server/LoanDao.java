package library.server;

import library.common.Loan;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Data access for loans = the issued-book records (a separate table from the books).
 */
public class LoanDao {

    private static final String SELECT = "SELECT l.id, l.book_id, COALESCE(b.title, l.book_title), l.student_id,"
            + " COALESCE(s.name, '(deleted)'), l.issued_by_employee, l.issued_by_name, l.issue_date, l.due_date,"
            + " l.return_date, l.renew_count, l.fine, l.fine_paid"
            + " FROM loans l LEFT JOIN books b ON b.book_id = l.book_id LEFT JOIN students s ON s.student_id = l.student_id ";

    private final Connection conn;

    public LoanDao(Database db) {
        this.conn = db.conn();
    }

    public int insert(String bookId, String bookTitle, String studentId, int employeeId, String employeeName,
                      LocalDate issue, LocalDate due) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO loans (book_id,book_title,student_id,issued_by_employee,issued_by_name,issue_date,due_date)"
                        + " VALUES (?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, bookId);
            ps.setString(2, bookTitle);
            ps.setString(3, studentId);
            ps.setInt(4, employeeId);
            ps.setString(5, employeeName);
            ps.setString(6, issue.toString());
            ps.setString(7, due.toString());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        }
    }

    public Loan find(int id) throws SQLException {
        List<Loan> l = query("WHERE l.id = ?", id);
        return l.isEmpty() ? null : l.get(0);
    }

    public List<Loan> all(boolean activeOnly) throws SQLException {
        return query(activeOnly ? "WHERE l.return_date IS NULL ORDER BY l.due_date" : "ORDER BY l.id DESC", null);
    }

    public List<Loan> byStudent(String studentId) throws SQLException {
        return query("WHERE l.student_id = ? ORDER BY l.return_date IS NOT NULL, l.id DESC", studentId);
    }

    public int activeCount(String studentId) throws SQLException {
        return count("SELECT COUNT(*) FROM loans WHERE student_id = ? AND return_date IS NULL", studentId);
    }

    public int activeCountForBook(String bookId) throws SQLException {
        return count("SELECT COUNT(*) FROM loans WHERE book_id = ? AND return_date IS NULL", bookId);
    }

    public boolean hasActiveLoan(String studentId, String bookId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT 1 FROM loans WHERE student_id = ? AND book_id = ? AND return_date IS NULL")) {
            ps.setString(1, studentId);
            ps.setString(2, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Unpaid fines of returned loans + overdue open loans (blocks new borrowing). */
    public boolean hasUnpaidFinesOrOverdue(String studentId) throws SQLException {
        return count("SELECT COUNT(*) FROM loans WHERE student_id = ? AND ((return_date IS NOT NULL AND fine > 0 AND fine_paid = 0)"
                + " OR (return_date IS NULL AND date('now','localtime') > due_date))", studentId) > 0;
    }

    public void markReturned(int id, LocalDate date, double fine) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE loans SET return_date = ?, fine = ? WHERE id = ?")) {
            ps.setString(1, date.toString());
            ps.setDouble(2, fine);
            ps.setInt(3, id);
            ps.executeUpdate();
        }
    }

    public void renew(int id, LocalDate newDue) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE loans SET due_date = ?, renew_count = renew_count + 1, overdue_notified = 0 WHERE id = ?")) {
            ps.setString(1, newDue.toString());
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public void markFinePaid(int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE loans SET fine_paid = 1 WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /** Overdue loans that have not been announced yet; marks them as announced. */
    public List<Loan> takeNewOverdue() throws SQLException {
        List<Loan> list = query("WHERE l.return_date IS NULL AND l.overdue_notified = 0"
                + " AND date('now','localtime') > l.due_date", null);
        try (PreparedStatement ps = conn.prepareStatement("UPDATE loans SET overdue_notified = 1 WHERE id = ?")) {
            for (Loan l : list) {
                ps.setInt(1, l.getId());
                ps.executeUpdate();
            }
        }
        return list;
    }

    private int count(String sql, String param) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private List<Loan> query(String tail, Object param) throws SQLException {
        List<Loan> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SELECT + tail)) {
            if (param != null) ps.setObject(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Loan l = new Loan();
                    l.setId(rs.getInt(1));
                    l.setBookId(rs.getString(2));
                    l.setBookTitle(rs.getString(3));
                    l.setStudentId(rs.getString(4));
                    l.setStudentName(rs.getString(5));
                    l.setIssuedByEmployeeId(rs.getInt(6));
                    l.setIssuedByName(rs.getString(7));
                    l.setIssueDate(date(rs.getString(8)));
                    l.setDueDate(date(rs.getString(9)));
                    l.setReturnDate(date(rs.getString(10)));
                    l.setRenewCount(rs.getInt(11));
                    l.setFine(rs.getDouble(12));
                    l.setFinePaid(rs.getInt(13) == 1);
                    list.add(l);
                }
            }
        }
        return list;
    }

    private static LocalDate date(String s) {
        return s == null ? null : LocalDate.parse(s);
    }
}
