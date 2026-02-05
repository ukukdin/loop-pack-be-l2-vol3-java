package com.loopers.domain.model;


import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class User {
    private Long id;
    private final UserId userId;
    private final UserName userName;
    private final String encodedPassword;
    private final Birthday birth; // YYYYMMDD format with default value
    private final Email email;
    private final WrongPasswordCount wrongPasswordCount;
    private LocalDateTime createdAt;

    /**
     * Create a new User instance for registration with no persistence id.
     *
     * @param userId             the user's identifier
     * @param userName           the user's display name
     * @param encodedPassword    the password already encoded/stored form
     * @param birth              the user's birthday (formatted YYYYMMDD)
     * @param email              the user's email address
     * @param wrongPasswordCount the initial wrong-password attempt count
     * @param createdAt          the creation timestamp for the new user
     * @return                   a User whose `id` is `null` representing a newly registered user
     */
    public static User register(UserId userId,UserName userName, String encodedPassword, Birthday birth, Email email, WrongPasswordCount wrongPasswordCount, LocalDateTime createdAt) {
        return new User(null,userId,userName,encodedPassword,birth,email, wrongPasswordCount,createdAt);
    }

    /**
     * Reconstructs a User instance from persisted or externally-provided field values.
     *
     * @param id               the persistent identifier of the user, may be null if unknown
     * @param userId           the domain user identifier value object
     * @param userName         the user's display name value object
     * @param encodedPassword  the stored encoded password
     * @param birth            the user's birthday value object (formatted YYYYMMDD)
     * @param email            the user's email value object
     * @param wrongPasswordCount the user's recorded consecutive wrong password attempts
     * @param createdAt        the timestamp when the user was originally created
     * @return                 a User populated with the given field values
     */
    public static User reconstitute(Long id, UserId userId, UserName userName, String encodedPassword, Birthday birth, Email email, WrongPasswordCount wrongPasswordCount, LocalDateTime createdAt) {
        return new User(id, userId, userName, encodedPassword, birth, email, wrongPasswordCount, createdAt);
    }

    /**
     * Checks whether the given password corresponds to this user's stored encoded password.
     *
     * @param password the plaintext password to verify
     * @param checker  the strategy used to compare the provided password with the stored encoded password
     * @return `true` if the provided password matches this user's encoded password, `false` otherwise
     */
    public boolean matchesPassword(Password password, PasswordMatchChecker checker) {
        return checker.matches(password, this.encodedPassword);
    }

    /**
     * Create a new User instance with an updated encoded password.
     *
     * @param newEncodedPassword the encoded password to set on the returned user
     * @return a new User with `encodedPassword` set to `newEncodedPassword` and all other fields unchanged
     */
    public User changePassword(String newEncodedPassword) {
        return new User(
                this.id,
                this.userId,
                this.userName,
                newEncodedPassword,
                this.birth,
                this.email,
                this.wrongPasswordCount,
                this.createdAt
        );
    }

    @FunctionalInterface
    public interface PasswordMatchChecker {
        /**
 * Checks whether the provided Password corresponds to the given encoded password.
 *
 * @param password         the plain Password value to verify
 * @param encodingPassword the stored encoded password string to compare against
 * @return                 `true` if the password matches `encodingPassword`, `false` otherwise
 */
boolean matches(Password password, String encodingPassword);
    }
}