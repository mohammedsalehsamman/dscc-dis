package library.server;

import dscc.util.AppConfig;
import dscc.util.ServerLog;
import library.common.Book;
import library.common.LibraryEvent.Type;
import library.common.Loan;
import library.common.Reservation;
import library.common.StaffInfo;
import library.common.Student;
import library.common.StudentSession;

import java.rmi.RemoteException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static library.server.ServiceSupport.blank;
import static library.server.ServiceSupport.log;
import static library.server.ServiceSupport.money;
import static library.server.ServiceSupport.read;
import static library.server.ServiceSupport.write;

/**
 * Loans (the issued-book records): issue, return with late fines, renewals and fine payment.
 * Rules come from config.properties: loan.days, loan.maxPerStudent, loan.maxRenewals, fine.perDay.
 */
class LoanService {

    private final Database db;
    private final BookDao books;
    private final StudentDao students;
    private final LoanDao loans;
    private final ReservationDao reservations;
    private final EventBus events;

    private final int loanDays = AppConfig.getInt(AppConfig.LOAN_DAYS);
    private final int maxLoans = AppConfig.getInt(AppConfig.MAX_LOANS);
    private final int maxRenewals = AppConfig.getInt(AppConfig.MAX_RENEWALS);
    private final double finePerDay = AppConfig.getDouble(AppConfig.FINE_PER_DAY);

    LoanService(Database db, BookDao books, StudentDao students, LoanDao loans, ReservationDao reservations,
                EventBus events) {
        this.db = db;
        this.books = books;
        this.students = students;
        this.loans = loans;
        this.reservations = reservations;
        this.events = events;
    }

    String issue(StaffInfo staff, String bookId, String studentId) {
        if (blank(bookId) || blank(studentId)) return log("issueBook", "Failure: book ID and student ID are required");
        return write(db, () -> {
            Book book = books.find(bookId.trim());
            if (book == null) return log("issueBook", "Failure: no book with ID " + bookId.trim());
            Student student = students.find(studentId.trim().toUpperCase());
            if (student == null) return log("issueBook", "Failure: no student with ID " + studentId.trim());
            if (loans.hasActiveLoan(student.getStudentId(), book.getBookId())) {
                return log("issueBook", "Failure: " + student.getName() + " already has this book");
            }
            if (student.getActiveLoans() >= maxLoans) {
                return log("issueBook", "Failure: " + student.getName() + " already has " + maxLoans + " books (maximum)");
            }
            if (loans.hasUnpaidFinesOrOverdue(student.getStudentId())) {
                return log("issueBook", "Failure: " + student.getName() + " has overdue books or unpaid fines");
            }
            Reservation mine = reservations.findOpen(book.getBookId(), student.getStudentId());
            if (mine != null && Reservation.READY.equals(mine.getStatus())) {
                reservations.setStatus(mine.getId(), Reservation.FULFILLED); // uses the copy held for this student
            } else {
                if (book.getAvailableCopies() <= 0) {
                    return log("issueBook", "Failure: no copy of '" + book.getTitle()
                            + "' is available - the student can reserve it");
                }
                books.changeAvailable(book.getBookId(), -1);
                if (mine != null) reservations.setStatus(mine.getId(), Reservation.FULFILLED);
            }
            LocalDate today = LocalDate.now();
            LocalDate due = today.plusDays(loanDays);
            int id = loans.insert(book.getBookId(), book.getTitle(), student.getStudentId(),
                    staff.getEmployeeId(), staff.getName(), today, due);
            events.publish(Type.BOOK_ISSUED, "'" + book.getTitle() + "' issued to " + student.getName()
                    + " by " + staff.getName() + " (due " + due + ")", book.getBookId(), student.getStudentId());
            return log("issueBook", "Success: '" + book.getTitle() + "' issued to " + student.getName()
                    + " - loan #" + id + ", due " + due);
        });
    }

