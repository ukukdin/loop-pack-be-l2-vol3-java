package com.loopers.infrastructure.security;

import com.loopers.domain.service.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class Sha256PasswordEncoder implements PasswordEncoder {

    /**
     * Produces an encoded password string that includes a per-password salt and its hash.
     *
     * @param rawPassword the plain-text password to encode
     * @return a string in the format "salt:hash" where `salt` is a Base64-encoded salt and
     *         `hash` is the Base64-encoded SHA-256 digest of the concatenation of the
     *         provided password and that salt
     */
    @Override
    public String encrypt(String rawPassword) {
        String salt = generateSalt();
        String hashed = sha256(rawPassword + salt);
        return salt + ":" + hashed;
    }

    /**
     * Checks whether a raw password matches an encoded password in the "salt:hash" format.
     *
     * @param rawPassword     the plaintext password to verify
     * @param encodedPassword the stored value in the form "salt:hash" where `salt` and `hash` are Base64 strings;
     *                        returns `false` if the format is not exactly two parts separated by ':'
     * @return                `true` if the SHA-256 hash of `rawPassword` concatenated with the extracted salt equals the stored hash, `false` otherwise
     */
    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
        String[] parts = encodedPassword.split(":");
        if (parts.length != 2) {
            return false;
        }
        String salt = parts[0];
        String storedHash = parts[1];
        String inputHash = sha256(rawPassword + salt);
        return storedHash.equals(inputHash);
    }

    /**
     * Generate a cryptographically secure random salt encoded as Base64.
     *
     * @return a Base64-encoded 16-byte random salt
     */
    private String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] saltBytes = new byte[16];
        random.nextBytes(saltBytes);
        return Base64.getEncoder().encodeToString(saltBytes);
    }

    /**
     * Compute the SHA-256 digest of the given input and return it as a Base64-encoded string.
     *
     * @param input the input string to hash
     * @return the Base64-encoded SHA-256 digest of the input
     * @throws RuntimeException if the SHA-256 MessageDigest algorithm is not available
     */
    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 을 찾지 못했습니다.", e);
        }
    }
}