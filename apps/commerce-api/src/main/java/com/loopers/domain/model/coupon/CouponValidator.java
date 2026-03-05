package com.loopers.domain.model.coupon;

import com.loopers.support.error.CouponErrorCode;
import com.loopers.support.error.CouponException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CouponValidator {

    public static void validate(
            String code,
            String name,
            DiscountPolicy discountPolicy,
            IssuancePolicy issuancePolicy,
            CouponTarget applicationTarget,
            LocalDateTime expiredAt,
            LocalDateTime now
    ) {
        validateCode(code);
        validateName(name);
        validateDiscountPolicy(discountPolicy);
        validateIssuancePolicy(issuancePolicy);
        validateCouponTarget(applicationTarget);
        validateExpiredAt(expiredAt);
    }
    private static void validateCode(String code) {
        if (code == null || code.isBlank()) {
            throw new CouponException(CouponErrorCode.INVALID_CODE);
        }
        // 코드 형식 검증 (영문+숫자 조합 등)
        if (!code.matches("^[A-Z0-9]+$")) {
            throw new CouponException(CouponErrorCode.INVALID_CODE_FORMAT);
        }
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new CouponException(CouponErrorCode.INVALID_NAME);
        }
        if (name.length() > 50) {
            throw new CouponException(CouponErrorCode.INVALID_NAME_LENGTH);
        }
    }

    private static void validateDiscountPolicy(DiscountPolicy discountPolicy) {
        if (discountPolicy == null) {
            throw new CouponException(CouponErrorCode.INVALID_DISCOUNT_POLICY);
        }
    }

    private static void validateIssuancePolicy(IssuancePolicy issuancePolicy) {
        if (issuancePolicy == null) {
            throw new CouponException(CouponErrorCode.INVALID_ISSUANCE_POLICY);
        }
    }

    private static void validateCouponTarget(CouponTarget applicationTarget) {
        if (applicationTarget == null) {
            throw new CouponException(CouponErrorCode.INVALID_APPLICATION_TARGET);
        }
    }

    private static void validateExpiredAt(LocalDateTime expiredAt) {
        if (expiredAt == null) {
            throw new CouponException(CouponErrorCode.INVALID_EXPIRED_AT);
        }
        if (expiredAt.isBefore(LocalDateTime.now())) {
            throw new CouponException(CouponErrorCode.INVALID_EXPIRED_AT);
        }
    }
}
