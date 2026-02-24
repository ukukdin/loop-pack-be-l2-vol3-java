package com.loopers.interfaces.api.like.dto;

import com.loopers.application.like.LikeQueryUseCase;

import java.time.LocalDateTime;

public record LikeResponse(
        Long productId,
        String productName,
        int price,
        LocalDateTime likedAt
) {
    public static LikeResponse from(LikeQueryUseCase.LikeInfo info) {
        return new LikeResponse(
                info.productId(),
                info.productName(),
                info.price(),
                info.likedAt()
        );
    }
}
