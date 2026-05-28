package edu.farmingdale.taskmanagerapp;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.Locale;

/**
 * Password and recovery-answer hashing helper.
 */
final class PasswordUtil {
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String HASH_PREFIX = "PBKDF2";
    private static final int ITERATIONS = 310_000;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int SALT_BYTES = 16;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private PasswordUtil() {
    }

    static String hashPassword(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password cannot be blank.");
        }
        return hashSecret(password.toCharArray());
    }

    static boolean verifyPassword(String candidatePassword, String storedPassword) {
        if (candidatePassword == null || storedPassword == null || storedPassword.isBlank()) {
            return false;
        }

        if (!isHashed(storedPassword)) {
            return constantTimeEquals(candidatePassword, storedPassword);
        }

        String[] parts = storedPassword.split("\\$");
        if (parts.length != 4) {
            return false;
        }

        try {
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[3]);
            byte[] actualHash = derive(candidatePassword.toCharArray(), salt, iterations, expectedHash.length * 8);
            return Arrays.equals(expectedHash, actualHash);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    static boolean needsRehash(String storedPassword) {
        if (!isHashed(storedPassword)) {
            return true;
        }

        String[] parts = storedPassword.split("\\$");
        if (parts.length != 4) {
            return true;
        }

        try {
            return Integer.parseInt(parts[1]) < ITERATIONS;
        } catch (NumberFormatException e) {
            return true;
        }
    }

    static String validatePassword(String password) {
        if (password == null || password.length() < 8) {
            return "Password must be at least 8 characters long.";
        }
        if (!password.matches(".*[A-Z].*")) {
            return "Password must include at least one uppercase letter.";
        }
        if (!password.matches(".*[a-z].*")) {
            return "Password must include at least one lowercase letter.";
        }
        if (!password.matches(".*\\d.*")) {
            return "Password must include at least one number.";
        }
        return null;
    }

    static String normalizeSecurityAnswer(String answer) {
        return answer == null ? "" : answer.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean isHashed(String storedPassword) {
        return storedPassword != null && storedPassword.startsWith(HASH_PREFIX + "$");
    }

    private static String hashSecret(char[] secret) {
        byte[] salt = new byte[SALT_BYTES];
        SECURE_RANDOM.nextBytes(salt);
        byte[] hash = derive(secret, salt, ITERATIONS, KEY_LENGTH_BITS);

        return HASH_PREFIX + "$"
                + ITERATIONS + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(hash);
    }

    private static byte[] derive(char[] secret, byte[] salt, int iterations, int keyLengthBits) {
        KeySpec spec = new PBEKeySpec(secret, salt, iterations, keyLengthBits);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Unable to hash password securely.", e);
        }
    }

    private static boolean constantTimeEquals(String left, String right) {
        byte[] leftBytes = left.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] rightBytes = right.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return MessageDigest.isEqual(leftBytes, rightBytes);
    }
}
