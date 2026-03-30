package com.loopers.infrastructure.couponissue;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "coupon_issue_requests", indexes = {
        @Index(name = "idx_coupon_issue_user", columnList = "couponId, userId")
})
public class CouponIssueRequestJpaEntity {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_REJECTED = "REJECTED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long couponId;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String status;

    private String rejectReason;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime processedAt;

    protected CouponIssueRequestJpaEntity() {}

    public CouponIssueRequestJpaEntity(Long couponId, String userId) {
        this.couponId = couponId;
        this.userId = userId;
        this.status = STATUS_PENDING;
        this.createdAt = LocalDateTime.now();
    }
}
