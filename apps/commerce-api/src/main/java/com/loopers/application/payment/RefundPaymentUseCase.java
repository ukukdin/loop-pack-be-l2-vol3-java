package com.loopers.application.payment;

import com.loopers.domain.model.user.UserId;

public interface RefundPaymentUseCase {

    void refundPayment(UserId userId, Long orderId);
}
