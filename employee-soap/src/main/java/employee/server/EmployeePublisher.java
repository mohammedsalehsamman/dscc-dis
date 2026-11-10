package employee.server;

import dscc.ui.ServerMonitor;
import dscc.util.ServerLog;
import employee.service.AuthServiceImpl;
import employee.service.EmployeeServiceImpl;

import javax.xml.ws.Endpoint;
import javax.xml.ws.handler.Handler;
import java.util.ArrayList;
import java.util.List;

/**
 * Publishes the two SOAP web services on the built-in HTTP server:
 * <ul>
 *   <li>/EmployeeService - employee management (token required in SOAP header)</li>
 *   <li>/AuthService     - login / tokens / user accounts</li>
 * </ul>
 * Usage: EmployeePublisher [baseUrl] [--nogui]   (default base URL http://0.0.0.0:8080)
 */
public class EmployeePublisher {

    public static void main(String[] args) throws Exception {
        String base = "http://0.0.0.0:8080";
        for (String a : args) if (a.startsWith("http")) base = a.endsWith("/") ? a.substring(0, a.length() - 1) : a;

        Database db = new Database("data/employees.db");
        publish(db, base);

        String shown = base.replace("0.0.0.0", "localhost");
        ServerLog.info("==============================================");
        ServerLog.info(" Employee SOAP Web Services are running");
        ServerLog.info(" Employee WSDL : " + shown + "/EmployeeService?wsdl");
        ServerLog.info(" Auth WSDL     : " + shown + "/AuthService?wsdl");
        ServerLog.info(" Demo accounts : admin/admin123, librarian/lib123, employee/emp123");
        ServerLog.info("==============================================");

        ServerMonitor.start(args, "Employee SOAP Server", new String[]{
                "Employee WSDL:  " + shown + "/EmployeeService?wsdl",
                "Auth WSDL:      " + shown + "/AuthService?wsdl"}, null);
        // Endpoint.publish starts a non-daemon HTTP server thread, so the JVM keeps running.
    }

    /**
     * Publishes both services under the given base URL (e.g. http://0.0.0.0:8080).
     * Returns the endpoints so that callers (tests) can stop them.
     */
    public static List<Endpoint> publish(Database db, String base) {
        // Employee service with the authentication handler in its handler chain
        Endpoint employeeEndpoint = Endpoint.create(new EmployeeServiceImpl(db));
        @SuppressWarnings("rawtypes")
        List<Handler> chain = new ArrayList<>();
        chain.add(new AuthServerHandler(db));
        employeeEndpoint.getBinding().setHandlerChain(chain);
        employeeEndpoint.publish(base + "/EmployeeService");

        // Authentication service (public)
        Endpoint authEndpoint = Endpoint.publish(base + "/AuthService", new AuthServiceImpl(db));
        return List.of(employeeEndpoint, authEndpoint);
    }
}
