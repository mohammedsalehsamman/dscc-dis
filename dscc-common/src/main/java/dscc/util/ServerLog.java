package dscc.util;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Central server log: prints to the console and forwards lines to any listener
 * (the Server Monitor window). Also counts handled requests.
 */
public final class ServerLog {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final List<Consumer<String>> LISTENERS = new CopyOnWriteArrayList<>();
    private static final AtomicLong REQUESTS = new AtomicLong();

    private ServerLog() {
    }

    public static void info(String message) {
        String line = "[" + LocalTime.now().format(TIME) + "] " + message;
        System.out.println(line);
        for (Consumer<String> l : LISTENERS) {
            try {
                l.accept(line);
            } catch (RuntimeException ignored) {
                // a broken listener must never break the server
            }
        }
    }

    public static void request() {
        REQUESTS.incrementAndGet();
    }

    public static long requestCount() {
        return REQUESTS.get();
    }

    public static void addListener(Consumer<String> listener) {
        LISTENERS.add(listener);
    }
}
