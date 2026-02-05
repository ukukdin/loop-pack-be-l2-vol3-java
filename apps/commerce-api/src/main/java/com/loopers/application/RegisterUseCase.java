package com.loopers.application;

import com.loopers.domain.model.*;

public interface RegisterUseCase {

    /**
 * Registers a new user with the provided identity, credentials, and contact information.
 *
 * @param userId the unique identifier for the new user
 * @param userName the user's display name
 * @param encodedPassword the user's password already encoded (hashed)
 * @param birthday the user's date of birth
 * @param email the user's email address
 */
void register(UserId userId, UserName userName, String encodedPassword, Birthday birthday, Email email);
}