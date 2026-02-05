package com.loopers.domain.service;

public interface PasswordEncoder {

    /**
 * Encode a raw password into its stored representation.
 *
 * @param rawPassword the raw (unencoded) password to encode
 * @return the encoded password string suitable for storage
 */
String encrypt(String rawPassword);

    /**
 * Checks whether a raw password corresponds to the given encoded password.
 *
 * @param rawPassword     the unencoded password to verify
 * @param encodedPassword the encoded password to compare against
 * @return                 `true` if the raw password matches the encoded password, `false` otherwise
 */
boolean matches(String rawPassword, String encodedPassword);


}