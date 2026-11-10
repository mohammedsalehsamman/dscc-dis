package dscc.portal;

import dscc.ui.Theme;

import javax.swing.*;

/**
 * DSCC Integrated Portal - one client for both systems.
 * Start the two servers first (employee-soap, then library-rmi), then run this class.
 */
public class PortalApp {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Theme.init();
            new LoginFrame().setVisible(true);
        });
    }
}
