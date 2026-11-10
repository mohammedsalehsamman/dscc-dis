package library.common;

import java.io.Serializable;

/**
 * Returned by studentLogin: the token the student uses for self-service operations.
 */
public class StudentSession implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String token;
    private final String studentId;
    private final String name;

    public StudentSession(String token, String studentId, String name) {
        this.token = token;
        this.studentId = studentId;
        this.name = name;
    }

    public String getToken() {
        return token;
    }

    public String getStudentId() {
        return studentId;
    }

    public String getName() {
        return name;
    }
}
