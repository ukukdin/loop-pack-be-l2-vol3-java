package com.loopers.application.order;

public interface UpdateOrderPaymentUseCase {

    void completePayment(Long orderId);

    void failPayment(Long orderId);
}
