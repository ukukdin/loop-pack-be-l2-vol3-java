package com.loopers.application;

import com.loopers.domain.model.user.UserId;

public interface UnlikeUseCase {

    void unlike(UserId userId, Long productId);
}
