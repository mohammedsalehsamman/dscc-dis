package library.client.ui;

import dscc.ui.Async;
import dscc.ui.BeanTableModel;
import dscc.ui.FormDialog;
import dscc.ui.FormDialog.Validators;
import dscc.ui.Refreshable;
import dscc.ui.SearchableTable;
import dscc.ui.TableExport;
import dscc.ui.Theme;
import dscc.ui.Toast;
import dscc.ui.UiKit;
import library.client.LibraryConnection;
import library.common.Book;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Book catalog. Everybody can search and filter; staff can add/edit/delete/issue,
 * students can reserve books that are all on loan. Updates live through RMI callbacks.
 */
public class CatalogPanel extends JPanel implements Refreshable {

    private static final String ALL = "All categories";

    private final LibraryConnection conn;
    private final BeanTableModel<Book> model = new BeanTableModel<Book>()
            .column("ID", String.class, Book::getBookId)
            .column("Title", String.class, Book::getTitle)
            .column("Author", String.class, Book::getAuthor)
            .column("Category", String.class, Book::getCategory)
            .column("Year", Integer.class, b -> b.getYear() == 0 ? null : b.getYear())
            .column("Available", Integer.class, Book::getAvailableCopies)
            .column("Copies", Integer.class, Book::getTotalCopies)
            .column("Status", String.class, b -> b.isAvailable() ? "Available" : "All on loan");
    private final SearchableTable table = new SearchableTable(model);
    private final JComboBox<String> categoryFilter = new JComboBox<>(new String[]{ALL});
    private final JCheckBox availableOnly = new JCheckBox("Available only");

