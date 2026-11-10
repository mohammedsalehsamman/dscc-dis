package library.server;

import library.common.AccessDeniedException;
import library.common.StaffInfo;
import library.integration.StaffVerifier;

/** Stands in for the Employee SOAP system: a fixed set of tokens instead of real logins. */
class FakeStaffVerifier implements StaffVerifier {

    static final String LIBRARIAN = "librarian-token";
    static final String EMPLOYEE = "employee-token";

    @Override
    public StaffInfo login(String username, String password) throws AccessDeniedException {
        if ("librarian".equals(username) && "lib123".equals(password)) return verify(LIBRARIAN);
        throw new AccessDeniedException("Wrong username or password");
    }

    @Override
    public StaffInfo verify(String token) throws AccessDeniedException {
        if (LIBRARIAN.equals(token)) {
            return new StaffInfo(token, "librarian", "LIBRARIAN", 4, "Lina Mahmoud", "Library");
        }
        if (EMPLOYEE.equals(token)) {
            throw new AccessDeniedException("Access denied: role EMPLOYEE cannot manage the library");
        }
        throw new AccessDeniedException("Staff session is invalid or expired - please sign in again");
    }
}
