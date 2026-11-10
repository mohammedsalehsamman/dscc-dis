package library.client.ui;

import dscc.ui.Async;
import dscc.ui.BeanTableModel;
import dscc.ui.Refreshable;
import dscc.ui.SearchableTable;
import dscc.ui.TableExport;
import dscc.ui.Theme;
import dscc.ui.Toast;
import dscc.ui.UiKit;
import library.client.LibraryConnection;
import library.common.Loan;

import javax.swing.*;
import java.awt.*;

/**
 * Loans = issued book records. Staff can issue, return (with automatic late fine), renew and
 * take fine payments. Overdue loans are shown in red. Live updates through RMI callbacks.
 */
public class LoansPanel extends JPanel implements Refreshable {

    private final LibraryConnection conn;
    private final BeanTableModel<Loan> model = new BeanTableModel<Loan>()
            .column("Loan", Integer.class, Loan::getId)
            .column("Book", String.class, Loan::getBookId)
            .column("Title", String.class, Loan::getBookTitle)
            .column("Student", String.class, Loan::getStudentId)
            .column("Name", String.class, Loan::getStudentName)
            .column("Issued by", String.class, Loan::getIssuedByName)
            .column("Issued", String.class, l -> str(l.getIssueDate()))
            .column("Due", String.class, l -> str(l.getDueDate()))
            .column("Returned", String.class, l -> str(l.getReturnDate()))
            .column("Status", String.class, Loan::getStatus)
            .column("Fine", String.class, l -> l.getFine() > 0
                    ? LibraryActions.money(l.getFine()) + (l.isFinePaid() ? " (paid)" : " (unpaid)") : "");
    private final SearchableTable table = new SearchableTable(model);
    private final JComboBox<String> show = new JComboBox<>(new String[]{"Active loans", "All loans (history)"});

    public LoansPanel(LibraryConnection conn) {
        super(new BorderLayout());
        this.conn = conn;
        JTable t = table.getTable();
        t.getColumnModel().getColumn(0).setMaxWidth(55);
        t.getColumnModel().getColumn(1).setMaxWidth(55);
        t.getColumnModel().getColumn(2).setPreferredWidth(220);
        table.setRowColorizer(row -> {
            Loan l = model.getRow(row);
            if (l == null) return null;
            if (l.isOverdue()) return Theme.DANGER;
            if (l.getFine() > 0 && !l.isFinePaid()) return Theme.WARNING;
            if (l.isReturned()) return Theme.muted();
            return null;
        });
        show.addActionListener(e -> refresh());

        JToolBar tb = UiKit.toolbar(table.getSearchField(), show, null,
                UiKit.primary("Issue book…", e -> LibraryActions.issueBook(this, conn, null, null, this::refresh)),
                UiKit.button("Return", e -> onReturn()),
                UiKit.button("Renew", e -> onRenew()),
                UiKit.button("Pay fine", e -> onPayFine()), null,
                UiKit.button("Refresh", e -> refresh()),
                UiKit.button("Export CSV", e -> TableExport.toCsv(this, t, "loans")),
                UiKit.button("Print / PDF", e -> TableExport.print(this, t, "Library loans")));

        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        legend.add(table.getCountLabel());
        legend.add(UiKit.badge("Overdue", Theme.DANGER));
        legend.add(UiKit.badge("Unpaid fine", Theme.WARNING));
        legend.add(LibraryActions.hint("Fine = days late × fine per day (config.properties)"));

        JPanel content = new JPanel(new BorderLayout());
        content.add(tb, BorderLayout.NORTH);
        content.add(table, BorderLayout.CENTER);
        content.add(legend, BorderLayout.SOUTH);
        add(UiKit.page(UiKit.header("Loans", "RMI: issueBook / returnBook / renewLoan / payFine - staff verified via SOAP"),
                content), BorderLayout.CENTER);
        UiKit.shortcut(this, "F5", this::refresh);
        conn.addListener(ev -> {
            if (isShowing()) refresh();
        });
    }

    @Override
    public void refresh() {
        boolean active = show.getSelectedIndex() == 0;
        Async.run(this, () -> conn.service().listLoans(conn.staffToken(), active), model::setRows);
    }

    private Loan selected() {
        Loan l = model.getRow(table.selectedModelRow());
        if (l == null) Toast.info(this, "Select a loan first");
        return l;
    }

    private void onReturn() {
        Loan l = selected();
        if (l == null) return;
        String extra = l.isOverdue() ? "\nThis book is " + l.daysLate() + " day(s) late - a fine will be charged." : "";
        if (JOptionPane.showConfirmDialog(this, "Return '" + l.getBookTitle() + "' from " + l.getStudentName() + "?" + extra,
                "Return book", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        Async.run(this, () -> conn.service().returnBook(conn.staffToken(), l.getId()), msg -> {
            Toast.result(this, msg);
            refresh();
        });
    }

    private void onRenew() {
        Loan l = selected();
        if (l == null) return;
        Async.run(this, () -> conn.service().renewLoan(conn.staffToken(), l.getId()), msg -> {
            Toast.result(this, msg);
            refresh();
        });
    }

    private void onPayFine() {
        Loan l = selected();
        if (l == null) return;
        Async.run(this, () -> conn.service().payFine(conn.staffToken(), l.getId()), msg -> {
            Toast.result(this, msg);
            refresh();
        });
    }

    private static String str(Object o) {
        return o == null ? "" : o.toString();
    }
}
