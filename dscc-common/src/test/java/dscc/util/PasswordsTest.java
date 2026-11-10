package dscc.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordsTest {

    @Test
    void hashIsVerifiedWithTheSamePasswordOnly() {
        String salt = Passwords.newSalt();
        String hash = Passwords.hash("admin123", salt);

        assertTrue(Passwords.verify("admin123", salt, hash));
        assertFalse(Passwords.verify("admin124", salt, hash));
        assertFalse(Passwords.verify("admin123", Passwords.newSalt(), hash));
    }

    @Test
    void hashUsesPbkdf2FormatAndNeverContainsThePassword() {
        String hash = Passwords.hash("secret-password", Passwords.newSalt());

        assertTrue(hash.startsWith("pbkdf2-sha256$" + Passwords.ITERATIONS + "$"), hash);
        assertFalse(hash.contains("secret-password"));
        assertFalse(Passwords.needsRehash(hash));
    }

    @Test
    void sameDataGivesTheSameHashButADifferentSaltDoesNot() {
        String salt = Passwords.newSalt();
        assertEquals(Passwords.hash("pw", salt), Passwords.hash("pw", salt));
        assertNotEquals(Passwords.hash("pw", salt), Passwords.hash("pw", Passwords.newSalt()));
    }

    @Test
    void saltsAreRandomHex() {
        String a = Passwords.newSalt();
        assertTrue(a.matches("[0-9a-f]{32}"), a);
        assertNotEquals(a, Passwords.newSalt());
    }

    @Test
    void legacySha256HashesStillVerifyAndAreFlaggedForRehash() {
        String salt = Passwords.newSalt();
        String legacy = Passwords.legacySha256("lib123", salt);

        assertTrue(Passwords.verify("lib123", salt, legacy));
        assertFalse(Passwords.verify("wrong", salt, legacy));
        assertTrue(Passwords.needsRehash(legacy));
    }

    @Test
    void lowerIterationCountIsFlaggedForRehash() {
        assertTrue(Passwords.needsRehash("pbkdf2-sha256$1000$abcd"));
        assertTrue(Passwords.needsRehash(null));
    }

    @Test
    void malformedOrMissingInputIsRejected() {
        String salt = Passwords.newSalt();
        assertFalse(Passwords.verify(null, salt, Passwords.hash("x", salt)));
        assertFalse(Passwords.verify("x", null, "anything"));
        assertFalse(Passwords.verify("x", salt, null));
        assertFalse(Passwords.verify("x", salt, "pbkdf2-sha256$notanumber$abcd"));
        assertFalse(Passwords.verify("x", salt, "pbkdf2-sha256$0$abcd"));
        assertFalse(Passwords.verify("x", salt, "pbkdf2-sha256$abcd"));
    }
}
