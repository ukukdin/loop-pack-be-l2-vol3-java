package com.loopers.application.coupon;

import com.loopers.domain.model.common.PageResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface CouponAdminQueryUseCase {

    CouponDetail getCoupon(Long couponId);

    PageResult<CouponSummary> getCoupons(int page, int size);

    PageResult<IssuedCouponInfo> getIssuedCoupons(Long couponId, int page, int size);

    record CouponDetail(
            Long id,
            String code,
            String name,
            String description,
            String discountType,
            BigDecimal discountValue,
            BigDecimal minOrderAmount,
            String status,
            LocalDateTime expiredAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    record CouponSummary(
            Long id,
            String name,
            String discountType,
            BigDecimal discountValue,
            String status,
            LocalDateTime expiredAt,
            LocalDateTime createdAt
    ) {}

    record IssuedCouponInfo(
            Long userCouponId,
            String userId,
            String status,
            LocalDateTime issuedAt,
            LocalDateTime usedAt
    ) {}
}
