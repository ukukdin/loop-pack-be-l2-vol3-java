package com.loopers.domain.model.order.event;

import com.loopers.domain.model.common.DomainEvent;
import com.loopers.domain.model.user.UserId;

import java.time.LocalDateTime;
import java.util.List;

public record OrderCreatedEvent(
        Long orderId,
        UserId userId,
        List<Long> productIds,
        LocalDateTime occurredAt
) implements DomainEvent {

    public OrderCreatedEvent(Long orderId, UserId userId, List<Long> productIds) {
        this(orderId, userId, List.copyOf(productIds), LocalDateTime.now());
    }
}
