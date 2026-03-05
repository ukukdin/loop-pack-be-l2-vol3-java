package com.loopers.domain.repository;

import com.loopers.domain.model.common.PageResult;
import com.loopers.domain.model.user.UserId;
import com.loopers.domain.model.userCoupon.UserCoupon;

import java.util.List;
import java.util.Optional;

public interface UserCouponRepository {

    UserCoupon save(UserCoupon userCoupon);

    Optional<UserCoupon> findById(Long id);

    Optional<UserCoupon> findByIdWithLock(Long id);

    List<UserCoupon> findByUserId(UserId userId);

    PageResult<UserCoupon> findByCouponId(Long couponId, int page, int size);

    int countByUserIdAndCouponId(UserId userId, Long couponId);
}
