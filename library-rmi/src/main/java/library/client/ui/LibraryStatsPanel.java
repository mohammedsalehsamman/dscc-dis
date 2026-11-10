package library.client.ui;

import dscc.ui.Async;
import dscc.ui.BarChartPanel;
import dscc.ui.PieChartPanel;
import dscc.ui.Refreshable;
import dscc.ui.StatCard;
import dscc.ui.Theme;
import dscc.ui.UiKit;
import library.client.LibraryConnection;
import library.common.LibraryStats;

import javax.swing.*;
import java.awt.*;

/**
 * Library dashboard: key numbers and four charts. Refreshes live when something changes.
 */
public class LibraryStatsPanel extends JPanel implements Refreshable {

    private final LibraryConnection conn;
    private final StatCard titles = new StatCard("Titles", Theme.ACCENT);
    private final StatCard copies = new StatCard("Copies available", Theme.SUCCESS);
    private final StatCard active = new StatCard("Books on loan", Theme.PURPLE);
    private final StatCard overdue = new StatCard("Overdue", Theme.DANGER);
    private final StatCard students = new StatCard("Students", Theme.TEAL);
    private final StatCard fines = new StatCard("Fines collected", Theme.WARNING);
    private final BarChartPanel perMonth = new BarChartPanel("Loans per month");
    private final BarChartPanel topBooks = new BarChartPanel("Most borrowed books");
    private final PieChartPanel categories = new PieChartPanel("Titles by category");
    private final BarChartPanel perEmployee = new BarChartPanel("Loans issued per librarian");

    public LibraryStatsPanel(LibraryConnection conn) {
        super(new BorderLayout());
        this.conn = conn;
        JPanel cards = new JPanel(new GridLayout(1, 6, 10, 0));
        for (StatCard c : new StatCard[]{titles, copies, active, overdue, students, fines}) cards.add(c);
        topBooks.setColor(Theme.SUCCESS);
        perEmployee.setColor(Theme.PURPLE);
        JPanel charts = new JPanel(new GridLayout(2, 2, 12, 12));
        charts.add(perMonth);
        charts.add(topBooks);
        charts.add(categories);
        charts.add(perEmployee);
        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.add(cards, BorderLayout.NORTH);
        content.add(charts, BorderLayout.CENTER);
        add(UiKit.page(UiKit.header("Library statistics", "RMI: getStatistics - updates live through callbacks"), content),
                BorderLayout.CENTER);
        UiKit.shortcut(this, "F5", this::refresh);
        conn.addListener(ev -> {
            if (isShowing()) refresh();
        });
    }

    @Override
    public void refresh() {
        Async.run(this, () -> conn.service().getStatistics(), this::show);
    }

    private void show(LibraryStats s) {
        titles.setValue(String.valueOf(s.titles));
        titles.setNote(s.totalCopies + " copies in total");
        copies.setValue(String.valueOf(s.availableCopies));
        copies.setNote("of " + s.totalCopies);
        active.setValue(String.valueOf(s.activeLoans));
        active.setNote(s.loansThisMonth + " issued this month");
        overdue.setValue(String.valueOf(s.overdueLoans));
        overdue.setNote(s.waitingReservations + " open reservations");
        students.setValue(String.valueOf(s.students));
        students.setNote("registered members");
        fines.setValue(LibraryActions.money(s.finesCollected));
        fines.setNote(LibraryActions.money(s.finesPending) + " pending");
        perMonth.setData(s.loansPerMonth);
        topBooks.setData(s.topBooks);
        categories.setData(s.titlesPerCategory);
        perEmployee.setData(s.loansPerEmployeeName);
    }
}
