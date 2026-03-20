package com.loopers.application.order;

import com.loopers.domain.model.user.UserId;

import java.time.LocalDate;
import java.util.List;

public interface CreateOrderUseCase {

    CreateOrderResult createOrder(UserId userId, OrderCommand command);

    CreateOrderResult getOrderPaymentInfo(UserId userId, Long orderId);

    record CreateOrderResult(
            Long orderId,
            Long paymentAmount
    ) {}

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
