package com.loopers.domain.model.order.event;

import com.loopers.domain.model.common.DomainEvent;
import com.loopers.domain.model.user.UserId;

import java.time.LocalDateTime;

public record PaymentCompletedEvent(
        Long orderId,
        UserId userId,
        int paymentAmount,
        LocalDateTime occurredAt
) implements DomainEvent {

    public PaymentCompletedEvent(Long orderId, UserId userId, int paymentAmount) {
        this(orderId, userId, paymentAmount, LocalDateTime.now());
    }
}
