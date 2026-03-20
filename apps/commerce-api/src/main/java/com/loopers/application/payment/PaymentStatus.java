package com.loopers.application.payment;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

public enum PaymentStatus {
    PENDING,
    SUCCESS,
    FAILED;

    public static PaymentStatus from(String value) {
        if (value == null || value.isBlank()) {
            throw new CoreException(ErrorType.VALIDATION_ERROR, "결제 상태는 필수입니다.");
        }
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CoreException(ErrorType.VALIDATION_ERROR,
                    "허용되지 않는 결제 상태입니다: " + value + " (허용값: PENDING, SUCCESS, FAILED)");
        }
    }
}
