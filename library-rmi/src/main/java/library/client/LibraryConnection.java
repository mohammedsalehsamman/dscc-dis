package library.client;

import library.common.LibraryCallback;
import library.common.LibraryEvent;
import library.common.LibraryService;
import library.common.StaffInfo;
import library.common.StudentSession;

import javax.swing.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Client-side connection to the Library RMI server.
 * <ul>
 *   <li>looks up the remote LibraryService in the registry,</li>
 *   <li>exports a callback object so the server can push events (RMI callbacks),</li>
 *   <li>keeps the logged-in staff member or student and their token.</li>
 * </ul>
 * Listeners receive events on the Swing thread, so UI code can update tables directly.
 */
public class LibraryConnection {

    private final String host;
    private final int port;
    private LibraryService service;
    private CallbackReceiver receiver;
    private final List<Consumer<LibraryEvent>> listeners = new CopyOnWriteArrayList<>();

    private StaffInfo staff;
    private StudentSession student;

    /** The object the SERVER calls. It runs on an RMI thread, so we hop to the Swing thread. */
    private class CallbackReceiver implements LibraryCallback {
        @Override
        public void onEvent(LibraryEvent event) throws RemoteException {
            SwingUtilities.invokeLater(() -> {
                for (Consumer<LibraryEvent> l : listeners) l.accept(event);
            });
        }
    }

    public LibraryConnection(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /** Looks up the service and registers for live events. */
    public LibraryService connect() throws Exception {
        service = (LibraryService) LocateRegistry.getRegistry(host, port).lookup(LibraryService.NAME);
        service.ping();
        try {
            receiver = new CallbackReceiver();
            LibraryCallback stub = (LibraryCallback) UnicastRemoteObject.exportObject(receiver, 0);
            service.registerCallback(stub);
        } catch (RemoteException e) {
            receiver = null; // live updates are optional (e.g. blocked by a firewall)
        }
        return service;
    }

    public LibraryService service() {
        return service;
    }

    public boolean hasLiveUpdates() {
        return receiver != null;
    }

    public void addListener(Consumer<LibraryEvent> l) {
        listeners.add(l);
    }

    public void removeListener(Consumer<LibraryEvent> l) {
        listeners.remove(l);
    }

    // ---------------------------------------------------------------- sessions

    public StaffInfo loginStaff(String username, String password) throws Exception {
        staff = service.staffLogin(username, password);
        student = null;
        return staff;
    }

    /** Single sign-on: use a token already obtained from the SOAP AuthService. */
    public StaffInfo useStaffToken(String token) throws Exception {
        staff = service.verifyStaff(token);
        student = null;
        return staff;
    }

    public StudentSession loginStudent(String studentId, String pin) throws Exception {
        student = service.studentLogin(studentId, pin);
        staff = null;
        return student;
    }

    public StaffInfo getStaff() {
        return staff;
    }

    public StudentSession getStudent() {
        return student;
    }

    public boolean isStaff() {
        return staff != null;
    }

    public boolean isStudent() {
        return student != null;
    }

    public String staffToken() {
        return staff == null ? null : staff.getToken();
    }

    public String studentToken() {
        return student == null ? null : student.getToken();
    }

    /** Token of whoever is logged in. */
    public String token() {
        return staff != null ? staff.getToken() : studentToken();
    }

    public String getHost() {
        return host;
    }

    public boolean ping() {
        try {
            return service != null && "OK".equals(service.ping());
        } catch (Exception e) {
            return false;
        }
    }

    /** Unregisters the callback and logs out the student session. */
    public void close() {
        try {
            if (student != null && service != null) service.studentLogout(student.getToken());
        } catch (Exception ignored) {
            // server may be down
        }
        if (receiver != null) {
            try {
                service.unregisterCallback((LibraryCallback) UnicastRemoteObject.toStub(receiver));
            } catch (Exception ignored) {
                // server may be down
            }
            try {
                UnicastRemoteObject.unexportObject(receiver, true);
            } catch (Exception ignored) {
                // already unexported
            }
            receiver = null;
        }
        staff = null;
        student = null;
    }
}
