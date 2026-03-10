package com.loopers.application.coupon;

import com.loopers.domain.model.user.UserId;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface CouponQueryUseCase {

    List<UserCouponInfo> getMyCoupons(UserId userId);

    record UserCouponInfo(
            Long userCouponId,
            Long couponId,
            String couponName,
            String discountType,
            BigDecimal discountValue,
            String status,
            LocalDateTime issuedAt,
            LocalDateTime usedAt,
            LocalDateTime expiredAt
    ) {}
}
