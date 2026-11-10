package employee.client.ui;

import dscc.ui.BeanTableModel;
import dscc.ui.SearchableTable;
import dscc.ui.TableExport;
import dscc.ui.Theme;
import dscc.ui.UiKit;
import employee.client.ActivityPoller;
import employee.model.ActivityEntry;

import javax.swing.*;
import java.awt.*;

/**
 * Live audit log of the Employee system (who did what, when). New entries appear at the top
 * within 3 seconds thanks to {@link ActivityPoller}.
 */
public class ActivityPanel extends JPanel {

    private final BeanTableModel<ActivityEntry> model = new BeanTableModel<ActivityEntry>()
            .column("#", Long.class, ActivityEntry::getId)
            .column("Time", String.class, ActivityEntry::getTime)
            .column("User", String.class, ActivityEntry::getUsername)
            .column("Action", String.class, ActivityEntry::getAction)
            .column("Details", String.class, ActivityEntry::getDetails);

    public ActivityPanel(ActivityPoller poller) {
        super(new BorderLayout());
        SearchableTable table = new SearchableTable(model);
        JTable t = table.getTable();
        t.getColumnModel().getColumn(0).setMaxWidth(60);
        t.getColumnModel().getColumn(1).setPreferredWidth(140);
        t.getColumnModel().getColumn(4).setPreferredWidth(320);
        table.setRowColorizer(row -> {
            ActivityEntry a = model.getRow(row);
            if (a == null) return null;
            if (a.getAction().contains("DELETE") || a.getAction().contains("FAILED")) return Theme.DANGER;
            if (a.getAction().startsWith("ADD") || a.getAction().startsWith("CREATE")) return Theme.SUCCESS;
            return null;
        });

        JLabel live = UiKit.badge("● LIVE  (polling every 3 s)", Theme.SUCCESS);
        JToolBar tb = UiKit.toolbar(table.getSearchField(), null, live, null,
                UiKit.button("Export CSV", e -> TableExport.toCsv(this, t, "activity-log")));

        JPanel content = new JPanel(new BorderLayout());
        content.add(tb, BorderLayout.NORTH);
        content.add(table, BorderLayout.CENTER);
        content.add(table.getCountLabel(), BorderLayout.SOUTH);
        add(UiKit.page(UiKit.header("Activity log", "SOAP: getActivitySince(lastId) - near real-time by polling"),
                content), BorderLayout.CENTER);

        poller.addListener((entries, initial) -> {
            for (ActivityEntry a : entries) model.insertRow(0, a);
        });
    }
}
