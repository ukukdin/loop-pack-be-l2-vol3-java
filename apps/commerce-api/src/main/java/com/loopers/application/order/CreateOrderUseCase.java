package com.loopers.application.order;

import com.loopers.domain.model.user.UserId;

import java.time.LocalDate;
import java.util.List;

public interface CreateOrderUseCase {

    void createOrder(UserId userId, OrderCommand command);

    record OrderCommand(
            List<OrderItemCommand> items,
            String receiverName,
            String address,
            String deliveryRequest,
            String paymentMethod,
            LocalDate desiredDeliveryDate,
            Long couponId
    ) {}

    record OrderItemCommand(
            Long productId,
            int quantity
    ) {}
}
