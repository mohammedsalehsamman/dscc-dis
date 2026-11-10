package library.server;

import library.common.Book;
import library.common.LibraryEvent.Type;
import library.common.Reservation;
import library.common.StaffInfo;

import java.rmi.RemoteException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static library.server.ServiceSupport.blank;
import static library.server.ServiceSupport.log;
import static library.server.ServiceSupport.read;
import static library.server.ServiceSupport.write;

/** The book catalog: the operations required by the assignment plus detailed book management. */
class CatalogService {

    private final Database db;
    private final BookDao books;
    private final LoanDao loans;
    private final ReservationDao reservations;
    private final EventBus events;

    CatalogService(Database db, BookDao books, LoanDao loans, ReservationDao reservations, EventBus events) {
        this.db = db;
        this.books = books;
        this.loans = loans;
        this.reservations = reservations;
        this.events = events;
    }

    // ------------------------------------------------------------ required by the assignment

    String addBook(String bookId, String title, String author) {
        if (blank(bookId) || blank(title) || blank(author)) {
            return log("addBook", "Failure: book ID, title and author are all required");
        }
        return write(db, () -> {
            String id = bookId.trim();
            if (books.find(id) != null) return log("addBook", "Failure: a book with ID " + id + " already exists");
            books.insert(new Book(id, title.trim(), author.trim()));
            events.publish(Type.BOOK_ADDED, "New book '" + title.trim() + "' (" + id + ")", id, null);
            return log("addBook", "Success: book '" + title.trim() + "' added with ID " + id);
        });
    }

    String searchBook(String title) throws RemoteException {
        if (blank(title)) return "Book not found";
        return read(db, () -> {
            StringBuilder sb = new StringBuilder();
            for (Book b : books.searchByTitle(title)) {
                if (sb.length() > 0) sb.append('\n');
                sb.append(describe(b));
            }
            return log("searchBook('" + title + "')", sb.length() == 0 ? "Book not found" : sb.toString());
        });
    }

    List<String> viewAllBooks() throws RemoteException {
        return read(db, () -> {
            List<String> list = new ArrayList<>();
            for (Book b : books.search(null, null, false)) list.add(describe(b));
            return list;
        });
    }

    private static String describe(Book b) {
        return b + " | Category: " + b.getCategory() + " | Copies: " + b.getAvailableCopies() + "/" + b.getTotalCopies()
                + " | Status: " + (b.isAvailable() ? "Available" : "All copies issued");
    }

    // ------------------------------------------------------------ browsing

    List<Book> list(String query, String category, boolean availableOnly) throws RemoteException {
        return read(db, () -> books.search(query, category, availableOnly));
    }

    Book get(String bookId) throws RemoteException {
        return read(db, () -> bookId == null ? null : books.find(bookId.trim()));
    }

    List<String> categories() throws RemoteException {
        return read(db, books::categories);
    }

    // ------------------------------------------------------------ staff management

    String add(StaffInfo staff, Book book) {
        String error = validate(book);
        if (error != null) return log("addBookDetailed", "Failure: " + error);
        return write(db, () -> {
            Book b = clean(book);
            if (books.find(b.getBookId()) != null) {
                return log("addBookDetailed", "Failure: a book with ID " + b.getBookId() + " already exists");
            }
            b.setAvailableCopies(b.getTotalCopies());
            books.insert(b);
            events.publish(Type.BOOK_ADDED, staff.getName() + " added '" + b.getTitle() + "' (" + b.getTotalCopies()
                    + " copies)", b.getBookId(), null);
            return log("addBookDetailed", "Success: book '" + b.getTitle() + "' added with ID " + b.getBookId());
        });
    }

    String update(StaffInfo staff, Book book) {
        String error = validate(book);
        if (error != null) return log("updateBook", "Failure: " + error);
        return write(db, () -> {
            Book b = clean(book);
            if (books.find(b.getBookId()) == null) return log("updateBook", "Failure: no book with ID " + b.getBookId());
            int onLoan = loans.activeCountForBook(b.getBookId());
            int held = reservations.readyCount(b.getBookId());
            int available = b.getTotalCopies() - onLoan - held;
            if (available < 0) {
                return log("updateBook", "Failure: cannot set " + b.getTotalCopies() + " copies - " + onLoan
                        + " on loan and " + held + " held for reservations");
            }
            b.setAvailableCopies(available);
            books.update(b);
            events.publish(Type.BOOK_UPDATED, staff.getName() + " updated '" + b.getTitle() + "'", b.getBookId(), null);
            return log("updateBook", "Success: book " + b.getBookId() + " updated");
        });
    }

    String delete(StaffInfo staff, String bookId) {
        return write(db, () -> {
            Book b = bookId == null ? null : books.find(bookId.trim());
            if (b == null) return log("deleteBook", "Failure: no book with ID " + bookId);
            int onLoan = loans.activeCountForBook(b.getBookId());
            if (onLoan > 0) return log("deleteBook", "Failure: " + onLoan + " copies are still on loan");
            for (Reservation r : reservations.open()) {
                if (r.getBookId().equals(b.getBookId())) reservations.setStatus(r.getId(), Reservation.CANCELLED);
            }
            books.delete(b.getBookId());
            events.publish(Type.BOOK_DELETED, staff.getName() + " removed '" + b.getTitle() + "'", b.getBookId(), null);
            return log("deleteBook", "Success: book " + b.getBookId() + " deleted (loan history kept)");
        });
    }

    private static String validate(Book b) {
        if (b == null) return "no book data was sent";
        if (blank(b.getBookId()) || blank(b.getTitle()) || blank(b.getAuthor())) return "book ID, title and author are required";
        if (b.getTotalCopies() < 1 || b.getTotalCopies() > 1000) return "copies must be between 1 and 1000";
        int nextYear = LocalDate.now().getYear() + 1;
        if (b.getYear() != 0 && (b.getYear() < 1000 || b.getYear() > nextYear)) return "invalid publication year";
        return null;
    }

    private static Book clean(Book b) {
        return new Book(b.getBookId().trim(), b.getTitle().trim(), b.getAuthor().trim(),
                blank(b.getCategory()) ? "General" : b.getCategory().trim(),
                blank(b.getIsbn()) ? null : b.getIsbn().trim(), b.getYear(), b.getTotalCopies());
    }
}
