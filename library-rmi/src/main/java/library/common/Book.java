package library.common;

import java.io.Serializable;

/**
 * A book record. Serializable so it can be sent through RMI.
 * Required fields (assignment): bookId, title, author.
 * Extra fields: category, isbn, year, totalCopies, availableCopies.
 */
public class Book implements Serializable {

    private static final long serialVersionUID = 2L;

    private String bookId;
    private String title;
    private String author;
    private String category = "General";
    private String isbn;
    private int year;
    private int totalCopies = 1;
    private int availableCopies = 1;

    public Book() {
    }

    public Book(String bookId, String title, String author) {
        this.bookId = bookId;
        this.title = title;
        this.author = author;
    }

    public Book(String bookId, String title, String author, String category, String isbn, int year, int copies) {
        this(bookId, title, author);
        this.category = category;
        this.isbn = isbn;
        this.year = year;
        this.totalCopies = copies;
        this.availableCopies = copies;
    }

    public String getBookId() {
        return bookId;
    }

    public void setBookId(String bookId) {
        this.bookId = bookId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getTotalCopies() {
        return totalCopies;
    }

    public void setTotalCopies(int totalCopies) {
        this.totalCopies = totalCopies;
    }

    public int getAvailableCopies() {
        return availableCopies;
    }

    public void setAvailableCopies(int availableCopies) {
        this.availableCopies = availableCopies;
    }

    public boolean isAvailable() {
        return availableCopies > 0;
    }

    @Override
    public String toString() {
        return "Book ID: " + bookId + " | Title: " + title + " | Author: " + author;
    }
}
