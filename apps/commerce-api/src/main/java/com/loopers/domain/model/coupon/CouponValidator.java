package com.loopers.domain.model.coupon;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

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
        validateExpiredAt(expiredAt, now);
    }

    private static void validateCode(String code) {
        if (code == null || code.isBlank()) {
            throw new CoreException(ErrorType.COUPON_INVALID_CODE);
        }
        if (!code.matches("^[A-Z0-9]+$")) {
            throw new CoreException(ErrorType.COUPON_INVALID_CODE_FORMAT);
        }
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new CoreException(ErrorType.COUPON_INVALID_NAME);
        }
        if (name.length() > 50) {
            throw new CoreException(ErrorType.COUPON_INVALID_NAME_LENGTH);
        }
    }

    private static void validateDiscountPolicy(DiscountPolicy discountPolicy) {
        if (discountPolicy == null) {
            throw new CoreException(ErrorType.COUPON_INVALID_DISCOUNT_POLICY);
        }
    }

    private static void validateIssuancePolicy(IssuancePolicy issuancePolicy) {
        if (issuancePolicy == null) {
            throw new CoreException(ErrorType.COUPON_INVALID_ISSUANCE_POLICY);
        }
    }

    private static void validateCouponTarget(CouponTarget applicationTarget) {
        if (applicationTarget == null) {
            throw new CoreException(ErrorType.COUPON_INVALID_TARGET);
        }
    }

    private static void validateExpiredAt(LocalDateTime expiredAt, LocalDateTime now) {
        if (expiredAt == null) {
            throw new CoreException(ErrorType.COUPON_INVALID_EXPIRED_AT);
        }
        if (expiredAt.isBefore(now)) {
            throw new CoreException(ErrorType.COUPON_INVALID_EXPIRED_AT);
        }
    }
}
