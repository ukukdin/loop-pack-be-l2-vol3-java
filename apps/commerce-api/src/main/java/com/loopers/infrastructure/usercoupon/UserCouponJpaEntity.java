package com.loopers.infrastructure.usercoupon;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "user_coupons")
public class UserCouponJpaEntity {

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

    protected UserCouponJpaEntity() {}

    public UserCouponJpaEntity(Long id, Long couponId, String userId, String status,
                               LocalDateTime issuedAt, LocalDateTime usedAt) {
        this.id = id;
        this.couponId = couponId;
        this.userId = userId;
        this.status = status;
        this.issuedAt = issuedAt;
        this.usedAt = usedAt;
    }
}
