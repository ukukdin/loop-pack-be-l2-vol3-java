package com.loopers.application.like;

import com.loopers.domain.model.user.UserId;

import java.time.LocalDateTime;
import java.util.List;

public interface LikeQueryUseCase {

    List<LikeInfo> getMyLikes(UserId userId);

    record LikeInfo(
            Long productId,
            String productName,
            int price,
            LocalDateTime likedAt
    ) {}
}
