package edu.farmingdale.taskmanagerapp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordUtilTest {

    @Test
    void hashPasswordVerifiesOriginalPassword() {
        String hash = PasswordUtil.hashPassword("StrongPass123");

        assertTrue(PasswordUtil.verifyPassword("StrongPass123", hash));
        assertFalse(PasswordUtil.verifyPassword("WrongPass123", hash));
        assertFalse(PasswordUtil.needsRehash(hash));
    }

    @Test
    void legacyPlaintextPasswordsStillVerifyAndNeedRehash() {
        assertTrue(PasswordUtil.verifyPassword("legacyPassword", "legacyPassword"));
        assertFalse(PasswordUtil.verifyPassword("badPassword", "legacyPassword"));
        assertTrue(PasswordUtil.needsRehash("legacyPassword"));
    }

    @Test
    void passwordPolicyRejectsWeakPasswords() {
        assertNotNull(PasswordUtil.validatePassword("short"));
        assertNotNull(PasswordUtil.validatePassword("lowercase123"));
        assertNotNull(PasswordUtil.validatePassword("UPPERCASE123"));
        assertNotNull(PasswordUtil.validatePassword("NoNumbers"));
        assertNull(PasswordUtil.validatePassword("StrongPass123"));
    }

    @Test
    void normalizesSecurityAnswersForStableComparison() {
        assertEquals("first school", PasswordUtil.normalizeSecurityAnswer("  First School  "));
    }
}
