package com.loopers.infrastructure.coupon;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponIssueRequestRepository extends JpaRepository<CouponIssueRequestEntity, Long> {
}
