package com.loopers.interfaces.api.coupon.dto;

import com.loopers.application.coupon.CouponAdminUseCase;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CouponDetailResponse(
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
) {
    public static CouponDetailResponse from(CouponAdminUseCase.CouponDetail detail) {
        return new CouponDetailResponse(
                detail.id(),
                detail.code(),
                detail.name(),
                detail.description(),
                detail.discountType(),
                detail.discountValue(),
                detail.minOrderAmount(),
                detail.status(),
                detail.expiredAt(),
                detail.createdAt(),
                detail.updatedAt()
        );
    }
}
