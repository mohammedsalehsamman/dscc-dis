package employee.service;

import employee.model.ActivityEntry;
import employee.model.Employee;
import employee.model.EmployeeStats;
import employee.server.ActivityDao;
import employee.server.AuthContext;
import employee.server.AuthServerHandler;
import employee.server.Database;
import employee.server.EmployeeDao;

import javax.annotation.Resource;
import javax.jws.WebService;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.WebServiceException;
import java.sql.SQLException;
import java.util.List;

/**
 * Implementation of the Employee SOAP web service, backed by SQLite.
 * Authentication (who is calling) is done by {@link AuthServerHandler};
 * this class does the authorization (what the caller's role may do).
 */
@WebService(
        serviceName = "EmployeeServiceImplService",
        portName = "EmployeeServiceImplPort",
        targetNamespace = EmployeeService.NAMESPACE,
        endpointInterface = "employee.service.EmployeeService")
public class EmployeeServiceImpl implements EmployeeService {

    @Resource
    private WebServiceContext context;

    private final Database db;
    private final EmployeeDao employees;
    private final ActivityDao activity;

    public EmployeeServiceImpl(Database db) {
        this.db = db;
        this.employees = new EmployeeDao(db);
        this.activity = new ActivityDao(db);
    }

    // ------------------------------------------------ required operations

    @Override
    public String addEmployee(Employee employee) {
        synchronized (db) {
            AuthContext auth = auth();
            String denied = requireAdmin(auth);
            if (denied != null) return denied;
            String error = validate(employee);
            if (error != null) return "Failure: " + error;
            try {
                if (employees.exists(employee.getId())) {
                    return "Failure: employee with id " + employee.getId() + " already exists";
                }
                employees.insert(clean(employee));
                activity.log(auth.username, "ADD_EMPLOYEE", "#" + employee.getId() + " " + employee.getName().trim());
                return "Success: employee " + employee.getId() + " added";
            } catch (SQLException e) {
                return "Failure: database error - " + e.getMessage();
            }
        }
    }

    @Override
    public Employee getEmployee(int id) {
        synchronized (db) {
            AuthContext auth = auth();
            if (!auth.isAdmin() && auth.employeeId != id) {
                throw new WebServiceException("Access denied: you can only view your own employee record");
            }
            try {
                return employees.find(id);
            } catch (SQLException e) {
                throw new WebServiceException("Database error: " + e.getMessage());
            }
        }
    }

    @Override
    public String updateEmployee(Employee employee) {
        synchronized (db) {
            AuthContext auth = auth();
            String denied = requireAdmin(auth);
            if (denied != null) return denied;
            String error = validate(employee);
            if (error != null) return "Failure: " + error;
            try {
                Employee old = employees.find(employee.getId());
                if (old == null) return "Failure: employee with id " + employee.getId() + " not found";
                employees.update(clean(employee));
                String change = old.getSalary() != employee.getSalary()
                        ? " (salary " + old.getSalary() + " -> " + employee.getSalary() + ")" : "";
                activity.log(auth.username, "UPDATE_EMPLOYEE", "#" + employee.getId() + " " + employee.getName().trim() + change);
                return "Success: employee " + employee.getId() + " updated";
            } catch (SQLException e) {
                return "Failure: database error - " + e.getMessage();
            }
        }
    }

    @Override
    public String deleteEmployee(int id) {
        synchronized (db) {
            AuthContext auth = auth();
            String denied = requireAdmin(auth);
            if (denied != null) return denied;
            if (id == auth.employeeId) return "Failure: you cannot delete your own employee record";
            try {
                Employee old = employees.find(id);
                if (old == null || !employees.delete(id)) {
                    return "Failure: employee with id " + id + " not found";
                }
                activity.log(auth.username, "DELETE_EMPLOYEE", "#" + id + " " + old.getName());
                return "Success: employee " + id + " deleted";
            } catch (SQLException e) {
                return "Failure: database error - " + e.getMessage();
            }
        }
    }

