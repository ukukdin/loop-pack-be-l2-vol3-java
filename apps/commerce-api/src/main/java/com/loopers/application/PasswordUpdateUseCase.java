package com.loopers.application;

import com.loopers.domain.model.Password;
import com.loopers.domain.model.UserId;

public interface PasswordUpdateUseCase {

    /**
 * Update the password for the specified user.
 *
 * @param userId          the identifier of the user whose password will be updated
 * @param currentPassword the user's current password
 * @param newPassword     the new password to set for the user
 */
void updatePassword(UserId userId, Password currentPassword, Password newPassword);
}