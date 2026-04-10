package com.loopers.interfaces.api.queue.dto;

import com.loopers.domain.model.queue.QueuePosition;

public record QueuePositionResponse(
        long rank,
        long totalWaiting,
        long estimatedWaitSeconds,
        String entryToken
) {
    public static QueuePositionResponse from(QueuePosition position) {
        return new QueuePositionResponse(
                position.getRank(),
                position.getTotalWaiting(),
                position.getEstimatedWaitSeconds(),
                position.getEntryToken()
        );
    }
}
