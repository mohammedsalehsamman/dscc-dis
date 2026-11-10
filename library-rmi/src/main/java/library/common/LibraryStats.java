package library.common;

import java.io.Serializable;
import java.util.LinkedHashMap;

/**
 * Library statistics for dashboards and reports.
 */
public class LibraryStats implements Serializable {

    private static final long serialVersionUID = 1L;

    public int titles;
    public int totalCopies;
    public int availableCopies;
    public int activeLoans;
    public int overdueLoans;
    public int students;
    public int waitingReservations;
    public int loansThisMonth;
    public double finesCollected;
    public double finesPending;

    /** Book title -> number of times borrowed (top 5). */
    public LinkedHashMap<String, Integer> topBooks = new LinkedHashMap<>();
    /** "yyyy-MM" -> number of loans (last 6 months). */
    public LinkedHashMap<String, Integer> loansPerMonth = new LinkedHashMap<>();
    /** Category -> number of titles. */
    public LinkedHashMap<String, Integer> titlesPerCategory = new LinkedHashMap<>();
    /** Employee id (from the SOAP system) -> number of loans issued. */
    public LinkedHashMap<Integer, Integer> loansPerEmployeeId = new LinkedHashMap<>();
    /** Employee name (snapshot at issue time) -> number of loans issued. */
    public LinkedHashMap<String, Integer> loansPerEmployeeName = new LinkedHashMap<>();
}
