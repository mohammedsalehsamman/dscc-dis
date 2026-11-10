package dscc.ui;

import dscc.util.ServerLog;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

/**
 * Server-side monitor window: status, uptime, number of requests, connected clients
 * and a live log of every operation. Started by the servers unless "--nogui" is passed.
 */
public class ServerMonitor extends JFrame {

    private final JTextArea logArea = new JTextArea();
    private final StatCard uptimeCard = new StatCard("Uptime", Theme.ACCENT);
    private final StatCard requestsCard = new StatCard("Requests", Theme.SUCCESS);
    private final StatCard clientsCard = new StatCard("Live clients", Theme.PURPLE);
    private final Instant started = Instant.now();

    private ServerMonitor(String title, String[] endpoints, Supplier<Integer> liveClients) {
        super(title + " - Server Monitor");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBorder(new EmptyBorder(16, 18, 16, 18));

        JPanel head = new JPanel(new BorderLayout());
        head.add(UiKit.header(title, "Server is running - closing this window stops the server"), BorderLayout.CENTER);
        JPanel eps = new JPanel(new GridLayout(0, 1));
        for (String e : endpoints) {
            JLabel l = new JLabel(e);
            l.putClientProperty("FlatLaf.style", "font: -1");
            eps.add(l);
        }
        head.add(eps, BorderLayout.SOUTH);

        JPanel cards = new JPanel(new GridLayout(1, 3, 12, 0));
        cards.add(uptimeCard);
        cards.add(requestsCard);
        cards.add(clientsCard);
        if (liveClients == null) clientsCard.setValue("n/a");

        JPanel top = new JPanel(new BorderLayout(0, 12));
        top.add(head, BorderLayout.NORTH);
        top.add(cards, BorderLayout.CENTER);

        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setBorder(BorderFactory.createTitledBorder("Live activity"));

        root.add(top, BorderLayout.NORTH);
        root.add(scroll, BorderLayout.CENTER);
        setContentPane(root);
        setSize(760, 560);
        setLocationByPlatform(true);

        ServerLog.addListener(line -> SwingUtilities.invokeLater(() -> {
            logArea.append(line + "\n");
            if (logArea.getLineCount() > 2000) logArea.setText("");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        }));

        new Timer(1000, e -> {
            Duration d = Duration.between(started, Instant.now());
            uptimeCard.setValue(String.format("%02d:%02d:%02d", d.toHours(), d.toMinutesPart(), d.toSecondsPart()));
            requestsCard.setValue(String.valueOf(ServerLog.requestCount()));
            if (liveClients != null) clientsCard.setValue(String.valueOf(liveClients.get()));
        }).start();
    }

    /**
     * Opens the monitor window, unless running headless or "--nogui" is among the arguments.
     *
     * @param liveClients supplier of the number of live (callback) clients, or null
     */
    public static void start(String[] args, String title, String[] endpoints, Supplier<Integer> liveClients) {
        for (String a : args) if ("--nogui".equalsIgnoreCase(a)) return;
        if (GraphicsEnvironment.isHeadless()) return;
        SwingUtilities.invokeLater(() -> {
            Theme.init();
            new ServerMonitor(title, endpoints, liveClients).setVisible(true);
        });
    }
}
