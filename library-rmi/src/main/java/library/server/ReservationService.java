package library.server;

import library.common.Book;
import library.common.LibraryEvent.Type;
import library.common.Reservation;
import library.common.StudentSession;

import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.List;

import static library.server.ServiceSupport.log;
import static library.server.ServiceSupport.read;
import static library.server.ServiceSupport.write;

/**
 * Reservations with a waiting queue per book.
 * Status: WAITING -> READY (a returned copy is held for the student) -> FULFILLED, or CANCELLED.
 */
class ReservationService {

    private final Database db;
    private final BookDao books;
    private final LoanDao loans;
    private final ReservationDao reservations;
    private final EventBus events;

    ReservationService(Database db, BookDao books, LoanDao loans, ReservationDao reservations, EventBus events) {
        this.db = db;
        this.books = books;
        this.loans = loans;
        this.reservations = reservations;
        this.events = events;
    }

    String reserve(StudentSession s, String bookId) {
        return write(db, () -> {
            Book book = bookId == null ? null : books.find(bookId.trim());
            if (book == null) return log("reserveBook", "Failure: no book with ID " + bookId);
            if (book.getAvailableCopies() > 0) {
                return log("reserveBook", "Failure: a copy is available now - ask the librarian to issue it to you");
            }
            if (loans.hasActiveLoan(s.getStudentId(), book.getBookId())) {
                return log("reserveBook", "Failure: you already have this book");
            }
            if (reservations.findOpen(book.getBookId(), s.getStudentId()) != null) {
                return log("reserveBook", "Failure: you already reserved this book");
            }
            reservations.insert(book.getBookId(), s.getStudentId());
            Reservation r = reservations.findOpen(book.getBookId(), s.getStudentId());
            events.publish(Type.RESERVATION_PLACED, s.getName() + " reserved '" + book.getTitle() + "'",
                    book.getBookId(), s.getStudentId());
            return log("reserveBook", "Success: '" + book.getTitle() + "' reserved - you are number "
                    + r.getQueuePosition() + " in the queue");
        });
    }

    /** @param studentCaller the student cancelling their own reservation, or null for staff */
    String cancel(StudentSession studentCaller, int reservationId) {
        return write(db, () -> {
            Reservation r = reservations.find(reservationId);
            if (r == null || !isOpen(r)) return log("cancelReservation", "Failure: no open reservation #" + reservationId);
            if (studentCaller != null && !r.getStudentId().equals(studentCaller.getStudentId())) {
                return log("cancelReservation", "Failure: this is not your reservation");
            }
            cancelInternal(r);
            return log("cancelReservation", "Success: reservation #" + reservationId + " cancelled");
        });
    }

    /** Cancels all open reservations of a student. Must be called while holding the database lock. */
    void cancelAllOf(String studentId) throws SQLException {
        for (Reservation r : reservations.byStudent(studentId)) {
            if (isOpen(r)) cancelInternal(r);
        }
    }

    /** Cancels a reservation; a held copy goes to the next student in the queue or back to the shelf. */
    private void cancelInternal(Reservation r) throws SQLException {
        reservations.setStatus(r.getId(), Reservation.CANCELLED);
        if (Reservation.READY.equals(r.getStatus())) {
            Reservation next = reservations.firstWaiting(r.getBookId());
            if (next != null) {
                reservations.setStatus(next.getId(), Reservation.READY);
                events.publish(Type.RESERVATION_READY, "'" + r.getBookTitle() + "' is ready for pickup by "
                        + next.getStudentName(), r.getBookId(), next.getStudentId());
            } else {
                books.changeAvailable(r.getBookId(), +1);
            }
        }
        events.publish(Type.RESERVATION_CANCELLED, "Reservation of '" + r.getBookTitle() + "' by "
                + r.getStudentName() + " cancelled", r.getBookId(), r.getStudentId());
    }

    List<Reservation> open() throws RemoteException {
        return read(db, reservations::open);
    }

    List<Reservation> byStudent(String studentId) throws RemoteException {
        return read(db, () -> reservations.byStudent(studentId));
    }

    private static boolean isOpen(Reservation r) {
        return Reservation.WAITING.equals(r.getStatus()) || Reservation.READY.equals(r.getStatus());
    }
}
