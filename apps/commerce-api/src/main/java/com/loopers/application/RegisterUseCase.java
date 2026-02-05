package com.loopers.application;

import com.loopers.domain.model.*;

public interface RegisterUseCase {

    void register(UserId userId, UserName userName, String encodedPassword, Birthday birthday, Email email);
}
