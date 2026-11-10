package employee.model;

/**
 * A login account (without password data). Roles: ADMIN, LIBRARIAN, EMPLOYEE.
 */
public class UserAccount {

    public static final String ADMIN = "ADMIN";
    public static final String LIBRARIAN = "LIBRARIAN";
    public static final String EMPLOYEE = "EMPLOYEE";

    private String username;
    private String role;
    private int employeeId;
    private String employeeName;

    public UserAccount() {
    }

    public UserAccount(String username, String role, int employeeId, String employeeName) {
        this.username = username;
        this.role = role;
        this.employeeId = employeeId;
        this.employeeName = employeeName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public int getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(int employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }
}
