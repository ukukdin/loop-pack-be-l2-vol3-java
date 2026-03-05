package com.loopers.domain.model.coupon;

import com.loopers.support.error.CouponErrorCode;
import com.loopers.support.error.CouponException;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
public class CouponTarget {

    private final TargetType targetType;
    private final List<Long> targetIds;
    private final BigDecimal minOrderAmount;

    private CouponTarget(TargetType targetType, List<Long> targetIds, BigDecimal minOrderAmount) {
        this.targetType = targetType;
        this.targetIds = targetIds;
        this.minOrderAmount = minOrderAmount;
    }

    public static CouponTarget create(TargetType targetType, List<Long> targetIds, BigDecimal minOrderAmount) {
        validate(targetType, targetIds);
        return new CouponTarget(targetType, targetIds, minOrderAmount);
    }

    public static CouponTarget reconstitute(TargetType targetType, List<Long> targetIds, BigDecimal minOrderAmount) {
        return new CouponTarget(targetType, targetIds, minOrderAmount);
    }

    public boolean isApplicable(Long targetId, BigDecimal orderAmount) {
        if (orderAmount.compareTo(minOrderAmount) < 0) return false;
        if (targetType == TargetType.ALL) return true;
        return targetIds.contains(targetId);
    }

    private static void validate(TargetType targetType, List<Long> targetIds) {
        if (targetType == TargetType.SPECIFIC
                && (targetIds == null || targetIds.isEmpty())) {
            throw new CouponException(CouponErrorCode.INVALID_TARGET_IDS);
        }
    }
}
