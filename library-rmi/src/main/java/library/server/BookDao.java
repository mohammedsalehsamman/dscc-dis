package library.server;

import library.common.Book;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data access for the books table (HashMap/ArrayList of the first version replaced by SQLite).
 */
public class BookDao {

    private static final String COLS = "book_id,title,author,category,isbn,year,total_copies,available_copies";
    private final Connection conn;

    public BookDao(Database db) {
        this.conn = db.conn();
    }

    public Book find(String bookId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT " + COLS + " FROM books WHERE book_id = ?")) {
            ps.setString(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public void insert(Book b) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO books (" + COLS + ") VALUES (?,?,?,?,?,?,?,?)")) {
            ps.setString(1, b.getBookId());
            ps.setString(2, b.getTitle());
            ps.setString(3, b.getAuthor());
            ps.setString(4, b.getCategory());
            ps.setString(5, b.getIsbn());
            ps.setInt(6, b.getYear());
            ps.setInt(7, b.getTotalCopies());
            ps.setInt(8, b.getAvailableCopies());
            ps.executeUpdate();
        }
    }

    public void update(Book b) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE books SET title=?, author=?, category=?, isbn=?, year=?, total_copies=?, available_copies=? WHERE book_id=?")) {
            ps.setString(1, b.getTitle());
            ps.setString(2, b.getAuthor());
            ps.setString(3, b.getCategory());
            ps.setString(4, b.getIsbn());
            ps.setInt(5, b.getYear());
            ps.setInt(6, b.getTotalCopies());
            ps.setInt(7, b.getAvailableCopies());
            ps.setString(8, b.getBookId());
            ps.executeUpdate();
        }
    }

    public void changeAvailable(String bookId, int delta) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE books SET available_copies = available_copies + ? WHERE book_id = ?")) {
            ps.setInt(1, delta);
            ps.setString(2, bookId);
            ps.executeUpdate();
        }
    }

    public boolean delete(String bookId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM books WHERE book_id = ?")) {
            ps.setString(1, bookId);
            return ps.executeUpdate() > 0;
        }
    }

    /** Case-insensitive search in id/title/author/isbn with optional category and availability filters. */
    public List<Book> search(String query, String category, boolean availableOnly) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT " + COLS + " FROM books WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (query != null && !query.trim().isEmpty()) {
            sql.append(" AND (title LIKE ? OR author LIKE ? OR isbn LIKE ? OR book_id LIKE ?)");
            String like = "%" + query.trim() + "%";
            for (int i = 0; i < 4; i++) params.add(like);
        }
        if (category != null && !category.trim().isEmpty()) {
            sql.append(" AND category = ?");
            params.add(category.trim());
        }
        if (availableOnly) sql.append(" AND available_copies > 0");
        sql.append(" ORDER BY CAST(SUBSTR(book_id, 2) AS INTEGER), book_id");
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            List<Book> list = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
            return list;
        }
    }

    /** Exact title matches first, then partial matches (used by the original searchBook). */
    public List<Book> searchByTitle(String title) throws SQLException {
        List<Book> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT " + COLS + " FROM books WHERE title LIKE ?"
                + " ORDER BY CASE WHEN LOWER(title) = LOWER(?) THEN 0 ELSE 1 END, title")) {
            ps.setString(1, "%" + title.trim() + "%");
            ps.setString(2, title.trim());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    public List<String> categories() throws SQLException {
        List<String> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT DISTINCT category FROM books WHERE category IS NOT NULL AND category <> '' ORDER BY category");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(rs.getString(1));
        }
        return list;
    }

    private static Book map(ResultSet rs) throws SQLException {
        Book b = new Book(rs.getString(1), rs.getString(2), rs.getString(3));
        b.setCategory(rs.getString(4));
        b.setIsbn(rs.getString(5));
        b.setYear(rs.getInt(6));
        b.setTotalCopies(rs.getInt(7));
        b.setAvailableCopies(rs.getInt(8));
        return b;
    }
}
