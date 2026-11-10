package dscc.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Password (and student PIN) hashing with PBKDF2-HMAC-SHA256 and a random per-user salt.
 * Passwords are never stored as plain text.
 * <p>
 * Stored format: {@code pbkdf2-sha256$<iterations>$<hex hash>}. The iteration count is stored
 * with the hash, so it can be raised later without breaking existing accounts.
 * Hashes from older versions (a single SHA-256 of salt + password, no prefix) still verify;
 * callers re-hash them on the next successful login (see {@link #needsRehash(String)}).
 */
public final class Passwords {

    static final String ALGORITHM = "pbkdf2-sha256";
    static final int ITERATIONS = 210_000;
    private static final int KEY_BITS = 256;

    private static final SecureRandom RANDOM = new SecureRandom();

    private Passwords() {
    }

    public static String newSalt() {
        byte[] b = new byte[16];
        RANDOM.nextBytes(b);
        return toHex(b);
    }

    /** Hashes a password with the current algorithm and iteration count. */
    public static String hash(String password, String salt) {
        return ALGORITHM + "$" + ITERATIONS + "$" + pbkdf2(password, salt, ITERATIONS);
    }

    public static boolean verify(String password, String salt, String storedHash) {
        if (password == null || salt == null || storedHash == null) return false;
        String expected;
        String actual;
        if (storedHash.startsWith(ALGORITHM + "$")) {
            String[] parts = storedHash.split("\\$");
            if (parts.length != 3) return false;
            int iterations;
            try {
                iterations = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                return false;
            }
            if (iterations < 1) return false;
            expected = parts[2];
            actual = pbkdf2(password, salt, iterations);
        } else {
            expected = storedHash;
            actual = legacySha256(password, salt);
        }
        return MessageDigest.isEqual(actual.getBytes(StandardCharsets.UTF_8), expected.getBytes(StandardCharsets.UTF_8));
    }

    /** True when the stored hash uses an older algorithm or fewer iterations than today's default. */
    public static boolean needsRehash(String storedHash) {
        return storedHash == null || !storedHash.startsWith(ALGORITHM + "$" + ITERATIONS + "$");
    }

    private static String pbkdf2(String password, String salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt.getBytes(StandardCharsets.UTF_8), iterations, KEY_BITS);
        try {
            return toHex(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        } finally {
            spec.clearPassword();
        }
    }

    /** The format used before PBKDF2: SHA-256(salt + password). Only used to verify old hashes. */
    static String legacySha256(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes(StandardCharsets.UTF_8));
            return toHex(md.digest(password.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
