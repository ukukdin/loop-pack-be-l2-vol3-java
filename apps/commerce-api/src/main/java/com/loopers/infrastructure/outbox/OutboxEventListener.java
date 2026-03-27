package com.loopers.infrastructure.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.domain.model.like.event.ProductLikedEvent;
import com.loopers.domain.model.like.event.ProductUnlikedEvent;
import com.loopers.domain.model.order.event.OrderCreatedEvent;
import com.loopers.domain.model.order.event.PaymentCompletedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 도메인 이벤트를 수신하여 Outbox 테이블에 저장한다.
 * 같은 트랜잭션 내에서 실행되어 비즈니스 데이터와 Outbox 엔트리의 원자성을 보장한다.
 */
@Component
public class OutboxEventListener {

    private static final String CATALOG_EVENTS = "catalog-events";
    private static final String ORDER_EVENTS = "order-events";

    private final OutboxJpaRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public OutboxEventListener(OutboxJpaRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @EventListener
    public void on(ProductLikedEvent event) {
        save("PRODUCT", String.valueOf(event.productId()), "PRODUCT_LIKED",
                CATALOG_EVENTS, String.valueOf(event.productId()),
                Map.of("productId", event.productId(),
                        "userId", event.userId().getValue(),
                        "occurredAt", event.occurredAt().toString()));
    }

    @EventListener
    public void on(ProductUnlikedEvent event) {
        save("PRODUCT", String.valueOf(event.productId()), "PRODUCT_UNLIKED",
                CATALOG_EVENTS, String.valueOf(event.productId()),
                Map.of("productId", event.productId(),
                        "userId", event.userId().getValue(),
                        "occurredAt", event.occurredAt().toString()));
    }

    @EventListener
    public void on(OrderCreatedEvent event) {
        save("ORDER", String.valueOf(event.orderId()), "ORDER_CREATED",
                ORDER_EVENTS, String.valueOf(event.orderId()),
                Map.of("orderId", event.orderId(),
                        "userId", event.userId().getValue(),
                        "productIds", event.productIds(),
                        "occurredAt", event.occurredAt().toString()));
    }

    @EventListener
    public void on(PaymentCompletedEvent event) {
        save("ORDER", String.valueOf(event.orderId()), "PAYMENT_COMPLETED",
                ORDER_EVENTS, String.valueOf(event.orderId()),
                Map.of("orderId", event.orderId(),
                        "userId", event.userId().getValue(),
                        "paymentAmount", event.paymentAmount(),
                        "occurredAt", event.occurredAt().toString()));
    }

    private void save(String aggregateType, String aggregateId, String eventType,
                      String topic, String partitionKey, Map<String, Object> payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            outboxRepository.save(new OutboxJpaEntity(
                    aggregateType, aggregateId, eventType, topic, partitionKey, json));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Outbox 이벤트 직렬화 실패: " + eventType, e);
        }
    }
}
