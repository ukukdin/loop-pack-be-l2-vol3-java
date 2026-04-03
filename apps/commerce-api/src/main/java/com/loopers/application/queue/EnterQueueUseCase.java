package com.loopers.application.queue;

import com.loopers.domain.model.user.UserId;

public interface EnterQueueUseCase {

    EnterQueueResult enter(UserId userId);

    record EnterQueueResult(
            long rank,
            long totalWaiting,
            long estimatedWaitSeconds
    ) {}
}
