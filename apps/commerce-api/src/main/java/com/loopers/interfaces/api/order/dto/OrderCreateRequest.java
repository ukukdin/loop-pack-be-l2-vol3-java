package com.loopers.interfaces.api.order.dto;

import com.loopers.application.order.CreateOrderUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record OrderCreateRequest(
        @NotEmpty(message = "주문 항목은 1개 이상이어야 합니다.")
        @Valid
        List<OrderItemRequest> items,
        @NotBlank(message = "수령인 이름은 필수입니다.")
        String receiverName,
        @NotBlank(message = "배송 주소는 필수입니다.")
        String address,
        String deliveryRequest,
        @NotBlank(message = "결제 수단은 필수입니다.")
        String paymentMethod,
        LocalDate desiredDeliveryDate,
        Long couponId
) {
    public CreateOrderUseCase.OrderCommand toCommand() {
        List<CreateOrderUseCase.OrderItemCommand> itemCommands = items.stream()
                .map(item -> new CreateOrderUseCase.OrderItemCommand(item.productId(), item.quantity()))
                .toList();

        return new CreateOrderUseCase.OrderCommand(
                itemCommands, receiverName, address, deliveryRequest,
                paymentMethod, desiredDeliveryDate, couponId
        );
    }

    public record OrderItemRequest(
            @NotNull(message = "상품 ID는 필수입니다.")
            Long productId,
            @Min(value = 1, message = "주문 수량은 1개 이상이어야 합니다.")
            int quantity
    ) {}
}
