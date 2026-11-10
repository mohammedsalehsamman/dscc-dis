package library.client.ui;

import dscc.ui.Async;
import dscc.ui.BeanTableModel;
import dscc.ui.Refreshable;
import dscc.ui.SearchableTable;
import dscc.ui.StatCard;
import dscc.ui.Theme;
import dscc.ui.Toast;
import dscc.ui.UiKit;
import dscc.util.AppConfig;
import library.client.LibraryConnection;
import library.common.Loan;
import library.common.Reservation;

import javax.swing.*;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Student self-service: my loans (with renew), my reservations (with cancel) and my fines.
 */
public class MyLibraryPanel extends JPanel implements Refreshable {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final LibraryConnection conn;
    private final StatCard onLoanCard = new StatCard("Books on loan", Theme.ACCENT);
    private final StatCard nextDueCard = new StatCard("Next due date", Theme.PURPLE);
    private final StatCard reservedCard = new StatCard("Reservations", Theme.SUCCESS);
    private final StatCard finesCard = new StatCard("Fines to pay", Theme.WARNING);

    private final BeanTableModel<Loan> loans = new BeanTableModel<Loan>()
            .column("Loan", Integer.class, Loan::getId)
            .column("Title", String.class, Loan::getBookTitle)
            .column("Issued", String.class, l -> String.valueOf(l.getIssueDate()))
            .column("Due", String.class, l -> String.valueOf(l.getDueDate()))
            .column("Returned", String.class, l -> l.getReturnDate() == null ? "" : l.getReturnDate().toString())
            .column("Status", String.class, Loan::getStatus)
            .column("Renewed", Integer.class, Loan::getRenewCount)
            .column("Fine", String.class, l -> l.getFine() > 0 ? LibraryActions.money(l.getFine()) + (l.isFinePaid() ? " (paid)" : " (unpaid)") : "");
    private final BeanTableModel<Reservation> reservations = new BeanTableModel<Reservation>()
            .column("#", Integer.class, Reservation::getId)
            .column("Title", String.class, Reservation::getBookTitle)
            .column("Reserved at", String.class, r -> r.getCreatedAt().format(FMT))
            .column("Status", String.class, Reservation::getStatus)
            .column("Queue", String.class, r -> r.getQueuePosition() > 0 ? "#" + r.getQueuePosition() : "");
    private final SearchableTable loansTable = new SearchableTable(loans);
    private final SearchableTable resTable = new SearchableTable(reservations);

    public MyLibraryPanel(LibraryConnection conn) {
        super(new BorderLayout());
        this.conn = conn;

        loansTable.setRowColorizer(row -> {
            Loan l = loans.getRow(row);
            if (l == null) return null;
            if (l.isOverdue()) return Theme.DANGER;
            return l.isReturned() ? Theme.muted() : null;
        });
        loansTable.getTable().getColumnModel().getColumn(0).setMaxWidth(60);
        loansTable.getTable().getColumnModel().getColumn(1).setPreferredWidth(280);
        loansTable.getTable().getColumnModel().getColumn(6).setMaxWidth(80);
        resTable.getTable().getColumnModel().getColumn(0).setMaxWidth(60);
        resTable.getTable().getColumnModel().getColumn(1).setPreferredWidth(280);
        resTable.setRowColorizer(row -> {
            Reservation r = reservations.getRow(row);
            return r != null && Reservation.READY.equals(r.getStatus()) ? Theme.SUCCESS : null;
        });

        JPanel cards = new JPanel(new GridLayout(1, 4, 12, 0));
        cards.add(onLoanCard);
        cards.add(nextDueCard);
        cards.add(reservedCard);
        cards.add(finesCard);

        JPanel loansBox = new JPanel(new BorderLayout());
        loansBox.add(UiKit.toolbar(new JLabel("My loans   "), UiKit.primary("Renew selected", e -> onRenew())), BorderLayout.NORTH);
        loansBox.add(loansTable, BorderLayout.CENTER);
        JPanel resBox = new JPanel(new BorderLayout());
        resBox.add(UiKit.toolbar(new JLabel("My reservations   "), UiKit.danger("Cancel selected", e -> onCancel())), BorderLayout.NORTH);
        resBox.add(resTable, BorderLayout.CENTER);
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, loansBox, resBox);
        split.setResizeWeight(0.6);
        split.setBorder(null);

        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.add(cards, BorderLayout.NORTH);
        content.add(split, BorderLayout.CENTER);
        content.add(LibraryActions.hint("Loan period " + AppConfig.getInt(AppConfig.LOAN_DAYS) + " days  ·  up to "
                + AppConfig.getInt(AppConfig.MAX_RENEWALS) + " renewals  ·  fine " + AppConfig.get(AppConfig.FINE_PER_DAY)
                + " per late day  ·  reserve books from the Catalog tab"), BorderLayout.SOUTH);
        String name = conn.getStudent() == null ? "" : conn.getStudent().getName();
        add(UiKit.page(UiKit.header("Welcome, " + name, "RMI: getMyLoans / getMyReservations / renewLoan / cancelReservation"),
                content), BorderLayout.CENTER);
        UiKit.shortcut(this, "F5", this::refresh);

