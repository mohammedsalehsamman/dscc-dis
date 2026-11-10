package employee.model;

/**
 * Headcount and salary figures for one department.
 */
public class DepartmentStat {

    private String department;
    private int count;
    private double totalSalary;
    private double averageSalary;

    public DepartmentStat() {
    }

    public DepartmentStat(String department, int count, double totalSalary, double averageSalary) {
        this.department = department;
        this.count = count;
        this.totalSalary = totalSalary;
        this.averageSalary = averageSalary;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

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
}
