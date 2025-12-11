package org.linlinjava.litemall.core.util.bcrypt;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.security.SecureRandom;
import org.linlinjava.litemall.core.util.bcrypt.BCrypt;

@DisplayName("BCrypt Utility Tests")
class BCryptTest {

    @Test
    @DisplayName("Should throw IllegalArgumentException when salt is null")
    void testHashpwSaltIsNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            BCrypt.hashpw("foo", null);
        });
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when salt is too short")
    void testHashpwSaltTooShort() {
        assertThrows(IllegalArgumentException.class, () -> {
            BCrypt.hashpw("foo", "foo");
        });
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for invalid salt version")
    void testHashpwInvalidSaltVersion() {
        assertThrows(IllegalArgumentException.class, () -> {
            BCrypt.hashpw("foo", "+2a$10$.....................");
        });
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for invalid salt version 2")
    void testHashpwInvalidSaltVersion2() {
        assertThrows(IllegalArgumentException.class, () -> {
            BCrypt.hashpw("foo", "$1a$10$.....................");
        });
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for invalid salt revision")
    void testHashpwInvalidSaltRevision() {
        assertThrows(IllegalArgumentException.class, () -> {
            BCrypt.hashpw("foo", "$2+$10$.....................");
        });
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for invalid salt revision 2")
    void testHashpwInvalidSaltRevision2() {
        assertThrows(IllegalArgumentException.class, () -> {
            BCrypt.hashpw("foo", "$2a+10$.....................");
        });
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when salt is too short 2")
    void testHashpwSaltTooShort2() {
        assertThrows(IllegalArgumentException.class, () -> {
            BCrypt.hashpw("foo", "$2a$10+.....................");
        });
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when salt rounds are missing")
    void testHashpwMissingSaltRounds() {
        assertThrows(IllegalArgumentException.class, () -> {
            BCrypt.hashpw("foo", "$2$a10$.....................");
        });
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for too few rounds")
    void testHashpwTooLittleRounds() {
        assertThrows(IllegalArgumentException.class, () -> {
            BCrypt.hashpw("foo", "$2a$03$......................");
        });
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for too many rounds")
    void testHashpwTooManyRounds() {
        assertThrows(IllegalArgumentException.class, () -> {
            BCrypt.hashpw("foo", "$2a$32$......................");
        });
    }

    @Test
    @DisplayName("Should hash password correctly with given salt")
    void testHashpw() {
        assertEquals("$2a$10$......................0li5vIK0lccG/IXHAOP2wBncDW/oa2u",
                BCrypt.hashpw("foo", "$2a$10$......................"));

        assertEquals("$2$09$......................GlnmyWmDnFB.MnSSUnFsiPvHsC2KPBm",
                BCrypt.hashpw("foo", "$2$09$......................"));
    }

    @Test
    @DisplayName("Should generate salt with default rounds")
    void testGensalt() {
        // Test that gensalt() generates a valid salt string
        String salt = BCrypt.gensalt();
        assertNotNull(salt);
        assertTrue(salt.startsWith("$2a$"));
        assertEquals(29, salt.length()); // BCrypt salt should be 29 characters
        
        // Test that gensalt(int) generates a valid salt string with specified rounds
        String saltWithRounds = BCrypt.gensalt(9);
        assertNotNull(saltWithRounds);
        assertTrue(saltWithRounds.startsWith("$2a$09$"));
        assertEquals(29, saltWithRounds.length());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for too few rounds in gensalt")
    void testGensaltTooLittleRounds() {
        assertThrows(IllegalArgumentException.class, () -> {
            BCrypt.gensalt(3);
        });
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for too many rounds in gensalt")
    void testGensaltTooManyRounds() {
        assertThrows(IllegalArgumentException.class, () -> {
            BCrypt.gensalt(32);
        });
    }

    @Test
    @DisplayName("Should verify password correctly with checkpw")
    void testCheckpw() {
        // Test with invalid hash
        assertFalse(BCrypt.checkpw("foo", "$2a$10$......................"));

        // Test with valid hash
        final String hashed = BCrypt.hashpw("foo", BCrypt.gensalt());
        assertTrue(BCrypt.checkpw("foo", hashed));
        assertFalse(BCrypt.checkpw("bar", hashed));
    }

    @Test
    @DisplayName("Should generate different salts on each call")
    void testGensaltGeneratesDifferentSalts() {
        String salt1 = BCrypt.gensalt();
        String salt2 = BCrypt.gensalt();
        
        assertNotEquals(salt1, salt2, "Each call to gensalt should generate a different salt");
    }

    @Test
    @DisplayName("Should verify password consistency")
    void testPasswordConsistency() {
        String password = "testPassword123";
        String salt = BCrypt.gensalt();
        
        String hash1 = BCrypt.hashpw(password, salt);
        String hash2 = BCrypt.hashpw(password, salt);
        
        assertEquals(hash1, hash2, "Hashing the same password with same salt should produce same result");
        assertTrue(BCrypt.checkpw(password, hash1));
        assertTrue(BCrypt.checkpw(password, hash2));
    }
}
