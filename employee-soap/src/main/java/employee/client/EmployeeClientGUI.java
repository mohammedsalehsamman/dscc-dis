package employee.client;

import employee.model.Employee;
import employee.model.LoginResult;
import employee.service.EmployeeService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/**
 * Swing SOAP client for the Employee Management web service.
 * It logs in through AuthService, reads the WSDL from the server and creates a proxy (port)
 * for EmployeeService; every button call is a real SOAP request over HTTP.
 * This is the simple client for the four operations required by the assignment;
 * the full application is {@link employee.client.ui.EmployeeClientApp}.
 */
public class EmployeeClientGUI extends JFrame {

    private final JTextField urlField = new JTextField("http://localhost:8080", 18);
    private final JTextField userField = new JTextField("admin", 8);
    private final JPasswordField passField = new JPasswordField("admin123", 8);
    private final JLabel statusLabel = new JLabel("Not connected");
    private final JTextField idField = new JTextField(10);
    private final JTextField nameField = new JTextField(20);
    private final JTextField salaryField = new JTextField(10);
    private final JTextArea logArea = new JTextArea(12, 50);

    private EmployeeService port;

    public EmployeeClientGUI() {
        super("Employee Management System - SOAP Client");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(new EmptyBorder(10, 10, 10, 10));
        root.add(buildConnectionPanel(), BorderLayout.NORTH);
        root.add(buildFormPanel(), BorderLayout.CENTER);
        root.add(buildLogPanel(), BorderLayout.SOUTH);
        setContentPane(root);

        pack();
        setLocationRelativeTo(null);
        connect();
    }

    // ---------------------------------------------------------------- UI

    private JPanel buildConnectionPanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
        p.setBorder(new TitledBorder("Server"));
        JButton connectBtn = new JButton("Connect");
        connectBtn.addActionListener(e -> connect());
        p.add(new JLabel("Server:"));
        p.add(urlField);
        p.add(new JLabel("User:"));
        p.add(userField);
        p.add(new JLabel("Password:"));
        p.add(passField);
        p.add(connectBtn);
        p.add(statusLabel);
        return p;
    }

    private JPanel buildFormPanel() {
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;

        addRow(form, c, 0, "Employee ID:", idField);
        addRow(form, c, 1, "Name:", nameField);
        addRow(form, c, 2, "Salary:", salaryField);

        JPanel buttons = new JPanel(new GridLayout(1, 5, 6, 0));
        JButton addBtn = new JButton("Add");
        JButton getBtn = new JButton("Get");
        JButton updateBtn = new JButton("Update");
        JButton deleteBtn = new JButton("Delete");
        JButton clearBtn = new JButton("Clear");
        buttons.add(addBtn);
        buttons.add(getBtn);
        buttons.add(updateBtn);
        buttons.add(deleteBtn);
        buttons.add(clearBtn);

        addBtn.addActionListener(e -> onAdd());
        getBtn.addActionListener(e -> onGet());
        updateBtn.addActionListener(e -> onUpdate());
        deleteBtn.addActionListener(e -> onDelete());
        clearBtn.addActionListener(e -> clearFields());

        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        form.add(buttons, c);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBorder(new TitledBorder("Employee"));
        wrapper.add(form, BorderLayout.WEST);
        return wrapper;
    }

    private void addRow(JPanel p, GridBagConstraints c, int row, String label, JComponent field) {
        c.gridx = 0;
        c.gridy = row;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        p.add(new JLabel(label), c);
        c.gridx = 1;
        p.add(field, c);
    }

    private JPanel buildLogPanel() {
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(new TitledBorder("Server responses"));
        p.add(new JScrollPane(logArea), BorderLayout.CENTER);
        return p;
    }

    // ---------------------------------------------------------------- actions

    private void connect() {
        String url = urlField.getText().trim();
        String user = userField.getText().trim();
        String pass = new String(passField.getPassword());
        statusLabel.setText("Connecting...");
        runAsync(() -> {
            // 1) login through AuthService  2) create the EmployeeService proxy (token goes in the SOAP header)
            SoapClient client = new SoapClient(url);
            LoginResult r = client.login(user, pass);
            if (!r.isSuccess()) throw new IllegalStateException(r.getMessage());
            log("Logged in as " + r.getFullName() + " (" + r.getRole() + ")");
            return client.employees();
        }, p -> {
            port = p;
            statusLabel.setText("Connected");
            statusLabel.setForeground(new Color(0, 128, 0));
            log("Connected to " + url + "/EmployeeService");
        }, "connect");
    }

    private void onAdd() {
        Employee e = readEmployee(true);
        if (e == null || !ensureConnected()) return;
        runAsync(() -> port.addEmployee(e), this::log, "addEmployee");
    }

    private void onGet() {
        Integer id = readId();
        if (id == null || !ensureConnected()) return;
        runAsync(() -> port.getEmployee(id), emp -> {
            if (emp == null) {
                log("Failure: employee with id " + id + " not found");
            } else {
                nameField.setText(emp.getName());
                salaryField.setText(String.valueOf(emp.getSalary()));
                log("Found: " + emp);
            }
        }, "getEmployee");
    }

    private void onUpdate() {
        Employee e = readEmployee(true);
        if (e == null || !ensureConnected()) return;
        runAsync(() -> port.updateEmployee(e), this::log, "updateEmployee");
    }

    private void onDelete() {
        Integer id = readId();
        if (id == null || !ensureConnected()) return;
        int answer = JOptionPane.showConfirmDialog(this,
                "Delete employee " + id + "?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (answer != JOptionPane.YES_OPTION) return;
        runAsync(() -> port.deleteEmployee(id), this::log, "deleteEmployee");
    }

    // ---------------------------------------------------------------- helpers

    private Integer readId() {
        try {
            return Integer.parseInt(idField.getText().trim());
        } catch (NumberFormatException ex) {
            error("Please enter a valid numeric Employee ID.");
            return null;
        }
    }

    private Employee readEmployee(boolean needAll) {
        Integer id = readId();
        if (id == null) return null;
        String name = nameField.getText().trim();
        if (needAll && name.isEmpty()) {
            error("Please enter the employee name.");
            return null;
        }
        double salary;
        try {
            salary = Double.parseDouble(salaryField.getText().trim());
        } catch (NumberFormatException ex) {
            error("Please enter a valid salary.");
            return null;
        }
        return new Employee(id, name, salary);
    }

    private boolean ensureConnected() {
        if (port == null) {
            error("Not connected to the server. Start EmployeePublisher, then press Connect.");
            return false;
        }
        return true;
    }

    private void clearFields() {
        idField.setText("");
        nameField.setText("");
        salaryField.setText("");
    }

    /** Runs a SOAP call off the Swing thread so the window does not freeze. */
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
                        port = null;
                        statusLabel.setText("Not connected");
                        statusLabel.setForeground(Color.RED);
                    }
                    log("ERROR in " + opName + ": " + cause.getMessage());
                    error("Error in " + opName + ":\n" + cause.getMessage());
                }
            }
        }.execute();
    }

    private void log(String msg) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> log(msg));
            return;
        }
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        logArea.append("[" + time + "] " + msg + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private void error(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new EmployeeClientGUI().setVisible(true));
    }
}
