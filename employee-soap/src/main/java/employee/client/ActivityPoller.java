package employee.client;

import dscc.ui.Async;
import employee.model.ActivityEntry;

import javax.swing.Timer;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

/**
 * Near-real-time updates for SOAP. SOAP is request/response only (the server cannot call the
 * client), so the client asks every 3 seconds: "anything newer than the last entry I saw?"
 * (getActivitySince(lastId)). Compare with the RMI callbacks, where the server pushes events.
 * Listeners get (new entries, initialLoad) on the Swing thread.
 */
public class ActivityPoller {

    private final SoapClient client;
    private final List<BiConsumer<List<ActivityEntry>, Boolean>> listeners = new CopyOnWriteArrayList<>();
    private final Timer timer;
    private long lastId = -1;
    private boolean busy;

    public ActivityPoller(SoapClient client, int intervalMillis) {
        this.client = client;
        this.timer = new Timer(intervalMillis, e -> poll());
    }

    public void addListener(BiConsumer<List<ActivityEntry>, Boolean> l) {
        listeners.add(l);
    }

    public void start() {
        poll();
        timer.start();
    }

    public void stop() {
        timer.stop();
    }

    public void poll() {
        if (busy || !client.isAdmin()) return;
        busy = true;
        final boolean initial = lastId < 0;
        Async.run(null, () -> client.employees().getActivitySince(lastId), entries -> {
            busy = false;
            if (entries == null || entries.isEmpty()) {
                if (initial) lastId = 0;
                return;
            }
            lastId = entries.get(entries.size() - 1).getId();
            for (BiConsumer<List<ActivityEntry>, Boolean> l : listeners) l.accept(entries, initial);
        }, err -> busy = false);
    }
}
