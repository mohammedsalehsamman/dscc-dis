package library.integration;

import library.common.AccessDeniedException;
import library.common.StaffInfo;

/**
 * Decides who a staff token belongs to and whether that person may manage the library.
 * The real implementation, {@link EmployeeVerifier}, asks the Employee SOAP system;
 * tests use an in-memory fake.
 */
public interface StaffVerifier {

    /** Staff login with an employee account; returns the verified staff member. */
    StaffInfo login(String username, String password) throws AccessDeniedException;

    /** Returns the verified staff member (LIBRARIAN or ADMIN) or throws AccessDeniedException. */
    StaffInfo verify(String token) throws AccessDeniedException;
}
