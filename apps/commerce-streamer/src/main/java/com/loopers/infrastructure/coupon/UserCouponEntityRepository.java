package com.loopers.infrastructure.coupon;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserCouponEntityRepository extends JpaRepository<UserCouponEntity, Long> {

    boolean existsByCouponIdAndUserId(Long couponId, String userId);
}
