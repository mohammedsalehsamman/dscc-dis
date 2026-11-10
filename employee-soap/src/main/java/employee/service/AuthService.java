package employee.service;

import employee.model.LoginResult;
import employee.model.UserAccount;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import java.util.List;

/**
 * Authentication web service (identity provider for the whole system).
 * The token returned by login is used by EmployeeService (SOAP header) and by the
 * Library RMI server, which validates it by calling {@link #validateToken}.
 */
@WebService(name = "AuthService", targetNamespace = AuthService.NAMESPACE)
public interface AuthService {

    String NAMESPACE = "http://auth.employee/";

    @WebMethod
    @WebResult(name = "result")
    LoginResult login(@WebParam(name = "username") String username,
                      @WebParam(name = "password") String password);

    @WebMethod
    @WebResult(name = "message")
    String logout(@WebParam(name = "token") String token);

    /** Returns who owns this token (success=false if invalid or expired). */
    @WebMethod
    @WebResult(name = "result")
    LoginResult validateToken(@WebParam(name = "token") String token);

    @WebMethod
    @WebResult(name = "message")
    String changePassword(@WebParam(name = "token") String token,
                          @WebParam(name = "oldPassword") String oldPassword,
                          @WebParam(name = "newPassword") String newPassword);

    // -------- user management (ADMIN only)

    @WebMethod
    @WebResult(name = "message")
    String createUser(@WebParam(name = "token") String token,
                      @WebParam(name = "username") String username,
                      @WebParam(name = "password") String password,
                      @WebParam(name = "role") String role,
                      @WebParam(name = "employeeId") int employeeId);

    @WebMethod
    @WebResult(name = "user")
    List<UserAccount> listUsers(@WebParam(name = "token") String token);

    @WebMethod
    @WebResult(name = "message")
    String deleteUser(@WebParam(name = "token") String token,
                      @WebParam(name = "username") String username);

    @WebMethod
    @WebResult(name = "message")
    String resetPassword(@WebParam(name = "token") String token,
                         @WebParam(name = "username") String username,
                         @WebParam(name = "newPassword") String newPassword);
}
