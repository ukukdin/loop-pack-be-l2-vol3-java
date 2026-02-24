package com.loopers.application;

import com.loopers.domain.model.user.UserId;

public interface LikeUseCase {

    void like(UserId userId, Long productId);
}
