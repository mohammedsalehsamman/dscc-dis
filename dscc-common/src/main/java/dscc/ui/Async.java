package dscc.ui;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/**
 * Runs a remote call (SOAP or RMI) in a background thread so the window never freezes,
 * then delivers the result on the Swing thread. Shows a wait cursor meanwhile.
 */
public final class Async {

    private Async() {
    }

    public static <T> void run(Component owner, Callable<T> call, Consumer<T> onSuccess) {
        run(owner, call, onSuccess, err -> Toast.error(owner, message(err)));
    }

    public static <T> void run(Component owner, Callable<T> call, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        Window w = owner == null ? null : SwingUtilities.getWindowAncestor(owner);
        if (w != null) w.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return call.call();
            }

            @Override
            protected void done() {
                if (w != null) w.setCursor(Cursor.getDefaultCursor());
                try {
                    onSuccess.accept(get());
                } catch (Exception ex) {
                    Throwable cause = ex instanceof java.util.concurrent.ExecutionException && ex.getCause() != null
                            ? ex.getCause() : ex;
                    if (onError != null) onError.accept(cause);
                }
            }
        }.execute();
    }

    /** Human-readable message from a remote exception (unwraps RMI / SOAP wrappers). */
    public static String message(Throwable t) {
        Throwable root = t;
        while (root.getCause() != null && root.getCause() != root
                && (root instanceof java.rmi.RemoteException || root.getMessage() == null)) {
            root = root.getCause();
        }
        if (root instanceof java.rmi.ConnectException || root instanceof java.net.ConnectException) {
            return "Server is not reachable. Make sure the server is running.";
        }
        String msg = root.getMessage();
        if (msg == null) return root.getClass().getSimpleName();
        // JAX-WS wraps SOAP faults in a long sentence; keep only the server's own text
        msg = msg.replace("Client received SOAP Fault from server: ", "");
        int i = msg.indexOf(" Please see the server log");
        if (i > 0) msg = msg.substring(0, i);
        if (msg.contains("Connection refused")) return "Server is not reachable. Make sure the server is running.";
        return msg;
    }
}
