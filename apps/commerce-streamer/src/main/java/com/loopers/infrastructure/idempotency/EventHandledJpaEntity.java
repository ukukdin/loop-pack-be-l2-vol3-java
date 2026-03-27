package com.loopers.infrastructure.idempotency;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "event_handled")
public class EventHandledJpaEntity {

    @Id
    private Long eventId;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private LocalDateTime handledAt;

    protected EventHandledJpaEntity() {}

    public EventHandledJpaEntity(Long eventId, String eventType) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.handledAt = LocalDateTime.now();
    }
}
