package com.loopers.domain.model.coupon;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class Coupon {

    private final Long id;
    private final String code;
    private final String name;
    private final String description;

    private final DiscountPolicy discountPolicy;
    private final IssuancePolicy issuancePolicy;
    private final LocalDateTime expiredAt;
    private final CouponTarget applicationTarget;

    private final boolean isDuplicate;
    private final CouponStatus status;

    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private Coupon(Long id, String code, String name, String description,
                   DiscountPolicy discountPolicy, IssuancePolicy issuancePolicy,
                   LocalDateTime expiredAt, CouponTarget applicationTarget,
                   boolean isDuplicate, CouponStatus status,
                   LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.description = description;
        this.discountPolicy = discountPolicy;
        this.issuancePolicy = issuancePolicy;
        this.expiredAt = expiredAt;
        this.applicationTarget = applicationTarget;
        this.isDuplicate = isDuplicate;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Coupon create(
            String code, String name, String description,
            DiscountPolicy discountPolicy, IssuancePolicy issuancePolicy,
            LocalDateTime expiredAt, CouponTarget applicationTarget,
            boolean isDuplicate
    ) {
        CouponValidator.validate(code, name, discountPolicy,
                issuancePolicy, applicationTarget, expiredAt, LocalDateTime.now());

        return new Coupon(null, code, name, description,
                discountPolicy, issuancePolicy, expiredAt, applicationTarget,
                isDuplicate, CouponStatus.AVAILABLE,
                LocalDateTime.now(), LocalDateTime.now());
    }

    public static Coupon reconstitute(
            Long id, String code, String name, String description,
            DiscountPolicy discountPolicy, IssuancePolicy issuancePolicy,
            LocalDateTime expiredAt, CouponTarget applicationTarget,
            boolean isDuplicate, CouponStatus status,
            LocalDateTime createdAt, LocalDateTime updatedAt
    ) {
        return new Coupon(id, code, name, description,
                discountPolicy, issuancePolicy, expiredAt, applicationTarget,
                isDuplicate, status, createdAt, updatedAt);
    }

    public Coupon update(String name, String description, DiscountPolicy discountPolicy,
                         LocalDateTime expiredAt) {
        return new Coupon(this.id, this.code, name, description,
                discountPolicy, this.issuancePolicy, expiredAt, this.applicationTarget,
                this.isDuplicate, this.status, this.createdAt, LocalDateTime.now());
    }

    public Coupon issue() {
        if (!isAvailable()) {
            throw new IllegalStateException("발급할 수 없는 쿠폰입니다.");
        }
        IssuancePolicy incremented = this.issuancePolicy.incrementIssuedCount();
        return new Coupon(this.id, this.code, this.name, this.description,
                this.discountPolicy, incremented, this.expiredAt, this.applicationTarget,
                this.isDuplicate, this.status, this.createdAt, LocalDateTime.now());
    }

    public boolean isExpired() {
        return expiredAt.isBefore(LocalDateTime.now());
    }

    public boolean isAvailable() {
        return status == CouponStatus.AVAILABLE && !isExpired();
    }
}
