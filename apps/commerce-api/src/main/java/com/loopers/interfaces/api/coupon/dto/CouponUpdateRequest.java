package com.loopers.interfaces.api.coupon.dto;

import com.loopers.application.coupon.CouponAdminUseCase;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CouponUpdateRequest(
        @NotBlank(message = "쿠폰 이름은 필수입니다.")
        String name,
        String description,
        @NotBlank(message = "할인 유형은 필수입니다.")
        String type,
        @NotNull(message = "할인 값은 필수입니다.")
        BigDecimal value,
        BigDecimal minOrderAmount,
        @NotNull(message = "만료일은 필수입니다.")
        LocalDateTime expiredAt
) {
    public CouponAdminUseCase.CouponUpdateCommand toCommand() {
        return new CouponAdminUseCase.CouponUpdateCommand(
                name, description, type, value, minOrderAmount, expiredAt
        );
    }
}
