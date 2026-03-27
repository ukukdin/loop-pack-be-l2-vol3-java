package com.loopers.infrastructure.pg;

import java.util.List;

public class PaymentGatewayResponse {

    public record ApiResponse<T>(
            String status,
            T data
    ) {}

    public record Transaction(
            String transactionKey,
            String status,
            String reason
    ) {}

    public record TransactionDetail(
            String transactionKey,
            String orderId,
            String cardType,
            String cardNo,
            Long amount,
            String status,
            String reason
    ){}

    public record Order(
            String orderId,
            List<Transaction> transactions
    ) {}
}