        conn.addListener(ev -> {
            if (conn.getStudent() != null && conn.getStudent().getStudentId().equals(ev.getStudentId()) && isShowing()) {
                refresh();
            }
        });
    }

    @Override
    public void refresh() {
        Async.run(this, () -> new Object[]{conn.service().getMyLoans(conn.studentToken()),
                conn.service().getMyReservations(conn.studentToken())}, data -> {
            @SuppressWarnings("unchecked") List<Loan> l = (List<Loan>) data[0];
            @SuppressWarnings("unchecked") List<Reservation> r = (List<Reservation>) data[1];
            loans.setRows(l);
            reservations.setRows(r);
            long open = l.stream().filter(x -> !x.isReturned()).count();
            onLoanCard.setValue(String.valueOf(open));
            onLoanCard.setNote(l.size() + " loans in total");
            Loan next = l.stream().filter(x -> !x.isReturned()).min((a, b) -> a.getDueDate().compareTo(b.getDueDate())).orElse(null);
            nextDueCard.setValue(next == null ? "-" : next.getDueDate().toString());
            nextDueCard.setNote(next == null ? "nothing to return" : next.isOverdue() ? "OVERDUE: " + next.getBookTitle() : next.getBookTitle());
            long openRes = r.stream().filter(x -> Reservation.WAITING.equals(x.getStatus()) || Reservation.READY.equals(x.getStatus())).count();
            long ready = r.stream().filter(x -> Reservation.READY.equals(x.getStatus())).count();
            reservedCard.setValue(String.valueOf(openRes));
            reservedCard.setNote(ready > 0 ? ready + " ready for pickup!" : "waiting");
            double fines = 0;
            for (Loan x : l) {
                if (x.isReturned() && !x.isFinePaid()) fines += x.getFine();
                if (x.isOverdue()) fines += x.daysLate() * AppConfig.getDouble(AppConfig.FINE_PER_DAY);
            }
            finesCard.setValue(LibraryActions.money(fines));
            finesCard.setNote(fines > 0 ? "pay at the library desk" : "no fines");
        });
    }

    private void onRenew() {
        Loan l = loans.getRow(loansTable.selectedModelRow());
        if (l == null) {
            Toast.info(this, "Select a loan first");
            return;
        }
        Async.run(this, () -> conn.service().renewLoan(conn.studentToken(), l.getId()), msg -> {
            Toast.result(this, msg);
            refresh();
        });
    }

    private void onCancel() {
        Reservation r = reservations.getRow(resTable.selectedModelRow());
        if (r == null) {
            Toast.info(this, "Select a reservation first");
            return;
        }
        Async.run(this, () -> conn.service().cancelReservation(conn.studentToken(), r.getId()), msg -> {
            Toast.result(this, msg);
            refresh();
        });
    }
}
