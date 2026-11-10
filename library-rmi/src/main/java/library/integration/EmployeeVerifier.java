package library.integration;

import dscc.util.ServerLog;
import dscc.ws.TokenHeaderHandler;
import library.common.AccessDeniedException;
import library.common.StaffInfo;
import library.integration.soap.auth.AuthService;
import library.integration.soap.auth.AuthServiceImplService;
import library.integration.soap.auth.LoginResult;
import library.integration.soap.employee.Employee;
import library.integration.soap.employee.EmployeeService;
import library.integration.soap.employee.EmployeeServiceImplService;

import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Integration between the two systems (RMI -> SOAP).
 * <p>
 * The Library RMI server does not keep its own staff accounts. When a staff operation arrives,
 * this class asks the Employee SOAP system:
 * <ol>
 *   <li>AuthService.validateToken(token)  - who is this? which role?</li>
 *   <li>EmployeeService.getEmployee(id)    - does the employee record still exist?</li>
 * </ol>
 * The SOAP client classes in {@code library.integration.soap.*} were generated from the WSDL
 * with the {@code wsimport} tool, so this project shares no Java code with the SOAP project:
 * the WSDL contract is enough (interoperability).
 * Results are cached for 60 seconds to avoid a SOAP call on every RMI request.
 */
public class EmployeeVerifier implements StaffVerifier {

    private static final long CACHE_MILLIS = 60_000;

    private final String baseUrl;
    private AuthService auth;
    private EmployeeService employees;
    private volatile String currentToken;
    private final Map<String, Cached> cache = new ConcurrentHashMap<>();

    private static class Cached {
        final StaffInfo info;
        final long time;

        Cached(StaffInfo info) {
            this.info = info;
            this.time = System.currentTimeMillis();
        }
    }

    /** @param baseUrl SOAP server base URL, e.g. http://localhost:8080 */
    public EmployeeVerifier(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    private AuthService auth() throws Exception {
        if (auth == null) {
            auth = new AuthServiceImplService(new URL(baseUrl + "/AuthService?wsdl")).getAuthServiceImplPort();
            TokenHeaderHandler.timeouts(auth, 3000, 10000);
        }
        return auth;
    }

    private EmployeeService employees() throws Exception {
        if (employees == null) {
            employees = new EmployeeServiceImplService(new URL(baseUrl + "/EmployeeService?wsdl")).getEmployeeServiceImplPort();
            TokenHeaderHandler.install(employees, () -> currentToken);
            TokenHeaderHandler.timeouts(employees, 3000, 10000);
        }
        return employees;
    }

    /** Staff login through the SOAP AuthService (used by the standalone library client). */
    @Override
    public synchronized StaffInfo login(String username, String password) throws AccessDeniedException {
        LoginResult r;
        try {
            r = auth().login(username, password);
        } catch (Exception e) {
            throw unreachable(e);
        }
        if (!r.isSuccess()) throw new AccessDeniedException(r.getMessage());
        return verify(r.getToken());
    }

    /** Returns the verified staff member (LIBRARIAN or ADMIN) or throws AccessDeniedException. */
    @Override
    public synchronized StaffInfo verify(String token) throws AccessDeniedException {
        if (token == null || token.trim().isEmpty()) {
            throw new AccessDeniedException("Staff login required (no token). Please sign in with an employee account.");
        }
        Cached c = cache.get(token);
        if (c != null && System.currentTimeMillis() - c.time < CACHE_MILLIS) return c.info;

        LoginResult r;
        Employee e;
        try {
            r = auth().validateToken(token);
            if (!r.isSuccess()) {
                cache.remove(token);
                throw new AccessDeniedException("Staff session is invalid or expired - please sign in again");
            }
            if (!"LIBRARIAN".equals(r.getRole()) && !"ADMIN".equals(r.getRole())) {
                throw new AccessDeniedException("Access denied: role " + r.getRole()
                        + " cannot manage the library (LIBRARIAN or ADMIN required)");
            }
            currentToken = token;
            e = employees().getEmployee(r.getEmployeeId());
        } catch (AccessDeniedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw unreachable(ex);
        } finally {
            currentToken = null;
        }
        if (e == null) {
            throw new AccessDeniedException("Access denied: employee record #" + r.getEmployeeId() + " no longer exists");
        }
        StaffInfo info = new StaffInfo(token, r.getUsername(), r.getRole(), e.getId(), e.getName(), e.getDepartment());
        cache.put(token, new Cached(info));
        ServerLog.info("SOAP verified staff: " + info.getName() + " (" + info.getRole() + ", " + info.getDepartment() + ")");
        return info;
    }

    public void forget(String token) {
        if (token != null) cache.remove(token);
    }

    private AccessDeniedException unreachable(Exception e) {
        auth = null;
        employees = null;
        ServerLog.info("SOAP server unreachable: " + e.getMessage());
        return new AccessDeniedException("Cannot verify the employee: the Employee SOAP server (" + baseUrl
                + ") is not reachable");
    }
}