    // ------------------------------------------------ extra operations

    @Override
    public List<Employee> listEmployees() {
        synchronized (db) {
            adminOrFault();
            try {
                return employees.list();
            } catch (SQLException e) {
                throw new WebServiceException("Database error: " + e.getMessage());
            }
        }
    }

    @Override
    public List<Employee> searchEmployees(String query, String department) {
        synchronized (db) {
            adminOrFault();
            try {
                return employees.search(query, department);
            } catch (SQLException e) {
                throw new WebServiceException("Database error: " + e.getMessage());
            }
        }
    }

    @Override
    public List<String> getDepartments() {
        synchronized (db) {
            try {
                return employees.departments();
            } catch (SQLException e) {
                throw new WebServiceException("Database error: " + e.getMessage());
            }
        }
    }

    @Override
    public EmployeeStats getStatistics() {
        synchronized (db) {
            adminOrFault();
            try {
                return employees.stats();
            } catch (SQLException e) {
                throw new WebServiceException("Database error: " + e.getMessage());
            }
        }
    }

    @Override
    public String applyRaise(String department, double percent) {
        synchronized (db) {
            AuthContext auth = auth();
            String denied = requireAdmin(auth);
            if (denied != null) return denied;
            if (department == null || department.trim().isEmpty()) return "Failure: choose a department";
            if (percent <= 0 || percent > 100) return "Failure: percent must be between 0 and 100";
            try {
                int n = employees.raise(department.trim(), percent);
                if (n == 0) return "Failure: no employees in department " + department;
                activity.log(auth.username, "SALARY_RAISE", percent + "% for " + department + " (" + n + " employees)");
                return "Success: " + n + " salaries raised by " + percent + "%";
            } catch (SQLException e) {
                return "Failure: database error - " + e.getMessage();
            }
        }
    }

    @Override
    public List<ActivityEntry> getActivitySince(long lastId) {
        synchronized (db) {
            adminOrFault();
            try {
                return activity.since(lastId);
            } catch (SQLException e) {
                throw new WebServiceException("Database error: " + e.getMessage());
            }
        }
    }

    // ------------------------------------------------ helpers

    private AuthContext auth() {
        AuthContext a = (AuthContext) context.getMessageContext().get(AuthServerHandler.AUTH_KEY);
        if (a == null) throw new WebServiceException("Authentication required");
        return a;
    }

    private static String requireAdmin(AuthContext auth) {
        return auth.isAdmin() ? null : "Failure: only an ADMIN can do this (your role is " + auth.role + ")";
    }

    private void adminOrFault() {
        AuthContext a = auth();
        if (!a.isAdmin()) throw new WebServiceException("Access denied: ADMIN role required (your role is " + a.role + ")");
    }

    private static String validate(Employee e) {
        if (e == null) return "no employee data was sent";
        if (e.getId() <= 0) return "id must be a positive number";
        if (e.getName() == null || e.getName().trim().isEmpty()) return "name is required";
        if (e.getSalary() < 0) return "salary cannot be negative";
        if (e.getEmail() != null && !e.getEmail().trim().isEmpty() && !e.getEmail().trim().matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            return "invalid email address";
        }
        if (e.getHireDate() != null && !e.getHireDate().trim().isEmpty() && !e.getHireDate().trim().matches("\\d{4}-\\d{2}-\\d{2}")) {
            return "hire date must be yyyy-MM-dd";
        }
        return null;
    }

    private static Employee clean(Employee e) {
        return new Employee(e.getId(), e.getName().trim(), e.getSalary(), trim(e.getDepartment()),
                trim(e.getPosition()), trim(e.getEmail()), trim(e.getPhone()), trim(e.getHireDate()));
    }

    private static String trim(String s) {
        return s == null || s.trim().isEmpty() ? null : s.trim();
    }
}
