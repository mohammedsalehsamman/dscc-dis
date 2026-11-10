package library.common;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Callback interface implemented by the CLIENT and called by the SERVER (reverse direction).
 * The client exports an object implementing this interface and registers it with
 * LibraryService.registerCallback; the server then pushes events to it in real time.
 */
public interface LibraryCallback extends Remote {

    void onEvent(LibraryEvent event) throws RemoteException;
}
