package library.client.ui;

import dscc.ui.Async;
import dscc.ui.FormDialog;
import dscc.ui.Toast;
import library.client.LibraryConnection;
import library.common.Book;
import library.common.Student;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Actions shared by several library screens (e.g. "issue a book" from the catalog or the loans page).
 */
final class LibraryActions {

    private LibraryActions() {
    }

    /** Opens the issue dialog (book + student pickers) and calls issueBook on the server. */
    static void issueBook(Component parent, LibraryConnection conn, String presetBookId, String presetStudentId,
                          Runnable afterSuccess) {
        Async.run(parent, () -> new Object[]{
                conn.service().listBooks(null, null, false),
                conn.service().listStudents(conn.staffToken())
        }, data -> {
            @SuppressWarnings("unchecked") List<Book> books = (List<Book>) data[0];
            @SuppressWarnings("unchecked") List<Student> students = (List<Student>) data[1];
            List<String> bookItems = new ArrayList<>();
            String presetBook = null;
            for (Book b : books) {
                String item = b.getBookId() + " - " + b.getTitle() + "  (" + b.getAvailableCopies() + " available)";
                bookItems.add(item);
                if (b.getBookId().equals(presetBookId)) presetBook = item;
            }
            List<String> studentItems = new ArrayList<>();
            String presetStudent = null;
            for (Student s : students) {
                String item = s.getStudentId() + " - " + s.getName() + "  (" + s.getActiveLoans() + " books)";
                studentItems.add(item);
                if (s.getStudentId().equals(presetStudentId)) presetStudent = item;
            }
            Map<String, String> v = new FormDialog(parent, "Issue a book")
                    .combo("book", "Book", bookItems.toArray(new String[0]), presetBook, true)
                    .combo("student", "Student", studentItems.toArray(new String[0]), presetStudent, true)
                    .okText("Issue")
                    .showDialog();
            if (v == null) return;
            String bookId = idOf(v.get("book"));
            String studentId = idOf(v.get("student"));
            Async.run(parent, () -> conn.service().issueBook(conn.staffToken(), bookId, studentId), msg -> {
                Toast.result(parent, msg);
                if (msg.startsWith("Success") && afterSuccess != null) afterSuccess.run();
            });
        });
    }

    /** "B1 - Clean Code (2 available)" -> "B1" */
    static String idOf(String item) {
        if (item == null) return "";
        int i = item.indexOf(" - ");
        return (i > 0 ? item.substring(0, i) : item).trim();
    }

    static String money(double v) {
        return String.format("%,.0f", v);
    }

    static JLabel hint(String text) {
        JLabel l = new JLabel(text);
        l.putClientProperty("FlatLaf.style", "font: -1; foreground: $Label.disabledForeground");
        return l;
    }
}
