package com.loopers.domain.model.order.event;

import com.loopers.domain.model.common.DomainEvent;
import com.loopers.domain.model.user.UserId;

import java.time.LocalDateTime;
import java.util.List;

public record OrderCancelledEvent(
        Long orderId,
        UserId userId,
        List<CancelledItem> cancelledItems,
        Long userCouponId,
        boolean needsRefund,
        LocalDateTime occurredAt
) implements DomainEvent {

    public record CancelledItem(Long productId, int quantity) {}

    public OrderCancelledEvent(Long orderId, UserId userId, List<CancelledItem> cancelledItems,
                               Long userCouponId, boolean needsRefund) {
        this(orderId, userId, cancelledItems, userCouponId, needsRefund, LocalDateTime.now());
    }
}
