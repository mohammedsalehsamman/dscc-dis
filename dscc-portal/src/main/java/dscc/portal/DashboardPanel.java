package dscc.portal;

import dscc.ui.Async;
import dscc.ui.BarChartPanel;
import dscc.ui.PieChartPanel;
import dscc.ui.Refreshable;
import dscc.ui.StatCard;
import dscc.ui.Theme;
import dscc.ui.UiKit;
import employee.client.ActivityPoller;
import employee.model.ActivityEntry;
import employee.model.DepartmentStat;
import employee.model.EmployeeStats;
import library.common.LibraryEvent;
import library.common.LibraryStats;

import javax.swing.*;
import java.awt.*;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;

/**
 * Combined dashboard: numbers and charts from BOTH servers on one page, plus a live feed that
 * merges RMI pushed events and SOAP polled activity.
 */
class DashboardPanel extends JPanel implements Refreshable {

    private static final DecimalFormat N = new DecimalFormat("#,##0");
    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final PortalSession session;
    private final StatCard soapCard = new StatCard("SOAP server", Theme.ACCENT);
    private final StatCard rmiCard = new StatCard("RMI server", Theme.PURPLE);
    private final StatCard employeesCard = new StatCard("Employees", Theme.ACCENT);
    private final StatCard payrollCard = new StatCard("Monthly payroll", Theme.SUCCESS);
    private final StatCard titlesCard = new StatCard("Book titles", Theme.TEAL);
    private final StatCard loansCard = new StatCard("Books on loan", Theme.PURPLE);
    private final StatCard overdueCard = new StatCard("Overdue", Theme.DANGER);
    private final StatCard studentsCard = new StatCard("Students", Theme.WARNING);
    private final BarChartPanel salaryChart = new BarChartPanel("Average salary by department  (SOAP)");
    private final BarChartPanel loansChart = new BarChartPanel("Loans per month  (RMI)");
    private final PieChartPanel categoryChart = new PieChartPanel("Titles by category  (RMI)");
    private final DefaultListModel<String> feed = new DefaultListModel<>();

    DashboardPanel(PortalSession session, ActivityPoller poller) {
        super(new BorderLayout());
        this.session = session;

        JPanel health = new JPanel(new GridLayout(1, 2, 12, 0));
        health.add(soapCard);
        health.add(rmiCard);

        JPanel numbers = new JPanel(new GridLayout(1, 0, 12, 0));
        if (session.isAdmin()) {
            numbers.add(employeesCard);
            numbers.add(payrollCard);
        }
        numbers.add(titlesCard);
        numbers.add(loansCard);
        numbers.add(overdueCard);
        numbers.add(studentsCard);

        JPanel charts = new JPanel(new GridLayout(1, 0, 12, 0));
        if (session.isAdmin()) {
            salaryChart.setFormat("#,##0");
            salaryChart.setColor(Theme.PURPLE);
            charts.add(salaryChart);
        }
        charts.add(loansChart);
        charts.add(categoryChart);
        charts.setPreferredSize(new Dimension(10, 260));

        JList<String> list = new JList<>(feed);
        list.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane feedScroll = new JScrollPane(list);
        feedScroll.setPreferredSize(new Dimension(10, 200));
        JPanel feedBox = new JPanel(new BorderLayout(0, 6));
        JPanel feedHead = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JLabel ft = new JLabel("Live activity   ");
        ft.putClientProperty("FlatLaf.style", "font: +2 bold");
        feedHead.add(ft);
        feedHead.add(UiKit.badge("RMI push" + (session.isAdmin() ? " + SOAP polling" : ""), Theme.SUCCESS));
        feedBox.add(feedHead, BorderLayout.NORTH);
        feedBox.add(feedScroll, BorderLayout.CENTER);

        JPanel stack = new JPanel();
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        for (JComponent c : new JComponent[]{health, numbers, charts, feedBox}) {
            c.setAlignmentX(LEFT_ALIGNMENT);
            stack.add(c);
            stack.add(Box.createVerticalStrut(12));
        }
        JScrollPane scroll = new JScrollPane(stack);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(UiKit.page(UiKit.header("Dashboard", LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy"))
                + "  ·  data from both servers"), scroll), BorderLayout.CENTER);

        if (session.library != null) {
            session.library.addListener(ev -> {
                addFeed("RMI ", ev.getTime().format(HM), ev.getType() + "  " + ev.getMessage());
                if (isShowing()) refresh();
            });
        }
        if (poller != null) {
            poller.addListener((entries, initial) -> {
                for (ActivityEntry a : entries) {
                    addFeed("SOAP", a.getTime().length() > 11 ? a.getTime().substring(11) : a.getTime(),
                            a.getAction() + "  " + a.getUsername() + ": " + a.getDetails());
                }
                if (!initial && isShowing()) refresh();
            });
        }
        if (session.library != null) {
            Async.run(this, () -> session.library.service().getRecentEvents(15), events -> {
                for (LibraryEvent ev : events) addFeed("RMI ", ev.getTime().format(HM), ev.getType() + "  " + ev.getMessage());
            }, err -> { });
        }
    }

