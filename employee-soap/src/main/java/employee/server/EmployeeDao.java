package employee.server;

import employee.model.DepartmentStat;
import employee.model.Employee;
import employee.model.EmployeeStats;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data access for the employees table. Uses PreparedStatement only (no SQL injection).
 */
public class EmployeeDao {

    private static final String COLS = "id,name,salary,department,position,email,phone,hire_date";
    private final Connection conn;

    public EmployeeDao(Database db) {
        this.conn = db.conn();
    }

    public boolean exists(int id) throws SQLException {
        return find(id) != null;
    }

    public Employee find(int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT " + COLS + " FROM employees WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public void insert(Employee e) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO employees (" + COLS + ") VALUES (?,?,?,?,?,?,?,?)")) {
            bind(ps, e);
            ps.executeUpdate();
        }
    }

    public boolean update(Employee e) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE employees SET id=?, name=?, salary=?, department=?, position=?, email=?, phone=?, hire_date=? WHERE id=?")) {
            bind(ps, e);
            ps.setInt(9, e.getId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM employees WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    public List<Employee> list() throws SQLException {
        return search(null, null);
    }

    /** Text search in name/position/email/phone plus an optional exact department filter. */
    public List<Employee> search(String query, String department) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT " + COLS + " FROM employees WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (query != null && !query.trim().isEmpty()) {
            sql.append(" AND (name LIKE ? OR position LIKE ? OR email LIKE ? OR phone LIKE ? OR CAST(id AS TEXT) = ?)");
            String like = "%" + query.trim() + "%";
            params.add(like);
            params.add(like);
            params.add(like);
            params.add(like);
            params.add(query.trim());
        }
        if (department != null && !department.trim().isEmpty()) {
            sql.append(" AND department = ?");
            params.add(department.trim());
        }
        sql.append(" ORDER BY id");
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            List<Employee> list = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
            return list;
        }
    }

    public List<String> departments() throws SQLException {
        List<String> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT DISTINCT department FROM employees WHERE department IS NOT NULL AND department <> '' ORDER BY department");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(rs.getString(1));
        }
        return list;
    }

    public EmployeeStats stats() throws SQLException {
        EmployeeStats s = new EmployeeStats();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*), COALESCE(SUM(salary),0), COALESCE(AVG(salary),0), COALESCE(MAX(salary),0), COALESCE(MIN(salary),0) FROM employees");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                s.setCount(rs.getInt(1));
                s.setTotalSalary(rs.getDouble(2));
                s.setAverageSalary(Math.round(rs.getDouble(3) * 100) / 100.0);
                s.setMaxSalary(rs.getDouble(4));
                s.setMinSalary(rs.getDouble(5));
            }
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COALESCE(NULLIF(department,''),'(none)') d, COUNT(*), SUM(salary), AVG(salary)"
                        + " FROM employees GROUP BY d ORDER BY d");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                s.getDepartments().add(new DepartmentStat(rs.getString(1), rs.getInt(2), rs.getDouble(3),
                        Math.round(rs.getDouble(4) * 100) / 100.0));
            }
        }
        return s;
    }

    /** Raises the salary of every employee in a department (or everyone if department is "ALL"). */
    public int raise(String department, double percent) throws SQLException {
        boolean all = "ALL".equalsIgnoreCase(department);
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE employees SET salary = ROUND(salary * (1 + ? / 100.0), 2)" + (all ? "" : " WHERE department = ?"))) {
            ps.setDouble(1, percent);
            if (!all) ps.setString(2, department);
            return ps.executeUpdate();
        }
    }

    private static void bind(PreparedStatement ps, Employee e) throws SQLException {
        ps.setInt(1, e.getId());
        ps.setString(2, e.getName());
        ps.setDouble(3, e.getSalary());
        ps.setString(4, e.getDepartment());
        ps.setString(5, e.getPosition());
        ps.setString(6, e.getEmail());
        ps.setString(7, e.getPhone());
        ps.setString(8, e.getHireDate());
    }

    private static Employee map(ResultSet rs) throws SQLException {
        return new Employee(rs.getInt("id"), rs.getString("name"), rs.getDouble("salary"),
                rs.getString("department"), rs.getString("position"), rs.getString("email"),
                rs.getString("phone"), rs.getString("hire_date"));
    }
}
