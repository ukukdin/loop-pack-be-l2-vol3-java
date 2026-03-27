package com.loopers.application.payment;

public interface PaymentCallbackUseCase {

    void handleCallback(CallbackCommand command);

    record CallbackCommand(
            String transactionKey,
            Long orderId,
            PaymentStatus status,
            String reason
    ) {}
}
