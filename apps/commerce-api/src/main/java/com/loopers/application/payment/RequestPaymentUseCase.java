package com.loopers.application.payment;

import com.loopers.domain.model.user.UserId;

public interface RequestPaymentUseCase {

    PaymentResult requestPayment(UserId userId, PaymentCommand command);

    record PaymentCommand(
            Long orderId,
            String cardType,
            String cardNo,
            Long amount
    ) {}

    record PaymentResult(
            String transactionKey,
            String status,
            String reason
    ) {}
}
