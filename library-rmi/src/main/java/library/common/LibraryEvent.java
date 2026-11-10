package library.common;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * An event pushed by the server to every registered client through RMI callbacks.
 */
public class LibraryEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Type {
        BOOK_ADDED, BOOK_UPDATED, BOOK_DELETED, BOOK_ISSUED, BOOK_RETURNED, LOAN_RENEWED,
        RESERVATION_PLACED, RESERVATION_READY, RESERVATION_CANCELLED, OVERDUE, STUDENT_REGISTERED
    }

    private final long id;
    private final Type type;
    private final String message;
    private final String bookId;
    private final String studentId;
    private final LocalDateTime time;

    public LibraryEvent(long id, Type type, String message, String bookId, String studentId, LocalDateTime time) {
        this.id = id;
        this.type = type;
        this.message = message;
        this.bookId = bookId;
        this.studentId = studentId;
        this.time = time;
    }

    public long getId() {
        return id;
    }

    public Type getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public String getBookId() {
        return bookId;
    }

    public String getStudentId() {
        return studentId;
    }

    public LocalDateTime getTime() {
        return time;
    }

    @Override
    public String toString() {
        return "[" + type + "] " + message;
    }
}
