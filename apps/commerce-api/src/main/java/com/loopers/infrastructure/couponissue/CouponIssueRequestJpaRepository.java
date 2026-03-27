package com.loopers.infrastructure.couponissue;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CouponIssueRequestJpaRepository extends JpaRepository<CouponIssueRequestJpaEntity, Long> {

    Optional<CouponIssueRequestJpaEntity> findTopByCouponIdAndUserIdOrderByCreatedAtDesc(Long couponId, String userId);
}
