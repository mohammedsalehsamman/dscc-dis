package library.common;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * A reservation (hold) placed by a student on a book with no available copies.
 * Status: WAITING -> READY (a copy was returned and is held) -> FULFILLED (issued) or CANCELLED.
 */
public class Reservation implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String WAITING = "WAITING";
    public static final String READY = "READY";
    public static final String FULFILLED = "FULFILLED";
    public static final String CANCELLED = "CANCELLED";

    private int id;
    private String bookId;
    private String bookTitle;
    private String studentId;
    private String studentName;
    private LocalDateTime createdAt;
    private String status;
    private int queuePosition;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getBookId() {
        return bookId;
    }

    public void setBookId(String bookId) {
        this.bookId = bookId;
    }

    public String getBookTitle() {
        return bookTitle;
    }

    public void setBookTitle(String bookTitle) {
        this.bookTitle = bookTitle;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getQueuePosition() {
        return queuePosition;
    }

    public void setQueuePosition(int queuePosition) {
        this.queuePosition = queuePosition;
    }
}
