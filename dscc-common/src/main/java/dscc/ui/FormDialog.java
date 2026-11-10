package dscc.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Reusable form dialog with live validation: invalid fields get a red outline and a message
 * under them while the user types. {@link #showDialog()} returns the values, or null if cancelled.
 *
 * <pre>
 * Map&lt;String,String&gt; v = new FormDialog(parent, "Add employee")
 *     .text("id", "Employee ID", "", Validators.positiveInt())
 *     .text("name", "Name", "", Validators.required())
 *     .showDialog();
 * </pre>
 */
public class FormDialog {

    private final Component parent;
    private final String title;
    private final Map<String, JComponent> fields = new LinkedHashMap<>();
    private final Map<String, String> labels = new LinkedHashMap<>();
    private final Map<String, Function<String, String>> validators = new LinkedHashMap<>();
    private final Map<String, JLabel> errors = new LinkedHashMap<>();
    private String okText = "Save";

    public FormDialog(Component parent, String title) {
        this.parent = parent;
        this.title = title;
    }

    public FormDialog text(String key, String label, String value, Function<String, String> validator) {
        JTextField f = new JTextField(value == null ? "" : value, 22);
        return add(key, label, f, validator);
    }

    public FormDialog readOnly(String key, String label, String value) {
        JTextField f = new JTextField(value == null ? "" : value, 22);
        f.setEditable(false);
        return add(key, label, f, null);
    }

    public FormDialog password(String key, String label, Function<String, String> validator) {
        JPasswordField f = new JPasswordField(22);
        f.putClientProperty("JTextField.showRevealButton", true);
        return add(key, label, f, validator);
    }

    public FormDialog combo(String key, String label, String[] options, String selected, boolean editable) {
        JComboBox<String> c = new JComboBox<>(options);
        c.setEditable(editable);
        if (selected != null) c.setSelectedItem(selected);
        return add(key, label, c, editable ? Validators.required() : null);
    }

    public FormDialog okText(String text) {
        this.okText = text;
        return this;
    }

    private FormDialog add(String key, String label, JComponent comp, Function<String, String> validator) {
        fields.put(key, comp);
        labels.put(key, label);
        if (validator != null) validators.put(key, validator);
        JLabel err = new JLabel(" ");
        err.setForeground(Theme.DANGER);
        err.putClientProperty("FlatLaf.style", "font: -2");
        errors.put(key, err);
        if (comp instanceof JTextComponent) {
            ((JTextComponent) comp).getDocument().addDocumentListener(new DocumentListener() {
                public void insertUpdate(DocumentEvent e) {
                    validate(key);
                }

                public void removeUpdate(DocumentEvent e) {
                    validate(key);
                }

                public void changedUpdate(DocumentEvent e) {
                    validate(key);
                }
            });
        }
        return this;
    }

    private String valueOf(JComponent c) {
        if (c instanceof JPasswordField) return new String(((JPasswordField) c).getPassword());
        if (c instanceof JTextComponent) return ((JTextComponent) c).getText().trim();
        if (c instanceof JComboBox) {
            Object o = ((JComboBox<?>) c).isEditable()
                    ? ((JComboBox<?>) c).getEditor().getItem() : ((JComboBox<?>) c).getSelectedItem();
            return o == null ? "" : o.toString().trim();
        }
        return "";
    }

    private boolean validate(String key) {
        Function<String, String> v = validators.get(key);
        JComponent comp = fields.get(key);
        String msg = v == null ? null : v.apply(valueOf(comp));
        comp.putClientProperty("JComponent.outline", msg == null ? null : "error");
        errors.get(key).setText(msg == null ? " " : msg);
        return msg == null;
    }

    public Map<String, String> showDialog() {
        Window owner = parent instanceof Window ? (Window) parent : SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog(owner, title, Dialog.ModalityType.APPLICATION_MODAL);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(16, 18, 6, 18));
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(2, 4, 0, 4);
        int row = 0;
        for (String key : fields.keySet()) {
            c.gridx = 0;
            c.gridy = row;
            c.fill = GridBagConstraints.NONE;
            c.weightx = 0;
            form.add(new JLabel(labels.get(key)), c);
            c.gridx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 1;
            form.add(fields.get(key), c);
            c.gridy = row + 1;
            form.add(errors.get(key), c);
            row += 2;
        }

        final java.util.concurrent.atomic.AtomicReference<Map<String, String>> result = new java.util.concurrent.atomic.AtomicReference<>();
        JButton ok = new JButton(okText);
        ok.putClientProperty("JButton.buttonType", "default");
        JButton cancel = new JButton("Cancel");
        ok.addActionListener(e -> {
            boolean allValid = true;
            for (String key : fields.keySet()) allValid &= validate(key);
            if (!allValid) return;
            Map<String, String> values = new LinkedHashMap<>();
            for (Map.Entry<String, JComponent> f : fields.entrySet()) values.put(f.getKey(), valueOf(f.getValue()));
            result.set(values);
            dialog.dispose();
        });
        cancel.addActionListener(e -> dialog.dispose());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.setBorder(new EmptyBorder(0, 12, 10, 12));
        buttons.add(cancel);
        buttons.add(ok);

        JLabel header = new JLabel(title);
        header.putClientProperty("FlatLaf.style", "font: +4 bold");
        header.setBorder(new EmptyBorder(14, 22, 0, 18));

        dialog.getContentPane().add(header, BorderLayout.NORTH);
        dialog.getContentPane().add(form, BorderLayout.CENTER);
        dialog.getContentPane().add(buttons, BorderLayout.SOUTH);
        dialog.getRootPane().setDefaultButton(ok);
        dialog.getRootPane().registerKeyboardAction(e -> dialog.dispose(),
                KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);
        dialog.pack();
        dialog.setMinimumSize(new Dimension(420, dialog.getHeight()));
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
        return result.get();
    }

    /** Common validators: each returns an error message, or null when the value is valid. */
    public static final class Validators {
        private Validators() {
        }

        public static Function<String, String> required() {
            return s -> s.isEmpty() ? "This field is required" : null;
        }

        public static Function<String, String> positiveInt() {
            return s -> {
                try {
                    return Integer.parseInt(s) > 0 ? null : "Must be a positive number";
                } catch (NumberFormatException e) {
                    return "Enter a whole number";
                }
            };
        }

        public static Function<String, String> intRange(int min, int max) {
            return s -> {
                try {
                    int v = Integer.parseInt(s);
                    return v >= min && v <= max ? null : "Must be between " + min + " and " + max;
                } catch (NumberFormatException e) {
                    return "Enter a whole number";
                }
            };
        }

        public static Function<String, String> nonNegativeNumber() {
            return s -> {
                try {
                    return Double.parseDouble(s) >= 0 ? null : "Cannot be negative";
                } catch (NumberFormatException e) {
                    return "Enter a number";
                }
            };
        }

        public static Function<String, String> optionalEmail() {
            return s -> s.isEmpty() || s.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+") ? null : "Invalid email address";
        }

        public static Function<String, String> optionalDate() {
            return s -> s.isEmpty() || s.matches("\\d{4}-\\d{2}-\\d{2}") ? null : "Use the format yyyy-MM-dd";
        }

        public static Function<String, String> minLength(int n) {
            return s -> s.length() >= n ? null : "At least " + n + " characters";
        }
    }
}
