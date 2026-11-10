package employee.service;

import employee.client.SoapClient;
import employee.model.Employee;
import employee.model.EmployeeStats;
import employee.model.LoginResult;
import employee.server.Database;
import employee.server.EmployeePublisher;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.xml.namespace.QName;
import javax.xml.ws.Endpoint;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceException;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end tests over real SOAP/HTTP: both services are published on a free local port with the
 * same handler chain as the real server, and called through the same client class as the GUIs.
 */
class EmployeeSoapServiceTest {

    @TempDir
    static Path dir;

    private static Database db;
    private static List<Endpoint> endpoints;
    private static String baseUrl;

    @BeforeAll
    static void startServer() throws Exception {
        int port;
        try (ServerSocket s = new ServerSocket(0)) {
            port = s.getLocalPort();
        }
        baseUrl = "http://localhost:" + port;
        db = new Database(dir.resolve("employees.db").toString());
        endpoints = EmployeePublisher.publish(db, baseUrl);
    }

    @AfterAll
    static void stopServer() throws Exception {
        if (endpoints != null) endpoints.forEach(Endpoint::stop);
        if (db != null) db.close();
    }

    private static SoapClient loggedIn(String user, String password) throws Exception {
        SoapClient c = new SoapClient(baseUrl);
        LoginResult r = c.login(user, password);
        assertTrue(r.isSuccess(), r.getMessage());
        return c;
    }

    @Test
    void loginReturnsTokenAndRole() throws Exception {
        SoapClient c = new SoapClient(baseUrl);
        LoginResult r = c.login("admin", "admin123");
        assertTrue(r.isSuccess());
        assertEquals("ADMIN", r.getRole());
        assertNotNull(r.getToken());
        assertTrue(c.auth().validateToken(r.getToken()).isSuccess());
    }

    @Test
    void wrongPasswordIsRefused() throws Exception {
        LoginResult r = new SoapClient(baseUrl).login("admin", "nope");
        assertFalse(r.isSuccess());
        assertNull(r.getToken());
    }

    @Test
    void employeeServiceRejectsRequestsWithoutAToken() throws Exception {
        // a plain JAX-WS client, without the handler that adds the token header
        EmployeeService anonymous = Service.create(new URL(baseUrl + "/EmployeeService?wsdl"),
                new QName(EmployeeService.NAMESPACE, "EmployeeServiceImplService")).getPort(EmployeeService.class);
        assertThrows(WebServiceException.class, () -> anonymous.getEmployee(1));
    }

    @Test
    void adminCanAddGetUpdateAndDeleteAnEmployee() throws Exception {
        EmployeeService svc = loggedIn("admin", "admin123").employees();

        Employee e = new Employee(500, "  Test Person ", 1200, "QA", "Tester", "test@dscc.edu", "0990000000", "2025-01-01");
        assertTrue(svc.addEmployee(e).startsWith("Success"));
        assertTrue(svc.addEmployee(e).startsWith("Failure"), "duplicate id is refused");

        Employee stored = svc.getEmployee(500);
        assertEquals("Test Person", stored.getName(), "name is trimmed");
        assertEquals(1200, stored.getSalary());

        stored.setSalary(1500);
        assertTrue(svc.updateEmployee(stored).startsWith("Success"));
        assertEquals(1500, svc.getEmployee(500).getSalary());

        assertTrue(svc.deleteEmployee(500).startsWith("Success"));
        assertNull(svc.getEmployee(500));
        assertTrue(svc.deleteEmployee(500).startsWith("Failure"));
    }

    @Test
    void invalidEmployeeDataIsRefused() throws Exception {
        EmployeeService svc = loggedIn("admin", "admin123").employees();
        assertTrue(svc.addEmployee(new Employee(0, "No Id", 100)).contains("id must be a positive number"));
        assertTrue(svc.addEmployee(new Employee(501, " ", 100)).contains("name is required"));
        assertTrue(svc.addEmployee(new Employee(502, "Negative", -1)).contains("salary cannot be negative"));
        assertTrue(svc.addEmployee(new Employee(503, "Mail", 1, null, null, "not-an-email", null, null))
                .contains("invalid email"));
    }

    @Test
    void adminCannotDeleteOwnRecord() throws Exception {
        EmployeeService svc = loggedIn("admin", "admin123").employees();
        assertTrue(svc.deleteEmployee(1).contains("cannot delete your own"));
    }

    @Test
    void employeeRoleCanOnlyReadOwnRecord() throws Exception {
        SoapClient c = loggedIn("employee", "emp123");
        EmployeeService svc = c.employees();
        int own = c.getSession().getEmployeeId();

        assertEquals(own, svc.getEmployee(own).getId());
        assertThrows(WebServiceException.class, () -> svc.getEmployee(own == 1 ? 3 : 1));
        assertThrows(WebServiceException.class, svc::listEmployees);
        assertTrue(svc.addEmployee(new Employee(600, "X", 1)).startsWith("Failure: only an ADMIN"));
        assertTrue(svc.deleteEmployee(3).startsWith("Failure: only an ADMIN"));
    }

    @Test
    void raiseUpdatesSalariesOfOneDepartmentOnly() throws Exception {
        EmployeeService svc = loggedIn("admin", "admin123").employees();
        double finance = svc.getEmployee(3).getSalary();
        double hr = svc.getEmployee(2).getSalary();

        assertTrue(svc.applyRaise("Finance", 10).startsWith("Success"));
        assertEquals(finance * 1.10, svc.getEmployee(3).getSalary(), 0.01);
        assertEquals(hr, svc.getEmployee(2).getSalary(), 0.001);

        assertTrue(svc.applyRaise("Finance", 0).startsWith("Failure"));
        assertTrue(svc.applyRaise("Nowhere", 5).startsWith("Failure"));
    }

    @Test
    void statisticsAreConsistent() throws Exception {
        EmployeeStats s = loggedIn("admin", "admin123").employees().getStatistics();
        assertTrue(s.getCount() >= 10);
        assertTrue(s.getMinSalary() <= s.getAverageSalary() && s.getAverageSalary() <= s.getMaxSalary());
        assertFalse(s.getDepartments().isEmpty());
    }

    @Test
    void logoutInvalidatesTheToken() throws Exception {
        SoapClient c = loggedIn("librarian", "lib123");
        String token = c.getToken();
        c.logout();
        assertFalse(new SoapClient(baseUrl).auth().validateToken(token).isSuccess());
    }
}
