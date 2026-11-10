package library.server;

import library.common.Reservation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data access for reservations (holds). Queue order = creation order.
 */
public class ReservationDao {

    private static final String SELECT = "SELECT r.id, r.book_id, COALESCE(b.title,'(deleted)'), r.student_id,"
            + " COALESCE(s.name,'(deleted)'), r.created_at, r.status,"
            + " (SELECT COUNT(*) FROM reservations q WHERE q.book_id = r.book_id AND q.status = 'WAITING' AND q.id <= r.id)"
            + " FROM reservations r LEFT JOIN books b ON b.book_id = r.book_id"
            + " LEFT JOIN students s ON s.student_id = r.student_id ";

    private final Connection conn;

    public ReservationDao(Database db) {
        this.conn = db.conn();
    }

    public void insert(String bookId, String studentId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO reservations (book_id, student_id, created_at, status) VALUES (?,?,?,'WAITING')")) {
            ps.setString(1, bookId);
            ps.setString(2, studentId);
            ps.setString(3, LocalDateTime.now().withNano(0).toString());
            ps.executeUpdate();
        }
    }

    public Reservation find(int id) throws SQLException {
        List<Reservation> l = query("WHERE r.id = ?", id);
        return l.isEmpty() ? null : l.get(0);
    }

    /** Open (WAITING or READY) reservation of a student for a book, or null. */
    public Reservation findOpen(String bookId, String studentId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT
                + "WHERE r.book_id = ? AND r.student_id = ? AND r.status IN ('WAITING','READY')")) {
            ps.setString(1, bookId);
            ps.setString(2, studentId);
            List<Reservation> l = read(ps);
            return l.isEmpty() ? null : l.get(0);
        }
    }

    /** First WAITING reservation in the queue of a book, or null. */
    public Reservation firstWaiting(String bookId) throws SQLException {
        List<Reservation> l = query("WHERE r.book_id = ? AND r.status = 'WAITING' ORDER BY r.id LIMIT 1", bookId);
        return l.isEmpty() ? null : l.get(0);
    }

    public int readyCount(String bookId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM reservations WHERE book_id = ? AND status = 'READY'")) {
            ps.setString(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public void setStatus(int id, String status) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE reservations SET status = ? WHERE id = ?")) {
            ps.setString(1, status);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public List<Reservation> open() throws SQLException {
        return query("WHERE r.status IN ('WAITING','READY') ORDER BY r.book_id, r.id", null);
    }

    public List<Reservation> byStudent(String studentId) throws SQLException {
        return query("WHERE r.student_id = ? ORDER BY r.status NOT IN ('WAITING','READY'), r.id DESC", studentId);
    }

    public int waitingCount() throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM reservations WHERE status IN ('WAITING','READY')");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private List<Reservation> query(String tail, Object param) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT + tail)) {
            if (param != null) ps.setObject(1, param);
            return read(ps);
        }
    }

    private static List<Reservation> read(PreparedStatement ps) throws SQLException {
        List<Reservation> list = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Reservation r = new Reservation();
                r.setId(rs.getInt(1));
                r.setBookId(rs.getString(2));
                r.setBookTitle(rs.getString(3));
                r.setStudentId(rs.getString(4));
                r.setStudentName(rs.getString(5));
                r.setCreatedAt(LocalDateTime.parse(rs.getString(6)));
                r.setStatus(rs.getString(7));
                r.setQueuePosition(Reservation.WAITING.equals(r.getStatus()) ? rs.getInt(8) : 0);
                list.add(r);
            }
        }
        return list;
    }
}
