package employee.model;

/**
 * Result of AuthService.login / validateToken.
 * success=false means wrong credentials or an expired token (see message).
 */
public class LoginResult {

    private boolean success;
    private String message;
    private String token;
    private String username;
    private String role;
    private int employeeId;
    private String fullName;

    public LoginResult() {
    }

    public static LoginResult failure(String message) {
        LoginResult r = new LoginResult();
        r.setSuccess(false);
        r.setMessage(message);
        return r;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
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

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
}