    private void addFeed(String source, String time, String text) {
        feed.add(0, time + "  [" + source + "]  " + text);
        while (feed.size() > 200) feed.removeElementAt(feed.size() - 1);
    }

    @Override
    public void refresh() {
        soapCard.setValue(session.soap == null ? "-" : "…");
        Async.run(this, () -> {
            Object[] r = new Object[4];
            r[0] = session.soap != null && session.soap.ping();
            r[1] = session.library != null && session.library.ping();
            if (session.isAdmin() && Boolean.TRUE.equals(r[0])) r[2] = session.soap.employees().getStatistics();
            if (Boolean.TRUE.equals(r[1])) r[3] = session.library.service().getStatistics();
            return r;
        }, r -> {
            boolean soapUp = (Boolean) r[0], rmiUp = (Boolean) r[1];
            soapCard.setValue(soapUp ? "Online" : "Offline");
            soapCard.setNote(session.soap == null ? "not used for students" : session.soap.getBaseUrl() + "  ·  Employee + Auth services");
            rmiCard.setValue(rmiUp ? "Online" : "Offline");
            rmiCard.setNote(session.library == null ? "not connected"
                    : "rmi://" + session.library.getHost() + "/LibraryService" + (session.library.hasLiveUpdates() ? "  ·  live callbacks ON" : ""));
            if (r[2] != null) {
                EmployeeStats es = (EmployeeStats) r[2];
                employeesCard.setValue(String.valueOf(es.getCount()));
                employeesCard.setNote(es.getDepartments().size() + " departments");
                payrollCard.setValue(N.format(es.getTotalSalary()));
                payrollCard.setNote("average " + N.format(es.getAverageSalary()));
                LinkedHashMap<String, Double> avg = new LinkedHashMap<>();
                for (DepartmentStat d : es.getDepartments()) avg.put(d.getDepartment(), d.getAverageSalary());
                salaryChart.setData(avg);
            }
            if (r[3] != null) {
                LibraryStats ls = (LibraryStats) r[3];
                titlesCard.setValue(String.valueOf(ls.titles));
                titlesCard.setNote(ls.availableCopies + " of " + ls.totalCopies + " copies available");
                loansCard.setValue(String.valueOf(ls.activeLoans));
                loansCard.setNote(ls.loansThisMonth + " issued this month");
                overdueCard.setValue(String.valueOf(ls.overdueLoans));
                overdueCard.setNote(N.format(ls.finesPending) + " in pending fines");
                studentsCard.setValue(String.valueOf(ls.students));
                studentsCard.setNote(ls.waitingReservations + " open reservations");
                loansChart.setData(ls.loansPerMonth);
                categoryChart.setData(ls.titlesPerCategory);
            }
        });
    }
}
