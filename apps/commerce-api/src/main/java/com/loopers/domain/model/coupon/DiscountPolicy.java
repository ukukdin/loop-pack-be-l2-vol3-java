package com.loopers.domain.model.coupon;

import com.loopers.support.error.CouponErrorCode;
import com.loopers.support.error.CouponException;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class DiscountPolicy {
    private final DiscountType discountType;
    private final BigDecimal discountValue;
    private final BigDecimal maxDiscountValue;
    private final BigDecimal minOrderAmount;

    private DiscountPolicy(DiscountType discountType, BigDecimal discountValue,
                           BigDecimal maxDiscountValue, BigDecimal minOrderAmount) {
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.maxDiscountValue = maxDiscountValue;
        this.minOrderAmount = minOrderAmount;
    }

    public static DiscountPolicy create(
            DiscountType discountType,
            BigDecimal discountValue,
            BigDecimal maxDiscountValue,
            BigDecimal minOrderAmount
    ) {
        validate(discountType, discountValue);
        return new DiscountPolicy(discountType, discountValue, maxDiscountValue, minOrderAmount);
    }



    public BigDecimal calculate(BigDecimal orderAmount){
        if (discountType == DiscountType.FIXED) {
            return discountValue;
        }
        BigDecimal calculated = orderAmount.multiply(discountValue);
        return maxDiscountValue != null ? calculated.min(maxDiscountValue) : calculated;
    }
    private static void validate(DiscountType discountType, BigDecimal discountValue) {
        if (discountValue == null || discountValue.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CouponException(CouponErrorCode.INVALID_DISCOUNT_VALUE);
        }
        if (discountType == DiscountType.PERCENTAGE
                && discountValue.compareTo(BigDecimal.ONE) > 0) {
            throw new CouponException(CouponErrorCode.INVALID_DISCOUNT_RATE);
        }
    }


}
