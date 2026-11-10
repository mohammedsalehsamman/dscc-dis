package dscc.portal;

import dscc.ui.Theme;
import dscc.ui.UiKit;
import dscc.util.AppConfig;
import employee.client.SoapClient;
import library.common.LibraryService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.rmi.registry.LocateRegistry;

/**
 * Server addresses (saved per user). Lets the portal run on a different machine than the servers.
 */
class SettingsDialog extends JDialog {

    private final JTextField soapUrl = new JTextField(AppConfig.get(AppConfig.SOAP_URL), 24);
    private final JTextField rmiHost = new JTextField(AppConfig.get(AppConfig.RMI_HOST), 24);
    private final JTextField rmiPort = new JTextField(AppConfig.get(AppConfig.RMI_PORT), 6);
    private final JLabel result = new JLabel(" ");

    SettingsDialog(Window owner) {
        super(owner, "Server settings", ModalityType.APPLICATION_MODAL);
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(new EmptyBorder(16, 20, 12, 20));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 4, 5, 4);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        Object[][] rows = {{"SOAP server URL", soapUrl}, {"RMI server host", rmiHost}, {"RMI registry port", rmiPort}};
        for (int i = 0; i < rows.length; i++) {
            c.gridx = 0;
            c.gridy = i;
            p.add(new JLabel((String) rows[i][0]), c);
            c.gridx = 1;
            p.add((Component) rows[i][1], c);
        }
        c.gridx = 0;
        c.gridy = rows.length;
        c.gridwidth = 2;
        p.add(result, c);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(UiKit.button("Test connection", e -> test()));
        buttons.add(UiKit.button("Cancel", e -> dispose()));
        JButton save = UiKit.primary("Save", e -> {
            AppConfig.save(AppConfig.SOAP_URL, soapUrl.getText());
            AppConfig.save(AppConfig.RMI_HOST, rmiHost.getText());
            AppConfig.save(AppConfig.RMI_PORT, rmiPort.getText());
            dispose();
        });
        buttons.add(save);
        getContentPane().add(UiKit.header("  Server settings", "  Where are the SOAP and RMI servers running?"), BorderLayout.NORTH);
        getContentPane().add(p, BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(owner);
    }

    private void test() {
        result.setForeground(UIManager.getColor("Label.foreground"));
        result.setText("Testing...");
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                boolean soap = new SoapClient(soapUrl.getText()).ping();
                boolean rmi;
                try {
                    LibraryService s = (LibraryService) LocateRegistry.getRegistry(rmiHost.getText().trim(),
                            Integer.parseInt(rmiPort.getText().trim())).lookup(LibraryService.NAME);
                    rmi = "OK".equals(s.ping());
                } catch (Exception e) {
                    rmi = false;
                }
                return (soap ? "SOAP: OK" : "SOAP: NOT reachable") + "    " + (rmi ? "RMI: OK" : "RMI: NOT reachable");
            }

            @Override
            protected void done() {
                try {
                    String r = get();
                    result.setText(r);
                    result.setForeground(r.contains("NOT") ? Theme.DANGER : Theme.SUCCESS);
                } catch (Exception ignored) {
                    // ignore
                }
            }
        }.execute();
    }
}
