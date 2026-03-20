package com.loopers.infrastructure.pg;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "pg-simulator", url = "${pg-simulator.url}")
public interface PaymentGatewayClient {

    @PostMapping("/api/v1/payments")
    PaymentGatewayResponse.ApiResponse<PaymentGatewayResponse.Transaction> createPayment(
           @RequestHeader("X-USER-ID") Long userId,
           @RequestBody PaymentGatewayRequest.CreatePayment request
           );

    @GetMapping("/api/v1/payments/{transactionKey}")
    PaymentGatewayResponse.ApiResponse<PaymentGatewayResponse.TransactionDetail> getTransaction(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable String transactionKey
    );

    @GetMapping("/api/v1/payments")
    PaymentGatewayResponse.ApiResponse<PaymentGatewayResponse.Order> getTransactionsByOrder(
            @RequestHeader("X-USER-ID") Long userId,
            @RequestParam String orderId
    );
}
