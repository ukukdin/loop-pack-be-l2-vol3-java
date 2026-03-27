package com.loopers.infrastructure.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EventHandledJpaRepository extends JpaRepository<EventHandledJpaEntity, Long> {
}
