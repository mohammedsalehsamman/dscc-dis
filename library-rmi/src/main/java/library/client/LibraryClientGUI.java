package library.client;

import library.common.LibraryService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/**
 * Swing RMI client for the Library Management System.
 * It looks up the remote LibraryService in the server's RMI registry and
 * calls its methods remotely. This is the simple client for the operations required by the
 * assignment (add, search, view all) plus the issued-book history; the full application is
 * {@link library.client.ui.LibraryClientApp}.
 */
public class LibraryClientGUI extends JFrame {

    private final JTextField hostField = new JTextField("localhost", 14);
    private final JLabel statusLabel = new JLabel("Not connected");

    private final JTextField addIdField = new JTextField(10);
    private final JTextField addTitleField = new JTextField(22);
    private final JTextField addAuthorField = new JTextField(22);

    private final JTextField searchField = new JTextField(22);

    private final DefaultListModel<String> listModel = new DefaultListModel<>();
    private final JLabel listTitle = new JLabel("Books");
    private final JTextArea outputArea = new JTextArea(8, 60);

    private LibraryService library;

    public LibraryClientGUI() {
        super("Library Management System - RMI Client");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(new EmptyBorder(10, 10, 10, 10));
        root.add(buildConnectionPanel(), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Add Book", buildAddPanel());
        tabs.addTab("Search Book", buildSearchPanel());
        tabs.addTab("Issued History", buildHistoryPanel());

        JPanel center = new JPanel(new BorderLayout(8, 8));
        center.add(tabs, BorderLayout.NORTH);
        center.add(buildListPanel(), BorderLayout.CENTER);
        root.add(center, BorderLayout.CENTER);
        root.add(buildOutputPanel(), BorderLayout.SOUTH);
        setContentPane(root);

        setSize(820, 640);
        setLocationRelativeTo(null);
        connect();
    }

    // ---------------------------------------------------------------- UI

    private JPanel buildConnectionPanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
        p.setBorder(new TitledBorder("RMI Server"));
        JButton connectBtn = new JButton("Connect");
        connectBtn.addActionListener(e -> connect());
        p.add(new JLabel("Host:"));
        p.add(hostField);
        p.add(new JLabel("Port: " + LibraryService.PORT));
        p.add(connectBtn);
        p.add(statusLabel);
        return p;
    }

    private JPanel buildAddPanel() {
        JPanel p = formPanel();
        GridBagConstraints c = gbc();
        row(p, c, 0, "Book ID:", addIdField);
        row(p, c, 1, "Title:", addTitleField);
        row(p, c, 2, "Author:", addAuthorField);
        JButton addBtn = new JButton("Add Book");
        addBtn.addActionListener(e -> onAddBook());
        c.gridx = 1;
        c.gridy = 3;
        p.add(addBtn, c);
        return p;
    }

    private JPanel buildSearchPanel() {
        JPanel p = formPanel();
        GridBagConstraints c = gbc();
        row(p, c, 0, "Title:", searchField);
        JButton searchBtn = new JButton("Search");
        searchBtn.addActionListener(e -> onSearch());
        searchField.addActionListener(e -> onSearch());
        c.gridx = 1;
        c.gridy = 1;
        p.add(searchBtn, c);
        return p;
    }

    private JPanel buildHistoryPanel() {
        JPanel p = formPanel();
        GridBagConstraints c = gbc();
        JButton historyBtn = new JButton("View Issued History");
        historyBtn.addActionListener(e -> onHistory());
        c.gridx = 0;
        c.gridy = 0;
        p.add(historyBtn, c);
        c.gridy = 1;
        p.add(new JLabel("Issuing and returning books needs a staff login - use LibraryClientApp or the DSCC Portal."), c);
        return p;
    }

    private JPanel buildListPanel() {
        JPanel p = new JPanel(new BorderLayout(4, 4));
        p.setBorder(new TitledBorder("Library"));
        JButton viewAllBtn = new JButton("View All Books");
        viewAllBtn.addActionListener(e -> onViewAll());
        JPanel top = new JPanel(new BorderLayout());
        top.add(listTitle, BorderLayout.WEST);
        top.add(viewAllBtn, BorderLayout.EAST);
        JList<String> list = new JList<>(listModel);
        list.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        p.add(top, BorderLayout.NORTH);
        p.add(new JScrollPane(list), BorderLayout.CENTER);
        return p;
    }

