package com.loopers.application.coupon;

import com.loopers.domain.model.user.UserId;

public interface RequestCouponIssueUseCase {

    Long requestIssue(UserId userId, Long couponId);

    String getIssueStatus(UserId userId, Long couponId);
}
