package com.loopers.infrastructure.metrics;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ProductMetricsJpaRepository extends JpaRepository<ProductMetricsJpaEntity, Long> {

    @Modifying
    @Query(value = """
            INSERT INTO product_metrics (product_id, like_count, order_count, view_count, total_sales_amount, updated_at)
            VALUES (:productId, :likeCount, 0, 0, 0, NOW())
            ON CONFLICT (product_id)
            DO UPDATE SET like_count = product_metrics.like_count + :likeCount, updated_at = NOW()
            """, nativeQuery = true)
    void upsertLikeCount(Long productId, long likeCount);

    @Modifying
    @Query(value = """
            INSERT INTO product_metrics (product_id, like_count, order_count, view_count, total_sales_amount, updated_at)
            VALUES (:productId, 0, :orderCount, 0, 0, NOW())
            ON CONFLICT (product_id)
            DO UPDATE SET order_count = product_metrics.order_count + :orderCount, updated_at = NOW()
            """, nativeQuery = true)
    void upsertOrderCount(Long productId, long orderCount);

    @Modifying
    @Query(value = """
            INSERT INTO product_metrics (product_id, like_count, order_count, view_count, total_sales_amount, updated_at)
            VALUES (:productId, 0, 0, 0, :amount, NOW())
            ON CONFLICT (product_id)
            DO UPDATE SET total_sales_amount = product_metrics.total_sales_amount + :amount, updated_at = NOW()
            """, nativeQuery = true)
    void upsertSalesAmount(Long productId, long amount);
}
