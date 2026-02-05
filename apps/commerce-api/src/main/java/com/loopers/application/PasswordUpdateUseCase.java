package com.loopers.application;

import com.loopers.domain.model.Password;
import com.loopers.domain.model.UserId;

public interface PasswordUpdateUseCase {

    void updatePassword(UserId userId, Password currentPassword, Password newPassword);
}
