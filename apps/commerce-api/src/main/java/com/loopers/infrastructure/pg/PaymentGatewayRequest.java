package com.loopers.infrastructure.pg;

public class PaymentGatewayRequest {

    public record CreatePayment(
            String orderId,
            String cardType,
            String cardNo,
            Long amount,
            String callbackUrl
    ) {
        @Override
        public String toString() {
            String masked = (cardNo == null || cardNo.length() < 4)
                    ? "****"
                    : "****-****-****-" + cardNo.substring(cardNo.length() - 4);
            return "CreatePayment[orderId=%s, cardType=%s, cardNo=%s, amount=%s, callbackUrl=%s]"
                    .formatted(orderId, cardType, masked, amount, callbackUrl);
        }
    }

}
