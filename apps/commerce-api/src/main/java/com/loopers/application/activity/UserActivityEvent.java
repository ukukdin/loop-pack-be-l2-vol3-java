package com.loopers.application.activity;

import com.loopers.domain.model.common.DomainEvent;
import com.loopers.domain.model.user.UserId;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 유저 행동 로깅 이벤트.
 * 비즈니스 도메인 이벤트와 분리된 순수 부가 로직용 이벤트이다.
 *
 * @param userId       행동 주체
 * @param activityType 행동 유형
 * @param targetId     대상 ID (orderId, productId 등)
 * @param metadata     부가 정보
 * @param occurredAt   발생 시각
 */
public record UserActivityEvent(
        UserId userId,
        ActivityType activityType,
        String targetId,
        Map<String, Object> metadata,
        LocalDateTime occurredAt
) implements DomainEvent {

    public enum ActivityType {
        ORDER_CREATED,
        PAYMENT_COMPLETED,
        PRODUCT_LIKED,
        PRODUCT_UNLIKED
    }

    public UserActivityEvent(UserId userId, ActivityType activityType, String targetId, Map<String, Object> metadata) {
        this(userId, activityType, targetId, metadata, LocalDateTime.now());
    }
}