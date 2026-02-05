package com.loopers.application;

import com.loopers.domain.model.UserId;

public interface UserQueryUseCase {

    /**
 * Retrieve user information for the specified user identifier.
 *
 * @param userId the identifier of the user to fetch information for
 * @return a UserInfoResponse containing the user's loginId, maskedName, birthday, and email
 */
UserInfoResponse getUserInfo(UserId userId);

    record UserInfoResponse(
            String loginId,
            String maskedName,
            String birthday,
            String email
    ) {}
}