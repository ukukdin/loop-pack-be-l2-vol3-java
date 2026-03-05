package com.loopers.interfaces.api.coupon.dto;

import com.loopers.application.coupon.CouponAdminQueryUseCase;

import java.time.LocalDateTime;

public record IssuedCouponResponse(
        Long userCouponId,
        String userId,
        String status,
        LocalDateTime issuedAt,
        LocalDateTime usedAt
) {
    public static IssuedCouponResponse from(CouponAdminQueryUseCase.IssuedCouponInfo info) {
        return new IssuedCouponResponse(
                info.userCouponId(),
                info.userId(),
                info.status(),
                info.issuedAt(),
                info.usedAt()
        );
    }
}
