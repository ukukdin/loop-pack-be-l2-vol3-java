package com.loopers.application;

import com.loopers.domain.model.user.UserId;

public interface AuthenticationUseCase {

    void authenticate(UserId userId, String rawPassword);
}