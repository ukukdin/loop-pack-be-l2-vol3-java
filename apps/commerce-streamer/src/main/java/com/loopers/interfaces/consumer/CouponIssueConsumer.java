package com.loopers.interfaces.consumer;

import com.loopers.confg.kafka.KafkaConfig;
import com.loopers.infrastructure.coupon.CouponIssueRequestEntity;
import com.loopers.infrastructure.coupon.CouponIssueRequestRepository;
import com.loopers.infrastructure.coupon.UserCouponEntity;
import com.loopers.infrastructure.coupon.UserCouponEntityRepository;
import com.loopers.infrastructure.idempotency.EventHandledJpaEntity;
import com.loopers.infrastructure.idempotency.EventHandledJpaRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Component
public class CouponIssueConsumer {

    private static final Logger log = LoggerFactory.getLogger(CouponIssueConsumer.class);
    private static final String COUPON_COUNT_KEY_PREFIX = "coupon:issue:count:";

    private final StringRedisTemplate redisTemplate;
    private final UserCouponEntityRepository userCouponRepository;
    private final CouponIssueRequestRepository issueRequestRepository;
    private final EventHandledJpaRepository eventHandledRepository;

    public CouponIssueConsumer(StringRedisTemplate redisTemplate,
                               UserCouponEntityRepository userCouponRepository,
                               CouponIssueRequestRepository issueRequestRepository,
                               EventHandledJpaRepository eventHandledRepository) {
        this.redisTemplate = redisTemplate;
        this.userCouponRepository = userCouponRepository;
        this.issueRequestRepository = issueRequestRepository;
        this.eventHandledRepository = eventHandledRepository;
    }

    @KafkaListener(
            topics = "coupon-issue-requests",
            groupId = "streamer-coupon",
            containerFactory = KafkaConfig.BATCH_LISTENER
    )
    public void consume(List<ConsumerRecord<Object, Object>> records, Acknowledgment ack) {
        for (ConsumerRecord<Object, Object> record : records) {
            try {
                processRecord(record);
            } catch (RuntimeException e) {
                log.error("coupon-issue-requests 처리 실패 - offset: {}, error: {}",
                        record.offset(), e.getMessage(), e);
            }
        }
        ack.acknowledge();
    }

    @Transactional
    @SuppressWarnings("unchecked")
    protected void processRecord(ConsumerRecord<Object, Object> record) {
        Map<String, Object> message = (Map<String, Object>) record.value();

        Long eventId = toLong(message.get("eventId"));
        if (eventHandledRepository.existsById(eventId)) {
            return;
        }

        Long requestId = toLong(message.get("requestId"));
        Long couponId = toLong(message.get("couponId"));
        String userId = (String) message.get("userId");
        int maxIssuance = toInt(message.get("maxIssuance"));

        CouponIssueRequestEntity request = issueRequestRepository.findById(requestId)
                .orElse(null);
        if (request == null) {
            log.warn("발급 요청을 찾을 수 없음 - requestId: {}", requestId);
            eventHandledRepository.save(new EventHandledJpaEntity(eventId, "COUPON_ISSUE_REQUESTED"));
            return;
        }

        // 중복 발급 방지
        if (userCouponRepository.existsByCouponIdAndUserId(couponId, userId)) {
            request.markRejected("이미 발급된 쿠폰입니다.");
            eventHandledRepository.save(new EventHandledJpaEntity(eventId, "COUPON_ISSUE_REQUESTED"));
            return;
        }

        // Redis 원자적 수량 체크
        if (maxIssuance > 0) {
            String key = COUPON_COUNT_KEY_PREFIX + couponId;
            Long currentCount = redisTemplate.opsForValue().increment(key);
            if (currentCount != null && currentCount > maxIssuance) {
                redisTemplate.opsForValue().decrement(key);
                request.markRejected("발급 수량 초과");
                eventHandledRepository.save(new EventHandledJpaEntity(eventId, "COUPON_ISSUE_REQUESTED"));
                log.info("쿠폰 발급 거절 (수량 초과) - couponId: {}, userId: {}", couponId, userId);
                return;
            }
        }

        // 쿠폰 발급
        userCouponRepository.save(UserCouponEntity.issue(couponId, userId));
        request.markSuccess();
        eventHandledRepository.save(new EventHandledJpaEntity(eventId, "COUPON_ISSUE_REQUESTED"));
        log.info("쿠폰 발급 성공 - couponId: {}, userId: {}", couponId, userId);
    }

    private Long toLong(Object value) {
        if (value instanceof Number n) return n.longValue();
        return Long.valueOf(String.valueOf(value));
    }

    private int toInt(Object value) {
        if (value instanceof Number n) return n.intValue();
        return Integer.parseInt(String.valueOf(value));
    }
}
