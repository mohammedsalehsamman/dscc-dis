package employee.service;

import dscc.util.ServerLog;
import employee.model.LoginResult;
import employee.model.UserAccount;
import employee.server.ActivityDao;
import employee.server.AuthContext;
import employee.server.Database;
import employee.server.EmployeeDao;
import employee.server.UserDao;

import javax.jws.WebService;
import javax.xml.ws.WebServiceException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

/**
 * Implementation of the authentication web service.
 */
@WebService(
        serviceName = "AuthServiceImplService",
        portName = "AuthServiceImplPort",
        targetNamespace = AuthService.NAMESPACE,
        endpointInterface = "employee.service.AuthService")
public class AuthServiceImpl implements AuthService {

    private static final List<String> ROLES = Arrays.asList("ADMIN", "LIBRARIAN", "EMPLOYEE");

    private final Database db;
    private final UserDao users;
    private final EmployeeDao employees;
    private final ActivityDao activity;

    public AuthServiceImpl(Database db) {
        this.db = db;
        this.users = new UserDao(db);
        this.employees = new EmployeeDao(db);
        this.activity = new ActivityDao(db);
    }

    @Override
    public LoginResult login(String username, String password) {
        ServerLog.request();
        synchronized (db) {
            try {
                if (username == null || password == null) return LoginResult.failure("Username and password are required");
                AuthContext a = users.checkPassword(username.trim(), password);
                if (a == null) {
                    activity.log(username, "LOGIN_FAILED", "wrong username or password");
                    return LoginResult.failure("Wrong username or password");
                }
                String token = users.createSession(a.username);
                activity.log(a.username, "LOGIN", a.fullName + " (" + a.role + ")");
                return toResult(new AuthContext(a.username, a.role, a.employeeId, a.fullName, token), "Welcome " + a.fullName);
            } catch (SQLException e) {
                return LoginResult.failure("Database error: " + e.getMessage());
            }
        }
    }

    @Override
    public String logout(String token) {
        ServerLog.request();
        synchronized (db) {
            try {
                AuthContext a = users.findSession(token);
                users.deleteSession(token);
                if (a != null) activity.log(a.username, "LOGOUT", a.fullName);
                return "Success: logged out";
            } catch (SQLException e) {
                return "Failure: " + e.getMessage();
            }
        }
    }

    @Override
    public LoginResult validateToken(String token) {
        ServerLog.request();
        synchronized (db) {
            try {
                AuthContext a = users.findSession(token);
                if (a == null) return LoginResult.failure("Invalid or expired token");
                return toResult(a, "Valid token");
            } catch (SQLException e) {
                return LoginResult.failure("Database error: " + e.getMessage());
            }
        }
    }

    @Override
    public String changePassword(String token, String oldPassword, String newPassword) {
        ServerLog.request();
        synchronized (db) {
            try {
                AuthContext a = users.findSession(token);
                if (a == null) return "Failure: please login again";
                if (users.checkPassword(a.username, oldPassword) == null) return "Failure: current password is wrong";
                if (newPassword == null || newPassword.length() < 4) return "Failure: new password must have at least 4 characters";
                users.setPassword(a.username, newPassword);
                activity.log(a.username, "CHANGE_PASSWORD", "password changed");
                return "Success: password changed";
            } catch (SQLException e) {
                return "Failure: " + e.getMessage();
            }
        }
    }

    @Override
    public String createUser(String token, String username, String password, String role, int employeeId) {
        ServerLog.request();
        synchronized (db) {
            try {
                AuthContext admin = admin(token);
                if (username == null || !username.trim().matches("[A-Za-z0-9_.]{3,30}")) {
                    return "Failure: username must be 3-30 letters/digits";
                }
                if (password == null || password.length() < 4) return "Failure: password must have at least 4 characters";
                if (role == null || !ROLES.contains(role.toUpperCase())) return "Failure: role must be one of " + ROLES;
                if (!employees.exists(employeeId)) return "Failure: employee " + employeeId + " does not exist";
                if (users.exists(username.trim())) return "Failure: username already taken";
                users.create(username.trim(), password, role.toUpperCase(), employeeId);
                activity.log(admin.username, "CREATE_USER", username.trim() + " (" + role.toUpperCase() + ", employee #" + employeeId + ")");
                return "Success: user " + username.trim() + " created";
            } catch (SQLException e) {
                return "Failure: " + e.getMessage();
            }
        }
    }

    @Override
    public List<UserAccount> listUsers(String token) {
        ServerLog.request();
        synchronized (db) {
            try {
                admin(token);
                return users.list();
            } catch (SQLException e) {
                throw new WebServiceException("Database error: " + e.getMessage());
            }
        }
    }

    @Override
    public String deleteUser(String token, String username) {
        ServerLog.request();
        synchronized (db) {
            try {
                AuthContext admin = admin(token);
                if (admin.username.equals(username)) return "Failure: you cannot delete your own account";
                if (!users.delete(username)) return "Failure: user not found";
                activity.log(admin.username, "DELETE_USER", username);
                return "Success: user " + username + " deleted";
            } catch (SQLException e) {
                return "Failure: " + e.getMessage();
            }
        }
    }

    @Override
    public String resetPassword(String token, String username, String newPassword) {
        ServerLog.request();
        synchronized (db) {
            try {
                AuthContext admin = admin(token);
                if (newPassword == null || newPassword.length() < 4) return "Failure: password must have at least 4 characters";
                if (!users.setPassword(username, newPassword)) return "Failure: user not found";
                activity.log(admin.username, "RESET_PASSWORD", username);
                return "Success: password of " + username + " reset";
            } catch (SQLException e) {
                return "Failure: " + e.getMessage();
            }
        }
    }

    private AuthContext admin(String token) throws SQLException {
        AuthContext a = users.findSession(token);
        if (a == null) throw new WebServiceException("Please login again");
        if (!a.isAdmin()) throw new WebServiceException("Access denied: ADMIN role required");
        return a;
    }

    private static LoginResult toResult(AuthContext a, String message) {
        LoginResult r = new LoginResult();
        r.setSuccess(true);
        r.setMessage(message);
        r.setToken(a.token);
        r.setUsername(a.username);
        r.setRole(a.role);
        r.setEmployeeId(a.employeeId);
        r.setFullName(a.fullName);
        return r;
    }
}
