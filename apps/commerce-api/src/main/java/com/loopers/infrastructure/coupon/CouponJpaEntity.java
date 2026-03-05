package com.loopers.infrastructure.coupon;

import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "coupons")
public class CouponJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false, length = 50)
    private String name;

    private String description;

    @Column(nullable = false)
    private String discountType;

    @Column(nullable = false)
    private BigDecimal discountValue;

    private BigDecimal maxDiscountValue;

    private BigDecimal minOrderAmount;

    private Integer maxIssuanceValue;

    @Column(nullable = false)
    private int issuedCount;

    private Integer maxIssuancePerUser;

    private LocalDateTime issueStartAt;

    private LocalDateTime issueEndAt;

    @Column(nullable = false)
    private String targetType;

    private String targetIds;

    private BigDecimal targetMinOrderAmount;

    @Column(nullable = false)
    private boolean isDuplicate;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private LocalDateTime expiredAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected CouponJpaEntity() {}

    public CouponJpaEntity(Long id, String code, String name, String description,
                           String discountType, BigDecimal discountValue,
                           BigDecimal maxDiscountValue, BigDecimal minOrderAmount,
                           Integer maxIssuanceValue, int issuedCount, Integer maxIssuancePerUser,
                           LocalDateTime issueStartAt, LocalDateTime issueEndAt,
                           String targetType, String targetIds, BigDecimal targetMinOrderAmount,
                           boolean isDuplicate, String status, LocalDateTime expiredAt,
                           LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.description = description;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.maxDiscountValue = maxDiscountValue;
        this.minOrderAmount = minOrderAmount;
        this.maxIssuanceValue = maxIssuanceValue;
        this.issuedCount = issuedCount;
        this.maxIssuancePerUser = maxIssuancePerUser;
        this.issueStartAt = issueStartAt;
        this.issueEndAt = issueEndAt;
        this.targetType = targetType;
        this.targetIds = targetIds;
        this.targetMinOrderAmount = targetMinOrderAmount;
        this.isDuplicate = isDuplicate;
        this.status = status;
        this.expiredAt = expiredAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
