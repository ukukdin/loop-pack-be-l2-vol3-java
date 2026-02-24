package com.loopers.infrastructure.order;

import com.loopers.infrastructure.order.OrderJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderJpaRepository extends JpaRepository<OrderJpaEntity, Long> {

    List<OrderJpaEntity> findAllByUserId(String userId);
}
