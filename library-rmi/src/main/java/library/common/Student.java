package library.common;

import java.io.Serializable;

/**
 * A library member (student). The PIN is never sent back to clients.
 */
public class Student implements Serializable {

    private static final long serialVersionUID = 1L;

    private String studentId;
    private String name;
    private String email;
    private String phone;
    private int activeLoans;
    private double unpaidFines;

    public Student() {
    }

    public Student(String studentId, String name, String email, String phone) {
        this.studentId = studentId;
        this.name = name;
        this.email = email;
        this.phone = phone;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public int getActiveLoans() {
        return activeLoans;
    }

    public void setActiveLoans(int activeLoans) {
        this.activeLoans = activeLoans;
    }

    public double getUnpaidFines() {
        return unpaidFines;
    }

    public void setUnpaidFines(double unpaidFines) {
        this.unpaidFines = unpaidFines;
    }

    @Override
    public String toString() {
        return studentId + " - " + name;
    }
}