    String returnBook(StaffInfo staff, int loanId) {
        return write(db, () -> {
            Loan loan = loans.find(loanId);
            if (loan == null) return log("returnBook", "Failure: loan #" + loanId + " not found");
            if (loan.isReturned()) return log("returnBook", "Failure: loan #" + loanId + " was already returned");
            LocalDate today = LocalDate.now();
            long late = Math.max(0, today.toEpochDay() - loan.getDueDate().toEpochDay());
            double fine = late * finePerDay;
            loans.markReturned(loanId, today, fine);

            Reservation next = reservations.firstWaiting(loan.getBookId());
            if (next != null) {
                reservations.setStatus(next.getId(), Reservation.READY); // copy is held for the next student
                events.publish(Type.RESERVATION_READY, "'" + loan.getBookTitle() + "' is ready for pickup by "
                        + next.getStudentName(), loan.getBookId(), next.getStudentId());
            } else {
                books.changeAvailable(loan.getBookId(), +1);
            }
            events.publish(Type.BOOK_RETURNED, "'" + loan.getBookTitle() + "' returned by " + loan.getStudentName()
                    + (late > 0 ? " - " + late + " day(s) late" : ""), loan.getBookId(), loan.getStudentId());
            return log("returnBook", "Success: loan #" + loanId + " returned (received by " + staff.getName() + ")"
                    + (late > 0 ? " - LATE by " + late + " day(s), fine " + money(fine) : " on time"));
        });
    }

    /**
     * @param studentCaller the student renewing their own loan, or null when staff renews it
     * @param who           name of the caller, for the event message
     */
    String renew(StudentSession studentCaller, String who, int loanId) {
        return write(db, () -> {
            Loan loan = loans.find(loanId);
            if (loan == null || loan.isReturned()) return log("renewLoan", "Failure: no open loan #" + loanId);
            if (studentCaller != null && !loan.getStudentId().equals(studentCaller.getStudentId())) {
                return log("renewLoan", "Failure: this is not your loan");
            }
            if (loan.isOverdue()) return log("renewLoan", "Failure: overdue loans cannot be renewed - please return the book");
            if (loan.getRenewCount() >= maxRenewals) {
                return log("renewLoan", "Failure: maximum " + maxRenewals + " renewals reached");
            }
            if (reservations.firstWaiting(loan.getBookId()) != null) {
                return log("renewLoan", "Failure: other students are waiting for this book");
            }
            LocalDate newDue = loan.getDueDate().plusDays(loanDays);
            loans.renew(loanId, newDue);
            events.publish(Type.LOAN_RENEWED, "'" + loan.getBookTitle() + "' renewed for " + loan.getStudentName()
                    + " by " + who + " (new due " + newDue + ")", loan.getBookId(), loan.getStudentId());
            return log("renewLoan", "Success: loan #" + loanId + " renewed, new due date " + newDue);
        });
    }

    String payFine(StaffInfo staff, int loanId) {
        return write(db, () -> {
            Loan loan = loans.find(loanId);
            if (loan == null) return log("payFine", "Failure: loan #" + loanId + " not found");
            if (!loan.isReturned()) return log("payFine", "Failure: return the book first, then the fine is calculated");
            if (loan.getFine() <= 0) return log("payFine", "Failure: this loan has no fine");
            if (loan.isFinePaid()) return log("payFine", "Failure: fine already paid");
            loans.markFinePaid(loanId);
            return log("payFine", "Success: fine of " + money(loan.getFine()) + " paid by " + loan.getStudentName()
                    + " (received by " + staff.getName() + ")");
        });
    }

    List<Loan> list(boolean activeOnly) throws RemoteException {
        return read(db, () -> loans.all(activeOnly));
    }

    List<Loan> byStudent(String studentId) throws RemoteException {
        return read(db, () -> loans.byStudent(studentId));
    }

    /** The "issued history" required by the assignment: every loan as a line of text. */
    List<String> issuedHistory() throws RemoteException {
        return read(db, () -> {
            List<String> list = new ArrayList<>();
            for (Loan l : loans.all(false)) list.add(l.toString());
            return list;
        });
    }

    /** Called every minute by the server's scheduler: announces loans that just became overdue. */
    void checkOverdue() {
        synchronized (db) {
            try {
                for (Loan l : loans.takeNewOverdue()) {
                    events.publish(Type.OVERDUE, "OVERDUE: '" + l.getBookTitle() + "' borrowed by " + l.getStudentName()
                            + " was due on " + l.getDueDate() + " (loan #" + l.getId() + ")", l.getBookId(), l.getStudentId());
                }
            } catch (SQLException e) {
                ServerLog.info("Overdue check failed: " + e.getMessage());
            }
        }
    }
}
