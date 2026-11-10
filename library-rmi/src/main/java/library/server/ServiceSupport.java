package library.server;

import dscc.util.ServerLog;

import java.rmi.RemoteException;
import java.sql.SQLException;

/**
 * Small helpers shared by the library services: the database lock, result logging and
 * the conversion of SQL errors into "Failure: ..." results or RemoteExceptions.
 */
final class ServiceSupport {

    /** A piece of database work. */
    @FunctionalInterface
    interface SqlWork<T> {
        T run() throws SQLException;
    }

    private ServiceSupport() {
    }

    /**
     * Runs a query under the database lock. RMI serves each client on its own thread and the
     * SQLite connection is shared, so all database work goes through this lock.
     * SQL errors reach the client as a RemoteException.
     */
    static <T> T read(Database db, SqlWork<T> work) throws RemoteException {
        synchronized (db) {
            try {
                return work.run();
            } catch (SQLException e) {
                ServerLog.info("Database error: " + e.getMessage());
                throw new RemoteException("Database error: " + e.getMessage());
            }
        }
    }

    /** Runs a change under the database lock; SQL errors become a "Failure: ..." result. */
    static String write(Database db, SqlWork<String> work) {
        synchronized (db) {
            try {
                return work.run();
            } catch (SQLException e) {
                return log("database", "Failure: database error - " + e.getMessage());
            }
        }
    }

    /** Logs the result of an operation on the server console and returns it. */
    static String log(String operation, String result) {
        ServerLog.info(operation + " -> " + result.replace("\n", " || "));
        return result;
    }

    static boolean blank(String s) {
        return s == null || s.trim().isEmpty();
    }

    static String money(double v) {
        return String.format("%,.0f", v);
    }
}
