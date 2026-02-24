package com.loopers.infrastructure.repository;

import com.loopers.infrastructure.entity.OrderJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderJpaRepository extends JpaRepository<OrderJpaEntity, Long> {

    List<OrderJpaEntity> findAllByUserId(String userId);
}
