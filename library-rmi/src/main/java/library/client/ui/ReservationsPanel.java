package library.client.ui;

import dscc.ui.Async;
import dscc.ui.BeanTableModel;
import dscc.ui.Refreshable;
import dscc.ui.SearchableTable;
import dscc.ui.Theme;
import dscc.ui.Toast;
import dscc.ui.UiKit;
import library.client.LibraryConnection;
import library.common.Reservation;

import javax.swing.*;
import java.awt.*;
import java.time.format.DateTimeFormatter;

/**
 * Open reservations (holds) for staff. READY = a returned copy is held for that student;
 * "Issue to student" hands it over.
 */
public class ReservationsPanel extends JPanel implements Refreshable {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final LibraryConnection conn;
    private final BeanTableModel<Reservation> model = new BeanTableModel<Reservation>()
            .column("#", Integer.class, Reservation::getId)
            .column("Book", String.class, Reservation::getBookId)
            .column("Title", String.class, Reservation::getBookTitle)
            .column("Student", String.class, Reservation::getStudentId)
            .column("Name", String.class, Reservation::getStudentName)
            .column("Reserved at", String.class, r -> r.getCreatedAt().format(FMT))
            .column("Status", String.class, Reservation::getStatus)
            .column("Queue", String.class, r -> r.getQueuePosition() > 0 ? "#" + r.getQueuePosition() : "");
    private final SearchableTable table = new SearchableTable(model);

    public ReservationsPanel(LibraryConnection conn) {
        super(new BorderLayout());
        this.conn = conn;
        table.setRowColorizer(row -> {
            Reservation r = model.getRow(row);
            return r != null && Reservation.READY.equals(r.getStatus()) ? Theme.SUCCESS : null;
        });
        JToolBar tb = UiKit.toolbar(table.getSearchField(), null,
                UiKit.primary("Issue to student", e -> {
                    Reservation r = selected();
                    if (r != null) LibraryActions.issueBook(this, conn, r.getBookId(), r.getStudentId(), this::refresh);
                }),
                UiKit.danger("Cancel reservation", e -> onCancel()), null,
                UiKit.button("Refresh", e -> refresh()));
        JPanel content = new JPanel(new BorderLayout());
        content.add(tb, BorderLayout.NORTH);
        content.add(table, BorderLayout.CENTER);
        content.add(LibraryActions.hint("Green = READY: a returned copy is held for this student (other students cannot borrow it)"),
                BorderLayout.SOUTH);
        add(UiKit.page(UiKit.header("Reservations", "RMI: listReservations / cancelReservation - queue served first come, first served"),
                content), BorderLayout.CENTER);
        UiKit.shortcut(this, "F5", this::refresh);
        conn.addListener(ev -> {
            if (isShowing()) refresh();
        });
    }

    @Override
    public void refresh() {
        Async.run(this, () -> conn.service().listReservations(conn.staffToken()), model::setRows);
    }

    private Reservation selected() {
        Reservation r = model.getRow(table.selectedModelRow());
        if (r == null) Toast.info(this, "Select a reservation first");
        return r;
    }

    private void onCancel() {
        Reservation r = selected();
        if (r == null) return;
        if (JOptionPane.showConfirmDialog(this, "Cancel the reservation of '" + r.getBookTitle() + "' for "
                + r.getStudentName() + "?", "Confirm", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        Async.run(this, () -> conn.service().cancelReservation(conn.staffToken(), r.getId()), msg -> {
            Toast.result(this, msg);
            refresh();
        });
    }
}
