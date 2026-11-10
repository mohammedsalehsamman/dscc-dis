package library.server;

import dscc.util.AppConfig;
import library.common.LibraryStats;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Aggregate queries (GROUP BY) for the dashboards and reports.
 */
public class StatsDao {

    private final Connection conn;

    public StatsDao(Database db) {
        this.conn = db.conn();
    }

    public LibraryStats load() throws SQLException {
        LibraryStats s = new LibraryStats();
        double finePerDay = AppConfig.getDouble(AppConfig.FINE_PER_DAY);
        String firstOfMonth = LocalDate.now().withDayOfMonth(1).toString();

        s.titles = intQuery("SELECT COUNT(*) FROM books");
        s.totalCopies = intQuery("SELECT COALESCE(SUM(total_copies),0) FROM books");
        s.availableCopies = intQuery("SELECT COALESCE(SUM(available_copies),0) FROM books");
        s.activeLoans = intQuery("SELECT COUNT(*) FROM loans WHERE return_date IS NULL");
        s.overdueLoans = intQuery("SELECT COUNT(*) FROM loans WHERE return_date IS NULL AND date('now','localtime') > due_date");
        s.students = intQuery("SELECT COUNT(*) FROM students");
        s.waitingReservations = intQuery("SELECT COUNT(*) FROM reservations WHERE status IN ('WAITING','READY')");
        s.loansThisMonth = intQuery("SELECT COUNT(*) FROM loans WHERE issue_date >= '" + firstOfMonth + "'");
        s.finesCollected = doubleQuery("SELECT COALESCE(SUM(fine),0) FROM loans WHERE fine_paid = 1");
        double unpaid = doubleQuery("SELECT COALESCE(SUM(fine),0) FROM loans WHERE return_date IS NOT NULL AND fine_paid = 0");
        double lateDays = doubleQuery("SELECT COALESCE(SUM(julianday(date('now','localtime')) - julianday(due_date)),0)"
                + " FROM loans WHERE return_date IS NULL AND date('now','localtime') > due_date");
        s.finesPending = unpaid + lateDays * finePerDay;

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COALESCE(b.title, l.book_title), COUNT(*) c FROM loans l LEFT JOIN books b ON b.book_id = l.book_id"
                        + " GROUP BY l.book_id ORDER BY c DESC, 1 LIMIT 5");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) s.topBooks.put(rs.getString(1), rs.getInt(2));
        }

        YearMonth now = YearMonth.now();
        for (int i = 5; i >= 0; i--) s.loansPerMonth.put(now.minusMonths(i).toString(), 0);
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT SUBSTR(issue_date,1,7), COUNT(*) FROM loans GROUP BY 1");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                if (s.loansPerMonth.containsKey(rs.getString(1))) s.loansPerMonth.put(rs.getString(1), rs.getInt(2));
            }
        }

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COALESCE(NULLIF(category,''),'General'), COUNT(*) FROM books GROUP BY 1 ORDER BY 2 DESC, 1");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) s.titlesPerCategory.put(rs.getString(1), rs.getInt(2));
        }

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT issued_by_employee, MAX(issued_by_name), COUNT(*) FROM loans"
                        + " GROUP BY issued_by_employee ORDER BY 3 DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                s.loansPerEmployeeId.put(rs.getInt(1), rs.getInt(3));
                s.loansPerEmployeeName.put(rs.getString(2), rs.getInt(3));
            }
        }
        return s;
    }

    private int intQuery(String sql) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private double doubleQuery(String sql) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getDouble(1) : 0;
        }
    }
}
