package library.server;

import dscc.util.ServerLog;
import library.common.AccessDeniedException;
import library.common.LibraryEvent.Type;
import library.common.StaffInfo;
import library.common.Student;
import library.common.StudentSession;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static library.server.ServiceSupport.blank;
import static library.server.ServiceSupport.log;
import static library.server.ServiceSupport.read;
import static library.server.ServiceSupport.write;

/** Library members: registration by staff, and student login sessions (student ID + PIN). */
class StudentService {

    private final Database db;
    private final StudentDao students;
    private final ReservationService reservations;
    private final EventBus events;
    private final Map<String, StudentSession> sessions = new ConcurrentHashMap<>();

    StudentService(Database db, StudentDao students, ReservationService reservations, EventBus events) {
        this.db = db;
        this.students = students;
        this.reservations = reservations;
        this.events = events;
    }

    // ------------------------------------------------------------ sessions

    StudentSession login(String studentId, String pin) throws RemoteException, AccessDeniedException {
        if (blank(studentId) || blank(pin)) throw new AccessDeniedException("Student ID and PIN are required");
        String id = studentId.trim().toUpperCase();
        Student s = read(db, () -> students.checkPin(id, pin.trim()) ? students.find(id) : null);
        if (s == null) {
            ServerLog.info("Student login FAILED: " + id);
            throw new AccessDeniedException("Wrong student ID or PIN");
        }
        StudentSession session = new StudentSession("STU-" + UUID.randomUUID(), s.getStudentId(), s.getName());
        sessions.put(session.getToken(), session);
        ServerLog.info("Student login: " + s.getName() + " (" + s.getStudentId() + ")");
        return session;
    }

    void logout(String token) {
        if (token != null) sessions.remove(token);
    }

    /** The logged-in student behind a token, or null (the token may belong to a staff member). */
    StudentSession find(String token) {
        return token == null ? null : sessions.get(token);
    }

    StudentSession require(String token) throws AccessDeniedException {
        StudentSession s = find(token);
        if (s == null) throw new AccessDeniedException("Student login required - please sign in again");
        return s;
    }

    // ------------------------------------------------------------ staff management

    String register(StaffInfo staff, Student student, String pin) {
        if (student == null || blank(student.getStudentId()) || !student.getStudentId().trim().matches("[A-Za-z0-9]{3,20}")) {
            return log("registerStudent", "Failure: student ID must be 3-20 letters/digits");
        }
        if (blank(student.getName())) return log("registerStudent", "Failure: name is required");
        if (pin == null || !pin.trim().matches("\\d{4,8}")) return log("registerStudent", "Failure: PIN must be 4-8 digits");
        return write(db, () -> {
            Student s = clean(student);
            if (students.find(s.getStudentId()) != null) {
                return log("registerStudent", "Failure: student " + s.getStudentId() + " already exists");
            }
            students.insert(s, pin.trim());
            events.publish(Type.STUDENT_REGISTERED, staff.getName() + " registered student " + s.getName(),
                    null, s.getStudentId());
            return log("registerStudent", "Success: student " + s.getStudentId() + " registered");
        });
    }

    String update(Student student, String newPinOrNull) {
        if (student == null || blank(student.getName())) return log("updateStudent", "Failure: name is required");
        if (!blank(newPinOrNull) && !newPinOrNull.trim().matches("\\d{4,8}")) {
            return log("updateStudent", "Failure: PIN must be 4-8 digits");
        }
        return write(db, () -> {
            Student s = clean(student);
            if (students.find(s.getStudentId()) == null) return log("updateStudent", "Failure: student not found");
            students.update(s);
            if (!blank(newPinOrNull)) students.setPin(s.getStudentId(), newPinOrNull.trim());
            return log("updateStudent", "Success: student " + s.getStudentId() + " updated");
        });
    }

    String delete(String studentId) {
        return write(db, () -> {
            Student s = studentId == null ? null : students.find(studentId.trim());
            if (s == null) return log("deleteStudent", "Failure: student not found");
            if (s.getActiveLoans() > 0) {
                return log("deleteStudent", "Failure: the student still has " + s.getActiveLoans() + " book(s)");
            }
            reservations.cancelAllOf(s.getStudentId());
            students.delete(s.getStudentId());
            sessions.values().removeIf(x -> x.getStudentId().equals(s.getStudentId()));
            return log("deleteStudent", "Success: student " + s.getStudentId() + " deleted");
        });
    }

    List<Student> list() throws RemoteException {
        return read(db, students::list);
    }

    private static Student clean(Student s) {
        return new Student(s.getStudentId().trim().toUpperCase(), s.getName().trim(),
                blank(s.getEmail()) ? null : s.getEmail().trim(), blank(s.getPhone()) ? null : s.getPhone().trim());
    }
}
