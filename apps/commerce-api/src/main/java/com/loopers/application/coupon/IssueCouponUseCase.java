package com.loopers.application.coupon;

import com.loopers.domain.model.user.UserId;

public interface IssueCouponUseCase {

    void issue(UserId userId, Long couponId);
}
