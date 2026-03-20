package com.loopers.infrastructure.pg;

public class PaymentGatewayRequest {

    public record CreatePayment(
            String orderId,
            String cardType,
            String cardNo,
            Long amount,
            String callbackUrl
    ) {}

}
