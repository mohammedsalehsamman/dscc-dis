package employee.model;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Employee entity exchanged between the SOAP service and its clients.
 * Required fields (from the assignment): id, name, salary.
 * Extra fields: department, position, email, phone, hireDate (yyyy-MM-dd).
 * JAXB needs a public no-arg constructor and getters/setters to convert
 * the object to/from XML inside the SOAP message.
 */
@XmlRootElement(name = "employee")
public class Employee {

    private int id;
    private String name;
    private double salary;
    private String department;
    private String position;
    private String email;
    private String phone;
    private String hireDate;

    public Employee() {
    }

    public Employee(int id, String name, double salary) {
        this.id = id;
        this.name = name;
        this.salary = salary;
    }

    public Employee(int id, String name, double salary, String department, String position,
                    String email, String phone, String hireDate) {
        this(id, name, salary);
        this.department = department;
        this.position = position;
        this.email = email;
        this.phone = phone;
        this.hireDate = hireDate;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getSalary() {
        return salary;
    }

    public void setSalary(double salary) {
        this.salary = salary;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getHireDate() {
        return hireDate;
    }

    public void setHireDate(String hireDate) {
        this.hireDate = hireDate;
    }

    @Override
    public String toString() {
        return "Employee{id=" + id + ", name='" + name + "', salary=" + salary
                + (department != null ? ", department='" + department + "'" : "") + "}";
    }
}
