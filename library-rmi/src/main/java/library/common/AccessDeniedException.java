package library.common;

/**
 * Thrown by the server when the caller is not logged in or does not have the required role.
 */
public class AccessDeniedException extends Exception {

    private static final long serialVersionUID = 1L;

    public AccessDeniedException(String message) {
        super(message);
    }
}
