package com.loopers.application.activity;

import com.loopers.domain.model.like.event.ProductLikedEvent;
import com.loopers.domain.model.like.event.ProductUnlikedEvent;
import com.loopers.domain.model.order.event.OrderCreatedEvent;
import com.loopers.domain.model.order.event.PaymentCompletedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

/**
 * 도메인 이벤트를 수신하여 UserActivityEvent로 변환 발행한다.
 * 도메인 이벤트 → UserActivityEvent 변환을 한 곳에서 관리하여
 * 각 서비스가 UserActivityEvent를 직접 알 필요가 없도록 한다.
 */
@Component
public class UserActivityEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public UserActivityEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCreated(OrderCreatedEvent event) {
        eventPublisher.publishEvent(new UserActivityEvent(
                event.userId(),
                UserActivityEvent.ActivityType.ORDER_CREATED,
                String.valueOf(event.orderId()),
                Map.of("productIds", event.productIds())
        ));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        eventPublisher.publishEvent(new UserActivityEvent(
                event.userId(),
                UserActivityEvent.ActivityType.PAYMENT_COMPLETED,
                String.valueOf(event.orderId()),
                Map.of("paymentAmount", event.paymentAmount())
        ));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProductLiked(ProductLikedEvent event) {
        eventPublisher.publishEvent(new UserActivityEvent(
                event.userId(),
                UserActivityEvent.ActivityType.PRODUCT_LIKED,
                String.valueOf(event.productId()),
                Map.of()
        ));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProductUnliked(ProductUnlikedEvent event) {
        eventPublisher.publishEvent(new UserActivityEvent(
                event.userId(),
                UserActivityEvent.ActivityType.PRODUCT_UNLIKED,
                String.valueOf(event.productId()),
                Map.of()
        ));
    }
}
