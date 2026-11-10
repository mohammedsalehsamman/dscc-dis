package employee.client;

import dscc.ws.TokenHeaderHandler;
import employee.model.LoginResult;
import employee.service.AuthService;
import employee.service.EmployeeService;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Client-side connection to the two SOAP services.
 * Creates the proxies (ports) from the WSDL and installs the handler that puts the
 * login token into the SOAP header of every EmployeeService request.
 */
public class SoapClient {

    private static final QName EMPLOYEE_SERVICE = new QName(EmployeeService.NAMESPACE, "EmployeeServiceImplService");
    private static final QName AUTH_SERVICE = new QName(AuthService.NAMESPACE, "AuthServiceImplService");

    private final String baseUrl;
    private volatile String token;
    private LoginResult session;
    private EmployeeService employeePort;
    private AuthService authPort;

    /** @param baseUrl e.g. http://localhost:8080 */
    public SoapClient(String baseUrl) {
        String b = baseUrl.trim();
        while (b.endsWith("/")) b = b.substring(0, b.length() - 1);
        if (b.endsWith("?wsdl")) b = b.substring(0, b.length() - 5);
        if (b.endsWith("/EmployeeService")) b = b.substring(0, b.length() - "/EmployeeService".length());
        this.baseUrl = b;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public synchronized AuthService auth() throws Exception {
        if (authPort == null) {
            authPort = Service.create(new URL(baseUrl + "/AuthService?wsdl"), AUTH_SERVICE).getPort(AuthService.class);
            TokenHeaderHandler.timeouts(authPort, 5000, 15000);
        }
        return authPort;
    }

    public synchronized EmployeeService employees() throws Exception {
        if (employeePort == null) {
            EmployeeService port = Service.create(new URL(baseUrl + "/EmployeeService?wsdl"), EMPLOYEE_SERVICE)
                    .getPort(EmployeeService.class);
            TokenHeaderHandler.install(port, () -> token);
            TokenHeaderHandler.timeouts(port, 5000, 15000);
            employeePort = port;
        }
        return employeePort;
    }

    /** Logs in; returns the server result (check isSuccess()). */
    public LoginResult login(String username, String password) throws Exception {
        LoginResult r = auth().login(username, password);
        if (r.isSuccess()) {
            token = r.getToken();
            session = r;
        }
        return r;
    }

    /** Uses a token obtained elsewhere (single sign-on). */
    public void useSession(LoginResult r) {
        this.session = r;
        this.token = r.getToken();
    }

    public void logout() {
        try {
            if (token != null) auth().logout(token);
        } catch (Exception ignored) {
            // server may already be down
        }
        token = null;
        session = null;
    }

    public String getToken() {
        return token;
    }

    public LoginResult getSession() {
        return session;
    }

    public boolean isAdmin() {
        return session != null && "ADMIN".equals(session.getRole());
    }

    /** Quick health check: can the WSDL be downloaded? */
    public boolean ping() {
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(baseUrl + "/AuthService?wsdl").openConnection();
            c.setConnectTimeout(2000);
            c.setReadTimeout(3000);
            boolean ok = c.getResponseCode() == 200;
            c.disconnect();
            return ok;
        } catch (Exception e) {
            return false;
        }
    }
}
