package com.loopers.interfaces.api.coupon.dto;

import com.loopers.application.coupon.CouponAdminQueryUseCase;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CouponSummaryResponse(
        Long id,
        String name,
        String discountType,
        BigDecimal discountValue,
        String status,
        LocalDateTime expiredAt,
        LocalDateTime createdAt
) {
    public static CouponSummaryResponse from(CouponAdminQueryUseCase.CouponSummary summary) {
        return new CouponSummaryResponse(
                summary.id(),
                summary.name(),
                summary.discountType(),
                summary.discountValue(),
                summary.status(),
                summary.expiredAt(),
                summary.createdAt()
        );
    }
}