    public CatalogPanel(LibraryConnection conn) {
        super(new BorderLayout());
        this.conn = conn;
        JTable t = table.getTable();
        t.getColumnModel().getColumn(0).setMaxWidth(60);
        t.getColumnModel().getColumn(1).setPreferredWidth(260);
        t.getColumnModel().getColumn(4).setMaxWidth(70);
        t.getColumnModel().getColumn(5).setMaxWidth(80);
        t.getColumnModel().getColumn(6).setMaxWidth(70);
        table.setRowColorizer(row -> {
            Book b = model.getRow(row);
            return b != null && !b.isAvailable() ? Theme.DANGER : null;
        });

        Runnable applyFilters = () -> {
            Object cat = categoryFilter.getSelectedItem();
            boolean avail = availableOnly.isSelected();
            table.setExtraFilter(new RowFilter<TableModel, Integer>() {
                @Override
                public boolean include(Entry<? extends TableModel, ? extends Integer> e) {
                    Book b = model.getRow(e.getIdentifier());
                    if (avail && !b.isAvailable()) return false;
                    return cat == null || ALL.equals(cat) || cat.equals(b.getCategory());
                }
            });
        };
        categoryFilter.addActionListener(e -> applyFilters.run());
        availableOnly.addActionListener(e -> applyFilters.run());

        List<Component> items = new ArrayList<>();
        items.add(table.getSearchField());
        items.add(categoryFilter);
        items.add(availableOnly);
        items.add(null);
        if (conn.isStaff()) {
            items.add(UiKit.primary("+ Add book", e -> onAdd()));
            items.add(UiKit.button("Edit", e -> onEdit()));
            items.add(UiKit.danger("Delete", e -> onDelete()));
            items.add(UiKit.button("Issue…", e -> {
                Book b = model.getRow(table.selectedModelRow());
                LibraryActions.issueBook(this, conn, b == null ? null : b.getBookId(), null, this::refresh);
            }));
            items.add(null);
        } else if (conn.isStudent()) {
            items.add(UiKit.primary("Reserve", e -> onReserve()));
            items.add(null);
        }
        items.add(UiKit.button("Refresh", e -> refresh()));
        items.add(UiKit.button("Export CSV", e -> TableExport.toCsv(this, t, "books")));

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(table.getCountLabel(), BorderLayout.WEST);
        bottom.add(LibraryActions.hint(conn.isStaff() ? "Double-click to edit  ·  Ctrl+N new book  ·  F5 refresh"
                : conn.isStudent() ? "Books shown in red are all on loan - select one and press Reserve"
                : "Sign in as staff or student for more actions"), BorderLayout.EAST);

        JPanel content = new JPanel(new BorderLayout());
        content.add(UiKit.toolbar(items.toArray(new Component[0])), BorderLayout.NORTH);
        content.add(table, BorderLayout.CENTER);
        content.add(bottom, BorderLayout.SOUTH);
        add(UiKit.page(UiKit.header("Book catalog", "RMI: listBooks / addBookDetailed / updateBook / deleteBook / reserveBook"),
                content), BorderLayout.CENTER);

        t.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && conn.isStaff()) onEdit();
            }
        });
        if (conn.isStaff()) UiKit.shortcut(this, "ctrl N", this::onAdd);
        UiKit.shortcut(this, "ctrl F", () -> table.getSearchField().requestFocusInWindow());
        UiKit.shortcut(this, "F5", this::refresh);

        conn.addListener(ev -> {
            if (isShowing()) refresh();
        });
    }

    @Override
    public void refresh() {
        Async.run(this, () -> conn.service().listBooks(null, null, false), list -> {
            model.setRows(list);
            Object current = categoryFilter.getSelectedItem();
            TreeSet<String> cats = new TreeSet<>();
            for (Book b : list) if (b.getCategory() != null) cats.add(b.getCategory());
            DefaultComboBoxModel<String> m = new DefaultComboBoxModel<>();
            m.addElement(ALL);
            for (String c : cats) m.addElement(c);
            categoryFilter.setModel(m);
            categoryFilter.setSelectedItem(current != null && cats.contains(current.toString()) ? current : ALL);
        });
    }

    private Book selected() {
        Book b = model.getRow(table.selectedModelRow());
        if (b == null) Toast.info(this, "Select a book first");
        return b;
    }

    private void onAdd() {
        int next = 1;
        for (Book b : model.getRows()) {
            try {
                next = Math.max(next, Integer.parseInt(b.getBookId().replaceAll("\\D", "")) + 1);
            } catch (NumberFormatException ignored) {
                // non-numeric id
            }
        }
        Book b = showForm(null, "B" + next);
        if (b == null) return;
        Async.run(this, () -> conn.service().addBookDetailed(conn.staffToken(), b), msg -> {
            Toast.result(this, msg);
            if (msg.startsWith("Success")) refresh();
        });
    }

    private void onEdit() {
        Book old = selected();
        if (old == null) return;
        Book b = showForm(old, old.getBookId());
        if (b == null) return;
        Async.run(this, () -> conn.service().updateBook(conn.staffToken(), b), msg -> {
            Toast.result(this, msg);
            if (msg.startsWith("Success")) refresh();
        });
    }

    private void onDelete() {
        Book b = selected();
        if (b == null) return;
        if (JOptionPane.showConfirmDialog(this, "Delete '" + b.getTitle() + "'?\nThe loan history is kept.",
                "Confirm delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return;
        Async.run(this, () -> conn.service().deleteBook(conn.staffToken(), b.getBookId()), msg -> {
            Toast.result(this, msg);
            if (msg.startsWith("Success")) refresh();
        });
    }

    private void onReserve() {
        Book b = selected();
        if (b == null) return;
        Async.run(this, () -> conn.service().reserveBook(conn.studentToken(), b.getBookId()), msg -> Toast.result(this, msg));
    }

    private Book showForm(Book old, String id) {
        List<String> cats = new ArrayList<>();
        for (int i = 1; i < categoryFilter.getItemCount(); i++) cats.add(categoryFilter.getItemAt(i));
        FormDialog f = new FormDialog(this, old == null ? "Add book" : "Edit book " + id);
        if (old == null) f.text("id", "Book ID", id, Validators.required());
        else f.readOnly("id", "Book ID", id);
        f.text("title", "Title", old == null ? "" : old.getTitle(), Validators.required())
                .text("author", "Author", old == null ? "" : old.getAuthor(), Validators.required())
                .combo("category", "Category", cats.toArray(new String[0]), old == null ? "General" : old.getCategory(), true)
                .text("isbn", "ISBN", old == null || old.getIsbn() == null ? "" : old.getIsbn(), null)
                .text("year", "Year", old == null || old.getYear() == 0 ? "" : String.valueOf(old.getYear()),
                        s -> s.isEmpty() ? null : Validators.intRange(1000, 2100).apply(s))
                .text("copies", "Number of copies", old == null ? "1" : String.valueOf(old.getTotalCopies()),
                        Validators.intRange(1, 1000));
        Map<String, String> v = f.showDialog();
        if (v == null) return null;
        return new Book(v.get("id"), v.get("title"), v.get("author"), v.get("category"), v.get("isbn"),
                v.get("year").isEmpty() ? 0 : Integer.parseInt(v.get("year")), Integer.parseInt(v.get("copies")));
    }
}
