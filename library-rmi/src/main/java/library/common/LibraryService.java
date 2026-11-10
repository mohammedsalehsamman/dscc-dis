package library.common;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Remote interface shared by the server and the clients.
 * Every method can be called over the network, so each one throws RemoteException.
 * <p>
 * Staff operations take the staff token issued by the Employee SOAP system (single sign-on):
 * the RMI server validates it by calling the SOAP AuthService / EmployeeService.
 * Student operations take the token returned by {@link #studentLogin}.
 */
public interface LibraryService extends Remote {

    /** Name used to bind/lookup the service in the RMI registry. */
    String NAME = "LibraryService";
    int PORT = 1099;

    // =========================================================== required by the assignment

    /** Adds a new book. Returns a success or failure message. */
    String addBook(String bookId, String title, String author) throws RemoteException;

    /** Searches by title. Returns book details, or "Book not found". */
    String searchBook(String title) throws RemoteException;

    /** Returns all books in the library with their details. */
    List<String> viewAllBooks() throws RemoteException;

    /** Returns the full issued-book history (one line per loan). */
    List<String> viewIssuedHistory() throws RemoteException;

    // =========================================================== login

    /** Staff login: forwarded to the SOAP AuthService. Must be LIBRARIAN or ADMIN. */
    StaffInfo staffLogin(String username, String password) throws RemoteException, AccessDeniedException;

    /** Checks a staff token (obtained from SOAP login) and returns the staff member. */
    StaffInfo verifyStaff(String staffToken) throws RemoteException, AccessDeniedException;

    StudentSession studentLogin(String studentId, String pin) throws RemoteException, AccessDeniedException;

    void studentLogout(String studentToken) throws RemoteException;

    // =========================================================== catalog

    /** Books filtered by text (title/author/isbn/id), category (null = all) and availability. */
    List<Book> listBooks(String query, String category, boolean availableOnly) throws RemoteException;

    Book getBook(String bookId) throws RemoteException;

    List<String> getCategories() throws RemoteException;

    String addBookDetailed(String staffToken, Book book) throws RemoteException;

    String updateBook(String staffToken, Book book) throws RemoteException;

    String deleteBook(String staffToken, String bookId) throws RemoteException;

    // =========================================================== students

    String registerStudent(String staffToken, Student student, String pin) throws RemoteException;

    String updateStudent(String staffToken, Student student, String newPinOrNull) throws RemoteException;

    String deleteStudent(String staffToken, String studentId) throws RemoteException;

    List<Student> listStudents(String staffToken) throws RemoteException, AccessDeniedException;

    // =========================================================== loans

    /** Issues a book to a student; due date = today + loan days. */
    String issueBook(String staffToken, String bookId, String studentId) throws RemoteException;

    /** Returns a loan; a fine is calculated for late days. */
    String returnBook(String staffToken, int loanId) throws RemoteException;

    /** Extends the due date (staff token or the borrowing student's token). */
    String renewLoan(String token, int loanId) throws RemoteException;

    String payFine(String staffToken, int loanId) throws RemoteException;

    List<Loan> listLoans(String staffToken, boolean activeOnly) throws RemoteException, AccessDeniedException;

    List<Loan> getMyLoans(String studentToken) throws RemoteException, AccessDeniedException;

    // =========================================================== reservations

    /** A student reserves a book that has no available copy. */
    String reserveBook(String studentToken, String bookId) throws RemoteException;

    /** Cancel (staff token or the owning student's token). */
    String cancelReservation(String token, int reservationId) throws RemoteException;

    List<Reservation> listReservations(String staffToken) throws RemoteException, AccessDeniedException;

    List<Reservation> getMyReservations(String studentToken) throws RemoteException, AccessDeniedException;

    // =========================================================== statistics & real time

    LibraryStats getStatistics() throws RemoteException;

    List<LibraryEvent> getRecentEvents(int limit) throws RemoteException;

    /** Registers a client callback: the server will push every LibraryEvent to it. */
    void registerCallback(LibraryCallback callback) throws RemoteException;

    void unregisterCallback(LibraryCallback callback) throws RemoteException;

    /** Health check. */
    String ping() throws RemoteException;
}
