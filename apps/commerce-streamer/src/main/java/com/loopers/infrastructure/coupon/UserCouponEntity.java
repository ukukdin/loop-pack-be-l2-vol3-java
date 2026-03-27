package com.loopers.infrastructure.coupon;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "user_coupons")
public class UserCouponEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long couponId;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime issuedAt;

    private LocalDateTime usedAt;

    protected UserCouponEntity() {}

    public static UserCouponEntity issue(Long couponId, String userId) {
        UserCouponEntity entity = new UserCouponEntity();
        entity.couponId = couponId;
        entity.userId = userId;
        entity.status = "AVAILABLE";
        entity.issuedAt = LocalDateTime.now();
        return entity;
    }
}
