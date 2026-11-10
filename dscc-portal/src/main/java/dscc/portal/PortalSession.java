package dscc.portal;

import employee.client.SoapClient;
import library.client.LibraryConnection;

/**
 * Who is signed in to the portal and the connections to both servers.
 * Employees sign in ONCE through SOAP; the same token is then accepted by the RMI server
 * (single sign-on). Students sign in through RMI only.
 */
public class PortalSession {

    public static final String STUDENT = "STUDENT";

    final SoapClient soap;          // null for students
    final LibraryConnection library; // null when the RMI server is offline
    final String role;               // ADMIN, LIBRARIAN, EMPLOYEE or STUDENT
    final String displayName;
    final String username;

    PortalSession(SoapClient soap, LibraryConnection library, String role, String displayName, String username) {
        this.soap = soap;
        this.library = library;
        this.role = role;
        this.displayName = displayName;
        this.username = username;
    }

    boolean isAdmin() {
        return "ADMIN".equals(role);
    }

    boolean isLibrarian() {
        return "LIBRARIAN".equals(role);
    }

    boolean isStudent() {
        return STUDENT.equals(role);
    }

    boolean libraryOnline() {
        return library != null;
    }

    void close() {
        if (library != null) library.close();
        if (soap != null) soap.logout();
    }
}
