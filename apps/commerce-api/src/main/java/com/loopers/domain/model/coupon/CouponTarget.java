package com.loopers.domain.model.coupon;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.Getter;

import java.util.List;

@Getter
public class CouponTarget {

    private final TargetType targetType;
    private final List<Long> targetIds;

    private CouponTarget(TargetType targetType, List<Long> targetIds) {
        this.targetType = targetType;
        this.targetIds = targetIds;
    }

    public static CouponTarget create(TargetType targetType, List<Long> targetIds) {
        validate(targetType, targetIds);
        return new CouponTarget(targetType, targetIds);
    }

    public static CouponTarget reconstitute(TargetType targetType, List<Long> targetIds) {
        return new CouponTarget(targetType, targetIds);
    }

    public boolean isApplicable(Long targetId) {
        if (targetType == TargetType.ALL) return true;
        return targetIds.contains(targetId);
    }

    private static void validate(TargetType targetType, List<Long> targetIds) {
        if (targetType == TargetType.SPECIFIC
                && (targetIds == null || targetIds.isEmpty())) {
            throw new CoreException(ErrorType.COUPON_INVALID_TARGET_IDS);
        }
    }
}
