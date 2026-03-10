package com.loopers.interfaces.api.coupon.dto;

import com.loopers.application.coupon.CouponQueryUseCase;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record UserCouponResponse(
        Long userCouponId,
        Long couponId,
        String couponName,
        String discountType,
        BigDecimal discountValue,
        String status,
        LocalDateTime issuedAt,
        LocalDateTime usedAt,
        LocalDateTime expiredAt
) {
    public static UserCouponResponse from(CouponQueryUseCase.UserCouponInfo info) {
        return new UserCouponResponse(
                info.userCouponId(),
                info.couponId(),
                info.couponName(),
                info.discountType(),
                info.discountValue(),
                info.status(),
                info.issuedAt(),
                info.usedAt(),
                info.expiredAt()
        );
    }
}
