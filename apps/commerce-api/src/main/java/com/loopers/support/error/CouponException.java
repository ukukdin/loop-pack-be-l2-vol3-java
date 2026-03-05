package com.loopers.support.error;

public class CouponException extends RuntimeException {
    private final CouponErrorCode errorCode;

    public CouponException(CouponErrorCode errorCode){
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
