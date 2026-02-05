package com.loopers.domain.model;


import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class User {
    private Long id;
    private final UserId userId;
    private final String encodedPassword;
    private final Birthday birth; // YYYYMMDD format with default value
    private final Email email;
    private final WrongPasswordCount wrongPasswordCount;

    public static User create(UserId userId, String encodedPassword, Birthday birth, Email email, WrongPasswordCount wrongPasswordCount) {
        return new User(null,userId,encodedPassword,birth,email, wrongPasswordCount);
    }

    public boolean matchesPassword(Password password, PasswordMatchChecker checker) {
        return checker.matches(password, this.encodedPassword);
    }

    @FunctionalInterface
    public interface PasswordMatchChecker {
        boolean matches(Password password, String encodingPassword);
    }
}
