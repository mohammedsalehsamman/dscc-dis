package employee.client.ui;

import dscc.ui.Async;
import dscc.ui.BarChartPanel;
import dscc.ui.PieChartPanel;
import dscc.ui.Refreshable;
import dscc.ui.StatCard;
import dscc.ui.Theme;
import dscc.ui.Toast;
import dscc.ui.UiKit;
import employee.client.ActivityPoller;
import employee.client.SoapClient;
import employee.model.DepartmentStat;
import employee.model.EmployeeStats;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.LinkedHashMap;

/**
 * Payroll dashboard: summary cards, charts per department and a "salary raise" tool.
 */
public class EmployeeStatsPanel extends JPanel implements Refreshable {

    private static final DecimalFormat MONEY = new DecimalFormat("#,##0");

    private final SoapClient client;
    private final StatCard countCard = new StatCard("Employees", Theme.ACCENT);
    private final StatCard payrollCard = new StatCard("Monthly payroll", Theme.SUCCESS);
    private final StatCard avgCard = new StatCard("Average salary", Theme.PURPLE);
    private final StatCard rangeCard = new StatCard("Highest salary", Theme.WARNING);
    private final BarChartPanel avgChart = new BarChartPanel("Average salary by department");
    private final PieChartPanel headcountChart = new PieChartPanel("Headcount by department");
    private final JComboBox<String> raiseDept = new JComboBox<>();
    private final JSpinner raisePercent = new JSpinner(new SpinnerNumberModel(5.0, 0.5, 50.0, 0.5));

    public EmployeeStatsPanel(SoapClient client, ActivityPoller poller) {
        super(new BorderLayout());
        this.client = client;

        JPanel cards = new JPanel(new GridLayout(1, 4, 12, 0));
        cards.add(countCard);
        cards.add(payrollCard);
        cards.add(avgCard);
        cards.add(rangeCard);

        JPanel charts = new JPanel(new GridLayout(1, 2, 12, 0));
        avgChart.setColor(Theme.PURPLE);
        charts.add(avgChart);
        charts.add(headcountChart);

        JPanel raise = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        raise.setBorder(BorderFactory.createTitledBorder("Salary raise (SOAP: applyRaise)"));
        raise.add(new JLabel("Department:"));
        raise.add(raiseDept);
        raise.add(new JLabel("Percent:"));
        raise.add(raisePercent);
        raise.add(UiKit.primary("Apply raise", e -> onRaise()));

        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.add(cards, BorderLayout.NORTH);
        center.add(charts, BorderLayout.CENTER);
        center.add(raise, BorderLayout.SOUTH);
        center.setBorder(new EmptyBorder(0, 0, 4, 0));

        add(UiKit.page(UiKit.header("Payroll statistics", "SOAP: getStatistics - aggregated with SQL GROUP BY on the server"),
                center), BorderLayout.CENTER);
        UiKit.shortcut(this, "F5", this::refresh);

        if (poller != null) {
            poller.addListener((entries, initial) -> {
                if (!initial && isShowing() && entries.stream().anyMatch(a ->
                        a.getAction().contains("EMPLOYEE") || a.getAction().contains("SALARY"))) {
                    refresh();
                }
            });
        }
    }

    @Override
    public void refresh() {
        Async.run(this, () -> client.employees().getStatistics(), this::show);
    }

    private void show(EmployeeStats s) {
        countCard.setValue(String.valueOf(s.getCount()));
        countCard.setNote(s.getDepartments().size() + " departments");
        payrollCard.setValue(MONEY.format(s.getTotalSalary()));
        payrollCard.setNote("sum of all salaries");
        avgCard.setValue(MONEY.format(s.getAverageSalary()));
        avgCard.setNote("per employee");
        rangeCard.setValue(MONEY.format(s.getMaxSalary()));
        rangeCard.setNote("lowest: " + MONEY.format(s.getMinSalary()));

        LinkedHashMap<String, Double> avg = new LinkedHashMap<>();
        LinkedHashMap<String, Integer> heads = new LinkedHashMap<>();
        Object current = raiseDept.getSelectedItem();
        DefaultComboBoxModel<String> m = new DefaultComboBoxModel<>();
        m.addElement("ALL");
        for (DepartmentStat d : s.getDepartments()) {
            avg.put(d.getDepartment(), d.getAverageSalary());
            heads.put(d.getDepartment(), d.getCount());
            m.addElement(d.getDepartment());
        }
        raiseDept.setModel(m);
        if (current != null) raiseDept.setSelectedItem(current);
        avgChart.setFormat("#,##0");
        avgChart.setData(avg);
        headcountChart.setData(heads);
    }

    private void onRaise() {
        String dept = (String) raiseDept.getSelectedItem();
        double pct = ((Number) raisePercent.getValue()).doubleValue();
        if (dept == null) return;
        int ok = JOptionPane.showConfirmDialog(this, "Raise the salaries of " + ("ALL".equals(dept) ? "ALL employees" : dept)
                + " by " + pct + "% ?", "Confirm salary raise", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;
        Async.run(this, () -> client.employees().applyRaise(dept, pct), msg -> {
            Toast.result(this, msg);
            refresh();
        });
    }
}
