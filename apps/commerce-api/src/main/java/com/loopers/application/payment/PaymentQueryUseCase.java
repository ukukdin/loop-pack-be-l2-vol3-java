package com.loopers.application.payment;

import com.loopers.domain.model.user.UserId;

public interface PaymentQueryUseCase {

    PaymentStatusResult getPaymentStatus(UserId userId, Long orderId);

    record PaymentStatusResult(
            String transactionKey,
            PaymentStatus status,
            String reason
    ) {}
}
