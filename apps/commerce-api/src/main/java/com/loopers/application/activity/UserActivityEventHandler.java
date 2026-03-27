package com.loopers.application.activity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 유저 행동 로깅 핸들러.
 * UserActivityEvent만 수신하여 구조화된 로깅을 수행한다.
 * @Async로 처리하여 로깅 실패가 비즈니스 로직에 영향을 주지 않는다.
 */
@Component
public class UserActivityEventHandler {

    private static final Logger activityLog = LoggerFactory.getLogger("user-activity");

    @Async
    @EventListener
    public void handle(UserActivityEvent event) {
        activityLog.info("activity={} userId={} targetId={} metadata={} at={}",
                event.activityType(), event.userId().getValue(), event.targetId(),
                event.metadata(), event.occurredAt());
    }
}
