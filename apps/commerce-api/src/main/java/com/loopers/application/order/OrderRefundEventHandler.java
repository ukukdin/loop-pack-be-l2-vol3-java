package com.loopers.application.order;

import com.loopers.application.payment.RefundPaymentUseCase;
import com.loopers.domain.model.order.event.OrderCancelledEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * PG 환불은 외부 API 호출이므로 트랜잭션 커밋 후 처리한다.
 * 환불 실패 시 주문 취소 자체는 이미 커밋되어 있고, 환불은 재시도 가능하다.
 */
@Component
public class OrderRefundEventHandler {

    private static final Logger log = LoggerFactory.getLogger(OrderRefundEventHandler.class);

    private final RefundPaymentUseCase refundPaymentUseCase;

    public OrderRefundEventHandler(RefundPaymentUseCase refundPaymentUseCase) {
        this.refundPaymentUseCase = refundPaymentUseCase;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(OrderCancelledEvent event) {
        if (!event.needsRefund()) {
            return;
        }
        try {
            refundPaymentUseCase.refundPayment(event.userId(), event.orderId());
        } catch (RuntimeException e) {
            log.error("PG 환불 실패 - orderId: {}, 수동 처리 필요, error: {}",
                    event.orderId(), e.getMessage(), e);
        }
    }
}
