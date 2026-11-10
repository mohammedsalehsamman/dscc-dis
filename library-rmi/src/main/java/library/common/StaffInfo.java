package library.common;

import java.io.Serializable;

/**
 * A verified library staff member. The data comes from the Employee SOAP system:
 * the RMI server validates the token by calling the SOAP AuthService and EmployeeService.
 */
public class StaffInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String token;
    private final String username;
    private final String role;
    private final int employeeId;
    private final String name;
    private final String department;

    public StaffInfo(String token, String username, String role, int employeeId, String name, String department) {
        this.token = token;
        this.username = username;
        this.role = role;
        this.employeeId = employeeId;
        this.name = name;
        this.department = department;
    }

    public String getToken() {
        return token;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }

    public int getEmployeeId() {
        return employeeId;
    }

    public String getName() {
        return name;
    }

    public String getDepartment() {
        return department;
    }

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }
}
