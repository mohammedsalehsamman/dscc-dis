package employee.server;

/**
 * The logged-in user behind a SOAP request (resolved from the token in the SOAP header).
 */
public class AuthContext {

    public final String username;
    public final String role;
    public final int employeeId;
    public final String fullName;
    public final String token;

    public AuthContext(String username, String role, int employeeId, String fullName, String token) {
        this.username = username;
        this.role = role;
        this.employeeId = employeeId;
        this.fullName = fullName;
        this.token = token;
    }

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }
}
