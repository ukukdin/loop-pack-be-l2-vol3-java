package com.loopers.infrastructure.order;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderJpaRepository extends JpaRepository<OrderJpaEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM OrderJpaEntity o WHERE o.id = :id")
    Optional<OrderJpaEntity> findByIdWithLock(@Param("id") Long id);

    List<OrderJpaEntity> findAllByUserId(String userId);

    List<OrderJpaEntity> findAllByUserIdAndCreatedAtBetween(String userId, LocalDateTime startAt, LocalDateTime endAt);

    List<OrderJpaEntity> findByStatusAndCreatedAtBefore(String status, LocalDateTime before);
}
