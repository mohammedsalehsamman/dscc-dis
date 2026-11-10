package library.server;

import library.common.AccessDeniedException;
import library.common.Book;
import library.common.LibraryEvent;
import library.common.Loan;
import library.common.Reservation;
import library.common.StudentSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.rmi.server.UnicastRemoteObject;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static library.server.FakeStaffVerifier.EMPLOYEE;
import static library.server.FakeStaffVerifier.LIBRARIAN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Library rules, tested on the real service and a fresh SQLite database with the sample data.
 * Sample data used here (dates are relative to today):
 * <ul>
 *   <li>S1002 has B1 on loan, issued 20 days ago, so 6 days overdue (loan period 14 days)</li>
 *   <li>S1003 has B10 on loan; B10 has a single copy</li>
 *   <li>S1004 has no open loans and no fines</li>
 * </ul>
 */
class LibraryServiceTest {

    @TempDir
    Path dir;

    private Database db;
    private LibraryServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        db = new Database(dir.resolve("library.db").toString());
        service = new LibraryServiceImpl(db, new FakeStaffVerifier());
    }

    @AfterEach
    void tearDown() throws Exception {
        UnicastRemoteObject.unexportObject(service, true);
        db.close();
    }

    // ------------------------------------------------------------ operations required by the assignment

    @Test
    void addBookAndSearchBook() throws Exception {
        assertTrue(service.addBook("B100", "Effective Java", "Joshua Bloch").startsWith("Success"));
        assertTrue(service.addBook("B100", "Again", "Someone").contains("already exists"));
        assertTrue(service.addBook(" ", "Title", "Author").startsWith("Failure"));

        assertTrue(service.searchBook("effective").contains("Effective Java"), "search ignores case");
        assertEquals("Book not found", service.searchBook("no such title"));
        assertEquals("Book not found", service.searchBook(""));
    }

    @Test
    void viewAllBooksAndIssuedHistory() throws Exception {
        assertEquals(14, service.viewAllBooks().size());
        assertEquals(16, service.viewIssuedHistory().size());
    }

    // ------------------------------------------------------------ staff authorization

    @Test
    void staffOperationsNeedAValidLibrarianToken() throws Exception {
        assertTrue(service.issueBook("bad-token", "B3", "S1004").contains("invalid or expired"));
        assertTrue(service.issueBook(EMPLOYEE, "B3", "S1004").contains("role EMPLOYEE"));
        assertTrue(service.deleteBook(null, "B3").startsWith("Failure"));
        assertThrows(AccessDeniedException.class, () -> service.listStudents(EMPLOYEE));
        assertThrows(AccessDeniedException.class, () -> service.listLoans("bad-token", true));
    }

    @Test
    void studentLoginChecksThePin() throws Exception {
        StudentSession s = service.studentLogin("s1004", "1234");
        assertEquals("S1004", s.getStudentId(), "student id is case-insensitive");
        assertThrows(AccessDeniedException.class, () -> service.studentLogin("S1004", "9999"));
        assertThrows(AccessDeniedException.class, () -> service.studentLogin("S9999", "1234"));
        assertThrows(AccessDeniedException.class, () -> service.getMyLoans("not-a-session"));
    }

    // ------------------------------------------------------------ loans

    @Test
    void issuingABookUsesACopyAndReturningGivesItBack() throws Exception {
        int before = service.getBook("B3").getAvailableCopies();
        assertTrue(service.issueBook(LIBRARIAN, "B3", "S1004").startsWith("Success"));
        assertEquals(before - 1, service.getBook("B3").getAvailableCopies());
        assertTrue(service.issueBook(LIBRARIAN, "B3", "S1004").contains("already has this book"));

        Loan loan = openLoan("S1004", "B3");
        assertEquals(LocalDate.now().plusDays(14), loan.getDueDate());
        assertEquals("Lina Mahmoud", loan.getIssuedByName(), "the verified employee is stored in the loan");

        assertTrue(service.returnBook(LIBRARIAN, loan.getId()).contains("on time"));
        assertEquals(before, service.getBook("B3").getAvailableCopies());
        assertTrue(service.returnBook(LIBRARIAN, loan.getId()).contains("already returned"));
    }

    @Test
    void studentWithAnOverdueBookCannotBorrow() throws Exception {
        String result = service.issueBook(LIBRARIAN, "B3", "S1002");
        assertTrue(result.contains("overdue books or unpaid fines"), result);
    }

    @Test
    void lateReturnIsFinedAndTheFineBlocksBorrowingUntilPaid() throws Exception {
        Loan loan = openLoan("S1002", "B1");
        long daysLate = LocalDate.now().toEpochDay() - loan.getDueDate().toEpochDay();
        assertEquals(6, daysLate);

        assertTrue(service.returnBook(LIBRARIAN, loan.getId()).contains("LATE by 6 day(s)"));
        Loan returned = service.listLoans(LIBRARIAN, false).stream()
                .filter(l -> l.getId() == loan.getId()).findFirst().orElseThrow();
        assertEquals(6 * 500, returned.getFine(), 0.001);

        assertTrue(service.issueBook(LIBRARIAN, "B3", "S1002").contains("unpaid fines"), "unpaid fine blocks borrowing");

        assertTrue(service.payFine(LIBRARIAN, loan.getId()).startsWith("Success"));
        assertTrue(service.payFine(LIBRARIAN, loan.getId()).contains("already paid"));
        assertTrue(service.issueBook(LIBRARIAN, "B3", "S1002").startsWith("Success"), "can borrow again after paying");
    }

    @Test
    void aLoanCanBeRenewedTwiceAtMost() throws Exception {
        service.issueBook(LIBRARIAN, "B3", "S1004");
        Loan loan = openLoan("S1004", "B3");
        StudentSession owner = service.studentLogin("S1004", "1234");

        assertTrue(service.renewLoan(owner.getToken(), loan.getId()).startsWith("Success"));
        assertTrue(service.renewLoan(LIBRARIAN, loan.getId()).startsWith("Success"));
        assertTrue(service.renewLoan(owner.getToken(), loan.getId()).contains("maximum 2 renewals"));

        assertEquals(LocalDate.now().plusDays(14 * 3), openLoan("S1004", "B3").getDueDate());
    }

    @Test
    void aStudentCannotRenewSomeoneElsesLoan() throws Exception {
        service.issueBook(LIBRARIAN, "B3", "S1004");
        Loan loan = openLoan("S1004", "B3");
        StudentSession other = service.studentLogin("S1001", "1234");
        assertTrue(service.renewLoan(other.getToken(), loan.getId()).contains("not your loan"));
    }

    @Test
    void overdueLoanCannotBeRenewed() throws Exception {
        Loan loan = openLoan("S1002", "B1");
        assertTrue(service.renewLoan(LIBRARIAN, loan.getId()).contains("overdue loans cannot be renewed"));
    }

    // ------------------------------------------------------------ reservations

    @Test
    void returnedCopyIsHeldForTheFirstStudentInTheQueue() throws Exception {
        StudentSession first = service.studentLogin("S1004", "1234");
        StudentSession second = service.studentLogin("S1001", "1234");

        assertTrue(service.reserveBook(first.getToken(), "B10").contains("number 1 in the queue"));
        assertTrue(service.reserveBook(second.getToken(), "B10").contains("number 2 in the queue"));
        assertTrue(service.reserveBook(first.getToken(), "B10").contains("already reserved"));

        service.returnBook(LIBRARIAN, openLoan("S1003", "B10").getId());
        assertEquals(0, service.getBook("B10").getAvailableCopies(), "the copy is held, not put back on the shelf");
        assertEquals(Reservation.READY, reservation(first, "B10").getStatus());
        assertEquals(Reservation.WAITING, reservation(second, "B10").getStatus());

        assertTrue(service.issueBook(LIBRARIAN, "B10", "S1001").contains("no copy"), "second in line must wait");
        assertTrue(service.issueBook(LIBRARIAN, "B10", "S1004").startsWith("Success"));
        assertEquals(Reservation.FULFILLED, reservation(first, "B10").getStatus());
    }

    @Test
    void cancellingAHeldReservationPassesTheCopyToTheNextStudent() throws Exception {
        StudentSession first = service.studentLogin("S1004", "1234");
        StudentSession second = service.studentLogin("S1001", "1234");
        service.reserveBook(first.getToken(), "B10");
        service.reserveBook(second.getToken(), "B10");
        service.returnBook(LIBRARIAN, openLoan("S1003", "B10").getId());

        Reservation held = reservation(first, "B10");
        assertTrue(service.cancelReservation(second.getToken(), held.getId()).contains("not your reservation"));
        assertTrue(service.cancelReservation(first.getToken(), held.getId()).startsWith("Success"));
        assertEquals(Reservation.READY, reservation(second, "B10").getStatus());
    }

    @Test
    void aBookWithFreeCopiesCannotBeReserved() throws Exception {
        StudentSession s = service.studentLogin("S1004", "1234");
        assertTrue(service.reserveBook(s.getToken(), "B3").contains("a copy is available now"));
    }

    // ------------------------------------------------------------ catalog management

    @Test
    void updatingCopiesKeepsBooksOnLoanIntoAccount() throws Exception {
        Book b2 = service.getBook("B2"); // 4 copies, 1 on loan
        b2.setTotalCopies(2);
        assertTrue(service.updateBook(LIBRARIAN, b2).startsWith("Success"));
        assertEquals(1, service.getBook("B2").getAvailableCopies());

        b2.setTotalCopies(0);
        assertTrue(service.updateBook(LIBRARIAN, b2).contains("copies must be between 1 and 1000"));
    }

    @Test
    void aBookOnLoanCannotBeDeleted() throws Exception {
        assertTrue(service.deleteBook(LIBRARIAN, "B1").contains("still on loan"));
        assertTrue(service.deleteBook(LIBRARIAN, "B13").startsWith("Success"));
        assertEquals(null, service.getBook("B13"));
    }

    // ------------------------------------------------------------ real-time events and PIN storage

    @Test
    void changesArePushedToRegisteredCallbacks() throws Exception {
        CountDownLatch received = new CountDownLatch(1);
        service.registerCallback(event -> {
            if (event.getType() == LibraryEvent.Type.BOOK_ISSUED) received.countDown();
        });
        service.issueBook(LIBRARIAN, "B3", "S1004");
        assertTrue(received.await(5, TimeUnit.SECONDS), "callback got the BOOK_ISSUED event");

        List<LibraryEvent> recent = service.getRecentEvents(10);
        assertEquals(LibraryEvent.Type.BOOK_ISSUED, recent.get(recent.size() - 1).getType());
    }

    @Test
    void overdueLoansAreAnnouncedOnce() throws Exception {
        service.checkOverdue();
        int afterFirst = service.getRecentEvents(500).size();
        assertTrue(afterFirst > 0);
        service.checkOverdue();
        assertEquals(afterFirst, service.getRecentEvents(500).size(), "no repeat for the same loan");
    }

    @Test
    void pinsAreStoredAsPbkdf2Hashes() throws Exception {
        try (PreparedStatement ps = db.conn().prepareStatement("SELECT pin_hash FROM students WHERE student_id = 'S1001'");
             ResultSet rs = ps.executeQuery()) {
            assertTrue(rs.next());
            assertTrue(rs.getString(1).startsWith("pbkdf2-sha256$"));
            assertFalse(rs.getString(1).contains("1234"));
        }
    }

    // ------------------------------------------------------------ helpers

    private Loan openLoan(String studentId, String bookId) throws Exception {
        for (Loan l : service.listLoans(LIBRARIAN, true)) {
            if (l.getStudentId().equals(studentId) && l.getBookId().equals(bookId)) return l;
        }
        return fail("no open loan of " + bookId + " for " + studentId);
    }

    private Reservation reservation(StudentSession s, String bookId) throws Exception {
        Reservation found = null;
        for (Reservation r : service.getMyReservations(s.getToken())) {
            if (r.getBookId().equals(bookId) && (found == null || r.getId() > found.getId())) found = r;
        }
        if (found == null) fail("no reservation of " + bookId + " for " + s.getStudentId());
        return found;
    }
}
