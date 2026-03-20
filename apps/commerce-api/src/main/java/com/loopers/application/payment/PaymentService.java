package com.loopers.application.payment;

import com.loopers.application.order.UpdateOrderPaymentUseCase;
import com.loopers.domain.model.user.UserId;
import com.loopers.infrastructure.pg.PaymentGatewayClient;
import com.loopers.infrastructure.pg.PaymentGatewayRequest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService implements RequestPaymentUseCase, PaymentCallbackUseCase, PaymentQueryUseCase {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentGatewayClient pgClient;
    private final UpdateOrderPaymentUseCase updateOrderPaymentUseCase;

    @Value("${payment.callback-url}")
    private String callbackUrl;

    public PaymentService(PaymentGatewayClient pgClient,
                          UpdateOrderPaymentUseCase updateOrderPaymentUseCase) {
        this.pgClient = pgClient;
        this.updateOrderPaymentUseCase = updateOrderPaymentUseCase;
    }

    @Override
    @CircuitBreaker(name = "pg-simulator", fallbackMethod = "requestPaymentFallback")
    @Retry(name = "pg-simulator")
    public PaymentResult requestPayment(UserId userId, PaymentCommand command) {
        var response = pgClient.createPayment(
                Long.valueOf(userId.getValue()),
                new PaymentGatewayRequest.CreatePayment(
                        String.valueOf(command.orderId()),
                        command.cardType(),
                        command.cardNo(),
                        command.amount(),
                        callbackUrl
                )
        );
        return new PaymentResult(
                response.data().transactionKey(),
                PaymentStatus.from(response.data().status()),
                response.data().reason()
        );
    }

    private PaymentResult requestPaymentFallback(UserId userId, PaymentCommand command, Exception e) {
        log.warn("PG 결제 요청 실패 (fallback) - orderId: {}, error: {}", command.orderId(), e.getMessage(), e);

        try {
            return toPaymentResult(getPaymentStatus(userId, command.orderId()));
        } catch (Exception ex) {
            log.warn("PG 상태 확인도 실패 - orderId: {}, 스케줄러에서 복구 예정", command.orderId(), ex);
            return new PaymentResult(null, PaymentStatus.PENDING, "PG 일시적 장애로 결제 대기 중");
        }
    }

    @Override
    @Transactional
    public PaymentStatusResult getPaymentStatus(UserId userId, Long orderId) {
        var response = pgClient.getTransactionsByOrder(
                Long.valueOf(userId.getValue()),
                String.valueOf(orderId)
        );

        var transactions = response.data().transactions();
        if (transactions == null || transactions.isEmpty()) {
            return new PaymentStatusResult(null, PaymentStatus.PENDING, "PG에 결제 내역 없음");
        }

        var latest = transactions.get(transactions.size() - 1);
        PaymentStatus status = PaymentStatus.from(latest.status());

        syncOrderStatus(orderId, status);

        return new PaymentStatusResult(latest.transactionKey(), status, latest.reason());
    }

    @Override
    @Transactional
    public void handleCallback(CallbackCommand command) {
        syncOrderStatus(command.orderId(), command.status());
    }

    private void syncOrderStatus(Long orderId, PaymentStatus status) {
        switch (status) {
            case SUCCESS -> updateOrderPaymentUseCase.completePayment(orderId);
            case FAILED -> updateOrderPaymentUseCase.failPayment(orderId);
            case PENDING -> { /* 대기 상태 — 변경 없음 */ }
        }
    }

    private PaymentResult toPaymentResult(PaymentStatusResult statusResult) {
        return new PaymentResult(statusResult.transactionKey(), statusResult.status(), statusResult.reason());
    }
}
