package employee.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Payroll statistics returned by EmployeeService.getStatistics().
 */
public class EmployeeStats {

    private int count;
    private double totalSalary;
    private double averageSalary;
    private double maxSalary;
    private double minSalary;
    private List<DepartmentStat> departments = new ArrayList<>();

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public double getTotalSalary() {
        return totalSalary;
    }

    public void setTotalSalary(double totalSalary) {
        this.totalSalary = totalSalary;
    }

    public double getAverageSalary() {
        return averageSalary;
    }

    public void setAverageSalary(double averageSalary) {
        this.averageSalary = averageSalary;
    }

    public double getMaxSalary() {
        return maxSalary;
    }

    public void setMaxSalary(double maxSalary) {
        this.maxSalary = maxSalary;
    }

    public double getMinSalary() {
        return minSalary;
    }

    public void setMinSalary(double minSalary) {
        this.minSalary = minSalary;
    }

    public List<DepartmentStat> getDepartments() {
        return departments;
    }

    public void setDepartments(List<DepartmentStat> departments) {
        this.departments = departments;
    }
}
