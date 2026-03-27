package com.loopers.application.activity;

import com.loopers.domain.model.like.event.ProductLikedEvent;
import com.loopers.domain.model.like.event.ProductUnlikedEvent;
import com.loopers.domain.model.order.event.OrderCreatedEvent;
import com.loopers.domain.model.order.event.PaymentCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 유저 행동 로깅 핸들러.
 * 모든 리스너는 AFTER_COMMIT + @Async로 처리하여
 * 로깅 실패가 비즈니스 로직에 영향을 주지 않는다.
 */
@Component
public class UserActivityEventHandler {

    private static final Logger activityLog = LoggerFactory.getLogger("user-activity");

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCreated(OrderCreatedEvent event) {
        activityLog.info("activity=ORDER_CREATED userId={} orderId={} productIds={} at={}",
                event.userId().getValue(), event.orderId(), event.productIds(), event.occurredAt());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        activityLog.info("activity=PAYMENT_COMPLETED userId={} orderId={} amount={} at={}",
                event.userId().getValue(), event.orderId(), event.paymentAmount(), event.occurredAt());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProductLiked(ProductLikedEvent event) {
        activityLog.info("activity=PRODUCT_LIKED userId={} productId={} at={}",
                event.userId().getValue(), event.productId(), event.occurredAt());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProductUnliked(ProductUnlikedEvent event) {
        activityLog.info("activity=PRODUCT_UNLIKED userId={} productId={} at={}",
                event.userId().getValue(), event.productId(), event.occurredAt());
    }
}
