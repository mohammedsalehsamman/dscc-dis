package library.client.ui;

import dscc.ui.Async;
import dscc.ui.BeanTableModel;
import dscc.ui.Refreshable;
import dscc.ui.SearchableTable;
import dscc.ui.TableExport;
import dscc.ui.Theme;
import dscc.ui.UiKit;
import library.client.LibraryConnection;
import library.common.LibraryEvent;

import javax.swing.*;
import java.awt.*;
import java.time.format.DateTimeFormatter;

/**
 * Live event feed. Events arrive instantly through RMI callbacks (the server calls the client).
 */
public class EventsPanel extends JPanel implements Refreshable {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final LibraryConnection conn;
    private final BeanTableModel<LibraryEvent> model = new BeanTableModel<LibraryEvent>()
            .column("Time", String.class, e -> e.getTime().format(FMT))
            .column("Event", String.class, e -> e.getType().name())
            .column("Message", String.class, LibraryEvent::getMessage);
    private boolean loaded;

    public EventsPanel(LibraryConnection conn) {
        super(new BorderLayout());
        this.conn = conn;
        SearchableTable table = new SearchableTable(model);
        JTable t = table.getTable();
        t.getColumnModel().getColumn(0).setPreferredWidth(150);
        t.getColumnModel().getColumn(0).setMaxWidth(170);
        t.getColumnModel().getColumn(1).setPreferredWidth(170);
        t.getColumnModel().getColumn(1).setMaxWidth(200);
        table.setRowColorizer(row -> {
            LibraryEvent e = model.getRow(row);
            if (e == null) return null;
            switch (e.getType()) {
                case OVERDUE:
                case BOOK_DELETED:
                    return Theme.DANGER;
                case RESERVATION_READY:
                case BOOK_ADDED:
                    return Theme.SUCCESS;
                default:
                    return null;
            }
        });
        JLabel live = conn.hasLiveUpdates() ? UiKit.badge("● LIVE  (RMI callbacks)", Theme.SUCCESS)
                : UiKit.badge("live updates unavailable (firewall?)", Theme.WARNING);
        JPanel content = new JPanel(new BorderLayout());
        content.add(UiKit.toolbar(table.getSearchField(), null, live, null,
                UiKit.button("Export CSV", e -> TableExport.toCsv(this, t, "library-events"))), BorderLayout.NORTH);
        content.add(table, BorderLayout.CENTER);
        content.add(table.getCountLabel(), BorderLayout.SOUTH);
        add(UiKit.page(UiKit.header("Live events", "The server pushes each change to every connected client (LibraryCallback.onEvent)"),
                content), BorderLayout.CENTER);

        conn.addListener(e -> {
            if (loaded) model.insertRow(0, e);
        });
    }

    @Override
    public void refresh() {
        if (loaded) return;
        Async.run(this, () -> conn.service().getRecentEvents(200), list -> {
            java.util.List<LibraryEvent> rows = new java.util.ArrayList<>(list);
            java.util.Collections.reverse(rows);
            model.setRows(rows);
            loaded = true;
        });
    }
}
