package library.server;

import dscc.util.ServerLog;
import library.common.AccessDeniedException;
import library.common.Book;
import library.common.LibraryCallback;
import library.common.LibraryEvent;
import library.common.LibraryService;
import library.common.LibraryStats;
import library.common.Loan;
import library.common.Reservation;
import library.common.StaffInfo;
import library.common.Student;
import library.common.StudentSession;
import library.integration.EmployeeVerifier;
import library.integration.StaffVerifier;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

import static library.server.ServiceSupport.log;
import static library.server.ServiceSupport.read;

/**
 * Remote object: the implementation of LibraryService that lives on the server.
 * <p>
 * This class is the RMI facade. For every call it decides who the caller is, then hands the work
 * to one of the services ({@link CatalogService}, {@link StudentService}, {@link LoanService},
 * {@link ReservationService}):
 * <ul>
 *   <li>Staff tokens are verified through the Employee SOAP system ({@link EmployeeVerifier}),
 *       BEFORE the services take the database lock, so a slow SOAP call never blocks other clients.</li>
 *   <li>Students are recognised by the session token they got from {@link #studentLogin}.</li>
 *   <li>Every change is pushed to all connected clients through RMI callbacks ({@link EventBus}).</li>
 * </ul>
 * Data is stored in SQLite; the services do all database work under one lock because RMI serves
 * each client on its own thread.
 */
public class LibraryServiceImpl extends UnicastRemoteObject implements LibraryService {

    private static final long serialVersionUID = 3L;

    private final transient Database db;
    private final transient StaffVerifier verifier;
    private final transient EventBus events;
    private final transient StatsDao stats;
    private final transient CatalogService catalog;
    private final transient StudentService students;
    private final transient LoanService loans;
    private final transient ReservationService reservations;

    /** Work that needs a verified staff member. */
    @FunctionalInterface
    private interface StaffWork {
        String run(StaffInfo staff);
    }

    public LibraryServiceImpl(Database db, StaffVerifier verifier) throws RemoteException {
        super();
        this.db = db;
        this.verifier = verifier;
        this.events = new EventBus(db);
        this.stats = new StatsDao(db);
        BookDao bookDao = new BookDao(db);
        LoanDao loanDao = new LoanDao(db);
        ReservationDao reservationDao = new ReservationDao(db);
        StudentDao studentDao = new StudentDao(db);
        this.catalog = new CatalogService(db, bookDao, loanDao, reservationDao, events);
        this.reservations = new ReservationService(db, bookDao, loanDao, reservationDao, events);
        this.students = new StudentService(db, studentDao, reservations, events);
        this.loans = new LoanService(db, bookDao, studentDao, loanDao, reservationDao, events);
    }

    public EventBus events() {
        return events;
    }

    // =================================================================== required by the assignment

    @Override
    public String addBook(String bookId, String title, String author) throws RemoteException {
        ServerLog.request();
        return catalog.addBook(bookId, title, author);
    }

    @Override
    public String searchBook(String title) throws RemoteException {
        ServerLog.request();
        return catalog.searchBook(title);
    }

    @Override
    public List<String> viewAllBooks() throws RemoteException {
        ServerLog.request();
        return catalog.viewAllBooks();
    }

    @Override
    public List<String> viewIssuedHistory() throws RemoteException {
        ServerLog.request();
        return loans.issuedHistory();
    }

    // =================================================================== login

    @Override
    public StaffInfo staffLogin(String username, String password) throws RemoteException, AccessDeniedException {
        ServerLog.request();
        StaffInfo s = verifier.login(username, password);
        ServerLog.info("Staff login (via SOAP): " + s.getName() + " (" + s.getRole() + ")");
        return s;
    }

    @Override
    public StaffInfo verifyStaff(String staffToken) throws RemoteException, AccessDeniedException {
        ServerLog.request();
        return verifier.verify(staffToken);
    }

    @Override
    public StudentSession studentLogin(String studentId, String pin) throws RemoteException, AccessDeniedException {
        ServerLog.request();
        return students.login(studentId, pin);
    }

    @Override
    public void studentLogout(String studentToken) throws RemoteException {
        ServerLog.request();
        students.logout(studentToken);
    }

    // =================================================================== catalog

    @Override
    public List<Book> listBooks(String query, String category, boolean availableOnly) throws RemoteException {
        ServerLog.request();
        return catalog.list(query, category, availableOnly);
    }

    @Override
    public Book getBook(String bookId) throws RemoteException {
        ServerLog.request();
        return catalog.get(bookId);
    }

    @Override
    public List<String> getCategories() throws RemoteException {
        ServerLog.request();
        return catalog.categories();
    }

    @Override
    public String addBookDetailed(String staffToken, Book book) throws RemoteException {
        ServerLog.request();
        return asStaff(staffToken, "addBookDetailed", staff -> catalog.add(staff, book));
    }

    @Override
    public String updateBook(String staffToken, Book book) throws RemoteException {
        ServerLog.request();
        return asStaff(staffToken, "updateBook", staff -> catalog.update(staff, book));
    }

