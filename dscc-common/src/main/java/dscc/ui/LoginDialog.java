package dscc.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.function.BiFunction;

/**
 * Generic modal login dialog. The authenticate function receives (user, password),
 * runs in a background thread and returns null on success or an error message.
 */
public class LoginDialog extends JDialog {

    private final JTextField userField = new JTextField(20);
    private final JPasswordField passField = new JPasswordField(20);
    private final JLabel errorLabel = new JLabel(" ");
    private boolean success;

    public LoginDialog(Window owner, String title, String subtitle, String userLabel, String passLabel,
                       String userHint, BiFunction<String, String, String> authenticate) {
        super(owner, title, ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(new EmptyBorder(22, 28, 18, 28));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(3, 0, 3, 0);

        JLabel t = new JLabel(title);
        t.putClientProperty("FlatLaf.style", "font: +8 bold");
        JLabel s = new JLabel(subtitle);
        s.putClientProperty("FlatLaf.style", "foreground: $Label.disabledForeground");
        userField.putClientProperty("JTextField.placeholderText", userHint);
        passField.putClientProperty("JTextField.showRevealButton", true);
        errorLabel.setForeground(Theme.DANGER);

        JButton login = new JButton("Sign in");
        login.putClientProperty("FlatLaf.style", "background: #2F6FEB; foreground: #FFFFFF; borderWidth: 0; font: bold");

        int y = 0;
        c.gridy = y++;
        p.add(t, c);
        c.gridy = y++;
        p.add(s, c);
        c.gridy = y++;
        p.add(Box.createVerticalStrut(10), c);
        c.gridy = y++;
        p.add(new JLabel(userLabel), c);
        c.gridy = y++;
        p.add(userField, c);
        c.gridy = y++;
        p.add(new JLabel(passLabel), c);
        c.gridy = y++;
        p.add(passField, c);
        c.gridy = y++;
        p.add(errorLabel, c);
        c.gridy = y;
        p.add(login, c);

        login.addActionListener(e -> {
            String u = userField.getText().trim(), pw = new String(passField.getPassword());
            if (u.isEmpty() || pw.isEmpty()) {
                errorLabel.setText("Please fill in both fields");
                return;
            }
            login.setEnabled(false);
            errorLabel.setText("Signing in...");
            Async.run(this, () -> authenticate.apply(u, pw), err -> {
                login.setEnabled(true);
                if (err == null) {
                    success = true;
                    dispose();
                } else {
                    errorLabel.setText(err);
                    passField.selectAll();
                    passField.requestFocusInWindow();
                }
            }, ex -> {
                login.setEnabled(true);
                errorLabel.setText(Async.message(ex));
            });
        });
        getRootPane().setDefaultButton(login);
        setContentPane(p);
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    /** Shows the dialog and returns true if the user signed in successfully. */
    public boolean showDialog() {
        setVisible(true);
        return success;
    }

    public String getUser() {
        return userField.getText().trim();
    }
}
