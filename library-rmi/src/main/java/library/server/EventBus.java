package library.server;

import dscc.util.ServerLog;
import library.common.LibraryCallback;
import library.common.LibraryEvent;

import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Real-time event distribution with RMI callbacks.
 * Every change is saved in the events table and pushed (on a background thread) to all
 * registered clients. Clients that are no longer reachable are removed automatically.
 */
public class EventBus {

    private final List<LibraryCallback> callbacks = new CopyOnWriteArrayList<>();
    private final ExecutorService sender = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "event-sender");
        t.setDaemon(true);
        return t;
    });
    private final Connection conn;

    public EventBus(Database db) {
        this.conn = db.conn();
    }

    public void register(LibraryCallback cb) {
        callbacks.add(cb);
        ServerLog.info("Client callback registered (live clients: " + callbacks.size() + ")");
    }

    public void unregister(LibraryCallback cb) {
        callbacks.remove(cb);
        ServerLog.info("Client callback removed (live clients: " + callbacks.size() + ")");
    }

    public int count() {
        return callbacks.size();
    }

    /** Must be called while holding the database lock (from the synchronized service methods). */
    public void publish(LibraryEvent.Type type, String message, String bookId, String studentId) {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        long id = 0;
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO events (time,type,message,book_id,student_id) VALUES (?,?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, now.toString());
            ps.setString(2, type.name());
            ps.setString(3, message);
            ps.setString(4, bookId);
            ps.setString(5, studentId);
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) {
                if (k.next()) id = k.getLong(1);
            }
        } catch (SQLException e) {
            ServerLog.info("Could not save event: " + e.getMessage());
        }
        LibraryEvent event = new LibraryEvent(id, type, message, bookId, studentId, now);
        ServerLog.info("EVENT " + event);
        sender.submit(() -> deliver(event));
    }

    private void deliver(LibraryEvent event) {
        for (LibraryCallback cb : callbacks) {
            try {
                cb.onEvent(event);
            } catch (RemoteException e) {
                callbacks.remove(cb);
                ServerLog.info("Client unreachable, callback removed (live clients: " + callbacks.size() + ")");
            }
        }
    }

    public List<LibraryEvent> recent(int limit) throws SQLException {
        List<LibraryEvent> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id,time,type,message,book_id,student_id FROM events ORDER BY id DESC LIMIT ?")) {
            ps.setInt(1, Math.max(1, Math.min(limit, 500)));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new LibraryEvent(rs.getLong(1), LibraryEvent.Type.valueOf(rs.getString(3)),
                            rs.getString(4), rs.getString(5), rs.getString(6), LocalDateTime.parse(rs.getString(2))));
                }
            }
        }
        Collections.reverse(list);
        return list;
    }
}
