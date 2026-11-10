package employee.service;

import employee.model.ActivityEntry;
import employee.model.Employee;
import employee.model.EmployeeStats;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import java.util.List;

/**
 * Service Endpoint Interface (SEI): the contract of the Employee SOAP web service.
 * The WSDL is generated from this interface.
 * <p>
 * Every request must carry a login token in the SOAP header (&lt;auth:Token&gt;),
 * obtained from {@link AuthService#login}. The token is checked by a server-side SOAP handler,
 * so the four operations required by the assignment keep their original signatures.
 */
@WebService(name = "EmployeeService", targetNamespace = EmployeeService.NAMESPACE)
public interface EmployeeService {

    String NAMESPACE = "http://service.employee/";

    // ------------------------------------------------ operations required by the assignment

    /** Adds a new employee. Returns a Success/Failure message. (ADMIN) */
    @WebMethod
    @WebResult(name = "message")
    String addEmployee(@WebParam(name = "employee") Employee employee);

    /** Returns the employee with the given id, or null if it does not exist. */
    @WebMethod
    @WebResult(name = "employee")
    Employee getEmployee(@WebParam(name = "id") int id);

    /** Updates an existing employee. Returns a Success/Failure message. (ADMIN) */
    @WebMethod
    @WebResult(name = "message")
    String updateEmployee(@WebParam(name = "employee") Employee employee);

    /** Deletes the employee with the given id. Returns a Success/Failure message. (ADMIN) */
    @WebMethod
    @WebResult(name = "message")
    String deleteEmployee(@WebParam(name = "id") int id);

    // ------------------------------------------------ extra operations

    /** All employees ordered by id. (ADMIN) */
    @WebMethod
    @WebResult(name = "employee")
    List<Employee> listEmployees();

    /** Search by name/position/email/phone/id, optionally limited to one department. (ADMIN) */
    @WebMethod
    @WebResult(name = "employee")
    List<Employee> searchEmployees(@WebParam(name = "query") String query,
                                   @WebParam(name = "department") String department);

    /** Distinct department names. */
    @WebMethod
    @WebResult(name = "department")
    List<String> getDepartments();

    /** Payroll statistics overall and per department. (ADMIN) */
    @WebMethod
    @WebResult(name = "statistics")
    EmployeeStats getStatistics();

    /** Raises salaries of a department (or "ALL") by a percentage. (ADMIN) */
    @WebMethod
    @WebResult(name = "message")
    String applyRaise(@WebParam(name = "department") String department,
                      @WebParam(name = "percent") double percent);

    /** Activity log entries newer than lastId (use -1 for the latest 50). Used for live updates. (ADMIN) */
    @WebMethod
    @WebResult(name = "entry")
    List<ActivityEntry> getActivitySince(@WebParam(name = "lastId") long lastId);
}
