package com.loopers.infrastructure.coupon;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "coupon_issue_requests")
public class CouponIssueRequestEntity {

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

    protected CouponIssueRequestEntity() {}

    public void markSuccess() {
        this.status = STATUS_SUCCESS;
        this.processedAt = LocalDateTime.now();
    }

    public void markRejected(String reason) {
        this.status = STATUS_REJECTED;
        this.rejectReason = reason;
        this.processedAt = LocalDateTime.now();
    }
}
