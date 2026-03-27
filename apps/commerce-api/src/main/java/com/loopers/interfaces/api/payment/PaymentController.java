package com.loopers.interfaces.api.payment;

import com.loopers.application.order.CreateOrderUseCase;
import com.loopers.application.payment.PaymentCallbackUseCase;
import com.loopers.application.payment.PaymentQueryUseCase;
import com.loopers.application.payment.PaymentStatus;
import com.loopers.application.payment.RequestPaymentUseCase;
import com.loopers.domain.model.user.UserId;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final RequestPaymentUseCase requestPaymentUseCase;
    private final PaymentCallbackUseCase paymentCallbackUseCase;
    private final PaymentQueryUseCase paymentQueryUseCase;
    private final CreateOrderUseCase createOrderUseCase;

    public PaymentController(RequestPaymentUseCase requestPaymentUseCase,
                             PaymentCallbackUseCase paymentCallbackUseCase,
                             PaymentQueryUseCase paymentQueryUseCase,
                             CreateOrderUseCase createOrderUseCase) {
        this.requestPaymentUseCase = requestPaymentUseCase;
        this.paymentCallbackUseCase = paymentCallbackUseCase;
        this.paymentQueryUseCase = paymentQueryUseCase;
        this.createOrderUseCase = createOrderUseCase;
    }

    @PostMapping
    public ResponseEntity<PaymentStatusResponse> requestPayment(@RequestAttribute("authenticatedUserId") UserId userId,
                                                                @Valid @RequestBody PaymentRequest request) {
        // 주문의 결제 금액 조회
        CreateOrderUseCase.CreateOrderResult orderInfo =
                createOrderUseCase.getOrderPaymentInfo(userId, request.orderId());

        RequestPaymentUseCase.PaymentResult result;
        try {
            result = requestPaymentUseCase.requestPayment(userId, new RequestPaymentUseCase.PaymentCommand(
                    request.orderId(),
                    request.cardType(),
                    request.cardNo(),
                    orderInfo.paymentAmount()
            ));
        } catch (Exception e) {
            log.warn("PG 결제 요청 실패 - orderId: {}, 콜백 또는 상태 확인으로 복구 필요",
                    request.orderId(), e);
            return ResponseEntity.ok(new PaymentStatusResponse(null, PaymentStatus.PENDING.name(), "결제 처리 중"));
        }

        return ResponseEntity.ok(new PaymentStatusResponse(
                result.transactionKey(), result.status().name(), result.reason()));
    }

    @PostMapping("/callback")
    public ResponseEntity<Void> handleCallback(@RequestBody CallbackRequest request) {
        Long orderId = parseOrderId(request.orderId());
        PaymentStatus status = PaymentStatus.from(request.status());

        paymentCallbackUseCase.handleCallback(
                new PaymentCallbackUseCase.CallbackCommand(
                        request.transactionKey(),
                        orderId,
                        status,
                        request.reason()
                )
        );
        return ResponseEntity.ok().build();
    }

    private Long parseOrderId(String orderId) {
        if (orderId == null || orderId.isBlank()) {
            throw new CoreException(ErrorType.VALIDATION_ERROR, "주문 ID는 필수입니다.");
        }
        try {
            return Long.valueOf(orderId);
        } catch (NumberFormatException e) {
            throw new CoreException(ErrorType.VALIDATION_ERROR, "주문 ID는 숫자여야 합니다: " + orderId);
        }
    }

    @GetMapping("/status/{orderId}")
    public ResponseEntity<PaymentStatusResponse> getPaymentStatus(
            @RequestAttribute("authenticatedUserId") UserId userId,
            @PathVariable Long orderId) {
        PaymentQueryUseCase.PaymentStatusResult result =
                paymentQueryUseCase.getPaymentStatus(userId, orderId);
        return ResponseEntity.ok(new PaymentStatusResponse(
                result.transactionKey(),
                result.status().name(),
                result.reason()
        ));
    }

    record PaymentRequest(
            @NotNull(message = "주문 ID는 필수입니다.")
            Long orderId,
            @NotBlank(message = "카드 종류는 필수입니다.")
            String cardType,
            @NotBlank(message = "카드 번호는 필수입니다.")
            String cardNo
    ) {
        @Override
        public String toString() {
            String masked = (cardNo == null || cardNo.length() < 4)
                    ? "****"
                    : "****-****-****-" + cardNo.substring(cardNo.length() - 4);
            return "PaymentRequest[orderId=%s, cardType=%s, cardNo=%s]"
                    .formatted(orderId, cardType, masked);
        }
    }

    record CallbackRequest(
            String transactionKey,
            String orderId,
            String status,
            String reason
    ) {}

    record PaymentStatusResponse(
            String transactionKey,
            String status,
            String reason
    ) {}
}
