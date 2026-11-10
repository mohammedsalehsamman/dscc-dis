package dscc.ui;

/**
 * A screen that can reload its data from the server (called when shown and on F5).
 */
public interface Refreshable {

    void refresh();
}
