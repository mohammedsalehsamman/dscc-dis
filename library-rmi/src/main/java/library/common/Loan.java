package library.common;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * A book loan (issued book record): who borrowed which book, which employee issued it,
 * when it is due, when it was returned and the late fine.
 */
public class Loan implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private String bookId;
    private String bookTitle;
    private String studentId;
    private String studentName;
    private int issuedByEmployeeId;
    private String issuedByName;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private LocalDate returnDate;
    private int renewCount;
    private double fine;
    private boolean finePaid;

    public boolean isFinePaid() {
        return finePaid;
    }

    public void setFinePaid(boolean finePaid) {
        this.finePaid = finePaid;
    }

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

    public int getIssuedByEmployeeId() {
        return issuedByEmployeeId;
    }

    public void setIssuedByEmployeeId(int issuedByEmployeeId) {
        this.issuedByEmployeeId = issuedByEmployeeId;
    }

    public String getIssuedByName() {
        return issuedByName;
    }

    public void setIssuedByName(String issuedByName) {
        this.issuedByName = issuedByName;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(LocalDate returnDate) {
        this.returnDate = returnDate;
    }

    public int getRenewCount() {
        return renewCount;
    }

    public void setRenewCount(int renewCount) {
        this.renewCount = renewCount;
    }

    public double getFine() {
        return fine;
    }

    public void setFine(double fine) {
        this.fine = fine;
    }

    public boolean isReturned() {
        return returnDate != null;
    }

    public boolean isOverdue() {
        return !isReturned() && dueDate != null && LocalDate.now().isAfter(dueDate);
    }

    /** Days late so far (open loan) or at return time (closed loan). */
    public long daysLate() {
        if (dueDate == null) return 0;
        LocalDate end = returnDate != null ? returnDate : LocalDate.now();
        return Math.max(0, ChronoUnit.DAYS.between(dueDate, end));
    }

    public String getStatus() {
        if (isReturned()) return daysLate() > 0 ? "Returned late" : "Returned";
        return isOverdue() ? "OVERDUE" : "On loan";
    }

    @Override
    public String toString() {
        return "Loan #" + id + " | Book: " + bookId + " (" + bookTitle + ") | Student: " + studentId
                + " (" + studentName + ") | Issued by: " + issuedByName + " | Issued: " + issueDate
                + " | Due: " + dueDate + " | Returned: " + (returnDate == null ? "not yet" : returnDate)
                + (fine > 0 ? " | Fine: " + fine : "");
    }
}
