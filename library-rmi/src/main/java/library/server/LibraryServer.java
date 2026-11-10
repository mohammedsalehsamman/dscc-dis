package library.server;

import dscc.ui.ServerMonitor;
import dscc.util.AppConfig;
import dscc.util.ServerLog;
import library.common.LibraryService;
import library.integration.EmployeeVerifier;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Library RMI server: creates the remote object, binds it in the RMI registry
 * and waits for client requests. It also:
 * <ul>
 *   <li>connects to the Employee SOAP server to verify staff (soap.url in config.properties),</li>
 *   <li>checks for overdue loans every minute and pushes alerts to the clients.</li>
 * </ul>
 * Usage: LibraryServer [this-machine-IP] [--nogui]
 */
public class LibraryServer {

    // static references keep the registry and remote object from being garbage collected
    private static Registry registry;
    private static LibraryServiceImpl service;

    public static void main(String[] args) {
        try {
            // Pass this machine's IP when clients connect from other PCs, so the stub sent to
            // clients contains a reachable address.
            for (String a : args) {
                if (!a.startsWith("--")) System.setProperty("java.rmi.server.hostname", a);
            }

            Database db = new Database("data/library.db");
            String soapUrl = AppConfig.soapBaseUrl();
            service = new LibraryServiceImpl(db, new EmployeeVerifier(soapUrl));
            registry = LocateRegistry.createRegistry(LibraryService.PORT);
            registry.rebind(LibraryService.NAME, service);

            int every = AppConfig.getInt(AppConfig.OVERDUE_CHECK_SECONDS);
            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "overdue-checker");
                t.setDaemon(true);
                return t;
            });
            scheduler.scheduleAtFixedRate(service::checkOverdue, 5, every, TimeUnit.SECONDS);

            ServerLog.info("==============================================");
            ServerLog.info(" Library RMI Server is ready");
            ServerLog.info(" Registry : rmi://localhost:" + LibraryService.PORT + "/" + LibraryService.NAME);
            ServerLog.info(" Staff are verified by the SOAP server at " + soapUrl);
            ServerLog.info(" Overdue check every " + every + " s; demo students S1001..S1006 (PIN 1234)");
            ServerLog.info(" Waiting for client requests...");
            ServerLog.info("==============================================");

            ServerMonitor.start(args, "Library RMI Server", new String[]{
                    "Registry:  rmi://localhost:" + LibraryService.PORT + "/" + LibraryService.NAME,
                    "SOAP identity provider:  " + soapUrl}, () -> service.events().count());
            // The RMI runtime keeps non-daemon threads alive, so the server keeps running.
        } catch (Exception e) {
            System.err.println("Server error: " + e);
            e.printStackTrace();
            System.exit(1);
        }
    }
}
