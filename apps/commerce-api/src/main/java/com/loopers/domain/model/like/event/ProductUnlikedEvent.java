package com.loopers.domain.model.like.event;

import com.loopers.domain.model.common.DomainEvent;
import com.loopers.domain.model.user.UserId;

import java.time.LocalDateTime;

public record ProductUnlikedEvent(
        Long productId,
        UserId userId,
        LocalDateTime occurredAt
) implements DomainEvent {

    public ProductUnlikedEvent(Long productId, UserId userId) {
        this(productId, userId, LocalDateTime.now());
    }
}
