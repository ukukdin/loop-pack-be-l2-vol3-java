package com.loopers.infrastructure.metrics;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "product_metrics")
public class ProductMetricsJpaEntity {

    @Id
    private Long productId;

    @Column(nullable = false)
    private long likeCount;

    @Column(nullable = false)
    private long orderCount;

    @Column(nullable = false)
    private long viewCount;

    @Column(nullable = false)
    private long totalSalesAmount;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected ProductMetricsJpaEntity() {}

    public ProductMetricsJpaEntity(Long productId) {
        this.productId = productId;
        this.likeCount = 0;
        this.orderCount = 0;
        this.viewCount = 0;
        this.totalSalesAmount = 0;
        this.updatedAt = LocalDateTime.now();
    }
}