    private JPanel buildOutputPanel() {
        outputArea.setEditable(false);
        outputArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(new TitledBorder("Server responses"));
        p.add(new JScrollPane(outputArea), BorderLayout.CENTER);
        return p;
    }

    private static JPanel formPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(new EmptyBorder(8, 8, 8, 8));
        return p;
    }

    private static GridBagConstraints gbc() {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;
        return c;
    }

    private static void row(JPanel p, GridBagConstraints c, int y, String label, JComponent field) {
        c.gridx = 0;
        c.gridy = y;
        p.add(new JLabel(label), c);
        c.gridx = 1;
        p.add(field, c);
    }

    // ---------------------------------------------------------------- actions

    private void connect() {
        String host = hostField.getText().trim();
        statusLabel.setText("Connecting...");
        runAsync(() -> {
            Registry registry = LocateRegistry.getRegistry(host, LibraryService.PORT);
            LibraryService service = (LibraryService) registry.lookup(LibraryService.NAME);
            service.viewAllBooks(); // test call to make sure the server answers
            return service;
        }, s -> {
            library = s;
            statusLabel.setText("Connected");
            statusLabel.setForeground(new Color(0, 128, 0));
            log("Connected to rmi://" + host + ":" + LibraryService.PORT + "/" + LibraryService.NAME);
            onViewAll();
        }, "connect");
    }

    private void onAddBook() {
        if (!ensureConnected()) return;
        String id = addIdField.getText(), title = addTitleField.getText(), author = addAuthorField.getText();
        runAsync(() -> library.addBook(id, title, author), msg -> {
            log(msg);
            if (msg.startsWith("Success")) {
                addIdField.setText("");
                addTitleField.setText("");
                addAuthorField.setText("");
                onViewAll();
            }
        }, "addBook");
    }

    private void onSearch() {
        if (!ensureConnected()) return;
        String title = searchField.getText();
        runAsync(() -> library.searchBook(title), result -> {
            log("Search '" + title.trim() + "':\n" + result);
            listTitle.setText("Search results for '" + title.trim() + "'");
            listModel.clear();
            for (String line : result.split("\n")) listModel.addElement(line);
        }, "searchBook");
    }

    private void onViewAll() {
        if (!ensureConnected()) return;
        runAsync(() -> library.viewAllBooks(), books -> showList("All books (" + books.size() + ")", books),
                "viewAllBooks");
    }

    private void onHistory() {
        if (!ensureConnected()) return;
        runAsync(() -> library.viewIssuedHistory(), records -> {
            showList("Issued book history (" + records.size() + ")", records);
            log("Loaded issued history: " + records.size() + " record(s)");
        }, "viewIssuedHistory");
    }

    // ---------------------------------------------------------------- helpers

    private void showList(String title, List<String> items) {
        listTitle.setText(title);
        listModel.clear();
        if (items.isEmpty()) listModel.addElement("(empty)");
        for (String s : items) listModel.addElement(s);
    }

    private boolean ensureConnected() {
        if (library == null) {
            error("Not connected. Start LibraryServer, then press Connect.");
            return false;
        }
        return true;
    }

    /** Runs a remote call off the Swing thread so the window does not freeze. */
    private <T> void runAsync(Callable<T> call, Consumer<T> onSuccess, String opName) {
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return call.call();
            }

            @Override
            protected void done() {
                try {
                    onSuccess.accept(get());
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    if ("connect".equals(opName)) {
                        library = null;
                        statusLabel.setText("Not connected");
                        statusLabel.setForeground(Color.RED);
                    }
                    log("ERROR in " + opName + ": " + cause);
                    error("Error in " + opName + ":\n" + cause.getMessage());
                }
            }
        }.execute();
    }

    private void log(String msg) {
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        outputArea.append("[" + time + "] " + msg + "\n");
        outputArea.setCaretPosition(outputArea.getDocument().getLength());
    }

    private void error(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LibraryClientGUI().setVisible(true));
    }
}
