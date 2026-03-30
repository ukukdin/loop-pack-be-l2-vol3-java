package com.loopers.infrastructure.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Outbox 테이블의 PENDING 이벤트를 폴링하여 Kafka로 발행한다.
 * 발행 성공 시 PUBLISHED로 상태를 변경한다.
 * 실패 시 다음 폴링에서 자동 재시도 (At Least Once 보장).
 */
@Component
public class OutboxEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxEventPublisher.class);

    private final OutboxJpaRepository outboxRepository;
    private final KafkaTemplate<Object, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public OutboxEventPublisher(OutboxJpaRepository outboxRepository,
                                KafkaTemplate<Object, Object> kafkaTemplate,
                                ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxJpaEntity> pendingEvents =
                outboxRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxJpaEntity.STATUS_PENDING);

        for (OutboxJpaEntity event : pendingEvents) {
            try {
                Map<String, Object> message = buildMessage(event);
                kafkaTemplate.send(event.getTopic(), event.getPartitionKey(), message).get();
                event.markPublished();
            } catch (Exception e) {
                log.warn("Outbox 이벤트 발행 실패 - eventId: {}, topic: {}, error: {}",
                        event.getId(), event.getTopic(), e.getMessage());
                continue;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildMessage(OutboxJpaEntity event) throws JsonProcessingException {
        Map<String, Object> payload = objectMapper.readValue(event.getPayload(), Map.class);
        Map<String, Object> message = new HashMap<>(payload);
        message.put("eventId", event.getId());
        message.put("eventType", event.getEventType());
        return message;
    }
}