    @Override
    public String deleteBook(String staffToken, String bookId) throws RemoteException {
        ServerLog.request();
        return asStaff(staffToken, "deleteBook", staff -> catalog.delete(staff, bookId));
    }

    // =================================================================== students

    @Override
    public String registerStudent(String staffToken, Student student, String pin) throws RemoteException {
        ServerLog.request();
        return asStaff(staffToken, "registerStudent", staff -> students.register(staff, student, pin));
    }

    @Override
    public String updateStudent(String staffToken, Student student, String newPinOrNull) throws RemoteException {
        ServerLog.request();
        return asStaff(staffToken, "updateStudent", staff -> students.update(student, newPinOrNull));
    }

    @Override
    public String deleteStudent(String staffToken, String studentId) throws RemoteException {
        ServerLog.request();
        return asStaff(staffToken, "deleteStudent", staff -> students.delete(studentId));
    }

    @Override
    public List<Student> listStudents(String staffToken) throws RemoteException, AccessDeniedException {
        ServerLog.request();
        verifier.verify(staffToken);
        return students.list();
    }

    // =================================================================== loans

    @Override
    public String issueBook(String staffToken, String bookId, String studentId) throws RemoteException {
        ServerLog.request();
        return asStaff(staffToken, "issueBook", staff -> loans.issue(staff, bookId, studentId));
    }

    @Override
    public String returnBook(String staffToken, int loanId) throws RemoteException {
        ServerLog.request();
        return asStaff(staffToken, "returnBook", staff -> loans.returnBook(staff, loanId));
    }

    /** Students renew their own loans; staff can renew any loan. */
    @Override
    public String renewLoan(String token, int loanId) throws RemoteException {
        ServerLog.request();
        StudentSession student = students.find(token);
        if (student != null) return loans.renew(student, student.getName(), loanId);
        return asStaff(token, "renewLoan", staff -> loans.renew(null, staff.getName(), loanId));
    }

    @Override
    public String payFine(String staffToken, int loanId) throws RemoteException {
        ServerLog.request();
        return asStaff(staffToken, "payFine", staff -> loans.payFine(staff, loanId));
    }

    @Override
    public List<Loan> listLoans(String staffToken, boolean activeOnly) throws RemoteException, AccessDeniedException {
        ServerLog.request();
        verifier.verify(staffToken);
        return loans.list(activeOnly);
    }

    @Override
    public List<Loan> getMyLoans(String studentToken) throws RemoteException, AccessDeniedException {
        ServerLog.request();
        return loans.byStudent(students.require(studentToken).getStudentId());
    }

    // =================================================================== reservations

    @Override
    public String reserveBook(String studentToken, String bookId) throws RemoteException {
        ServerLog.request();
        StudentSession s;
        try {
            s = students.require(studentToken);
        } catch (AccessDeniedException e) {
            return log("reserveBook", "Failure: " + e.getMessage());
        }
        return reservations.reserve(s, bookId);
    }

    /** Students cancel their own reservations; staff can cancel any reservation. */
    @Override
    public String cancelReservation(String token, int reservationId) throws RemoteException {
        ServerLog.request();
        StudentSession student = students.find(token);
        if (student != null) return reservations.cancel(student, reservationId);
        return asStaff(token, "cancelReservation", staff -> reservations.cancel(null, reservationId));
    }

    @Override
    public List<Reservation> listReservations(String staffToken) throws RemoteException, AccessDeniedException {
        ServerLog.request();
        verifier.verify(staffToken);
        return reservations.open();
    }

    @Override
    public List<Reservation> getMyReservations(String studentToken) throws RemoteException, AccessDeniedException {
        ServerLog.request();
        return reservations.byStudent(students.require(studentToken).getStudentId());
    }

    // =================================================================== statistics & real time

    @Override
    public LibraryStats getStatistics() throws RemoteException {
        ServerLog.request();
        return read(db, stats::load);
    }

    @Override
    public List<LibraryEvent> getRecentEvents(int limit) throws RemoteException {
        ServerLog.request();
        return read(db, () -> events.recent(limit));
    }

    @Override
    public void registerCallback(LibraryCallback callback) throws RemoteException {
        ServerLog.request();
        if (callback != null) events.register(callback);
    }

    @Override
    public void unregisterCallback(LibraryCallback callback) throws RemoteException {
        ServerLog.request();
        if (callback != null) events.unregister(callback);
    }

    @Override
    public String ping() throws RemoteException {
        return "OK";
    }

    /** Called every minute by the server's scheduler: announces loans that just became overdue. */
    public void checkOverdue() {
        loans.checkOverdue();
    }

    // =================================================================== helpers

    /** Verifies the staff token (outside the database lock), then runs the work or returns the refusal. */
    private String asStaff(String token, String operation, StaffWork work) {
        StaffInfo staff;
        try {
            staff = verifier.verify(token);
        } catch (AccessDeniedException e) {
            return log(operation, "Failure: " + e.getMessage());
        }
        return work.run(staff);
    }
}
