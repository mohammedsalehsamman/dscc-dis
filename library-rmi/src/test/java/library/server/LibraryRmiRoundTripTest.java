package library.server;

import library.common.LibraryCallback;
import library.common.LibraryEvent;
import library.common.LibraryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.ServerSocket;
import java.nio.file.Path;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A real RMI round trip: registry on a free port, lookup by name, remote calls through the stub,
 * and a callback object exported by the "client" that the server calls back over RMI.
 */
class LibraryRmiRoundTripTest {

    @TempDir
    Path dir;

    @Test
    void clientCallsTheServerAndTheServerCallsTheClientBack() throws Exception {
        int port;
        try (ServerSocket s = new ServerSocket(0)) {
            port = s.getLocalPort();
        }
        try (Database db = new Database(dir.resolve("library.db").toString())) {
            LibraryServiceImpl impl = new LibraryServiceImpl(db, new FakeStaffVerifier());
            Registry serverRegistry = LocateRegistry.createRegistry(port);
            BlockingQueue<LibraryEvent> inbox = new LinkedBlockingQueue<>();
            LibraryCallback callback = inbox::add; // the client-side object the server will call
            boolean exported = false;
            try {
                serverRegistry.rebind(LibraryService.NAME, impl);

                // ---- client side
                LibraryService remote = (LibraryService) LocateRegistry.getRegistry("localhost", port)
                        .lookup(LibraryService.NAME);
                assertEquals("OK", remote.ping());
                assertEquals(14, remote.viewAllBooks().size());

                LibraryCallback stub = (LibraryCallback) UnicastRemoteObject.exportObject(callback, 0);
                exported = true;
                remote.registerCallback(stub);

                assertTrue(remote.addBook("B200", "Remote Book", "RMI Author").startsWith("Success"));

                LibraryEvent event = inbox.poll(5, TimeUnit.SECONDS);
                assertNotNull(event, "the server pushed an event to the client");
                assertEquals(LibraryEvent.Type.BOOK_ADDED, event.getType());
                assertEquals("B200", event.getBookId());
            } finally {
                if (exported) UnicastRemoteObject.unexportObject(callback, true);
                UnicastRemoteObject.unexportObject(impl, true);
                UnicastRemoteObject.unexportObject(serverRegistry, true);
            }
        }
    }
}
