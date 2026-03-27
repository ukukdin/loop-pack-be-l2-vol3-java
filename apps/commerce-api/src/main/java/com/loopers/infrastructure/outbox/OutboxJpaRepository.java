package com.loopers.infrastructure.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxJpaRepository extends JpaRepository<OutboxJpaEntity, Long> {

    List<OutboxJpaEntity> findTop100ByStatusOrderByCreatedAtAsc(String status);
}
