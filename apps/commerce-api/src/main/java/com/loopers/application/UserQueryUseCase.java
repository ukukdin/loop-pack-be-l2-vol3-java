package com.loopers.application;

import com.loopers.domain.model.UserId;

public interface UserQueryUseCase {

    UserInfoResponse getUserInfo(UserId userId);

    record UserInfoResponse(
            String loginId,
            String maskedName,
            String birthday,
            String email
    ) {}
}
