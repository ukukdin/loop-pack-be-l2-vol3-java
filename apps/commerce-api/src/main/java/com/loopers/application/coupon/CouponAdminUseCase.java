package com.loopers.application.coupon;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface CouponAdminUseCase {

    void createCoupon(CouponCreateCommand command);

    void updateCoupon(Long couponId, CouponUpdateCommand command);

    void deleteCoupon(Long couponId);

    record CouponCreateCommand(
            String code,
            String name,
            String description,
            String type,
            BigDecimal value,
            BigDecimal minOrderAmount,
            LocalDateTime expiredAt
    ) {}

    record CouponUpdateCommand(
            String name,
            String description,
            String type,
            BigDecimal value,
            BigDecimal minOrderAmount,
            LocalDateTime expiredAt
    ) {}
}
