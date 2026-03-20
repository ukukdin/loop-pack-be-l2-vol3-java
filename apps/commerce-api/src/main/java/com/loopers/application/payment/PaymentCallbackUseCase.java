package com.loopers.application.payment;

public interface PaymentCallbackUseCase {

    void handleCallback(CallbackCommand command);

    record CallbackCommand(
            String transactionKey,
            String orderId,
            String status,
            String reason
    ){}
}
