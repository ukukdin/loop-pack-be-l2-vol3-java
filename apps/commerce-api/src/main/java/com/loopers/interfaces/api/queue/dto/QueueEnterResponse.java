package com.loopers.interfaces.api.queue.dto;

import com.loopers.application.queue.EnterQueueUseCase;

public record QueueEnterResponse(
        long rank,
        long totalWaiting,
        long estimatedWaitSeconds
) {
    public static QueueEnterResponse from(EnterQueueUseCase.EnterQueueResult result) {
        return new QueueEnterResponse(
                result.rank(),
                result.totalWaiting(),
                result.estimatedWaitSeconds()
        );
    }
}
