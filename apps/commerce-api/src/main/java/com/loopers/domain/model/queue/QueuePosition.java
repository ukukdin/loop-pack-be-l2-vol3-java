package com.loopers.domain.model.queue;

import lombok.Getter;

@Getter
public class QueuePosition {

    private final long rank;
    private final long totalWaiting;
    private final long estimatedWaitSeconds;
    private final String entryToken;

    private QueuePosition(long rank, long totalWaiting, long estimatedWaitSeconds, String entryToken) {
        this.rank = rank;
        this.totalWaiting = totalWaiting;
        this.estimatedWaitSeconds = estimatedWaitSeconds;
        this.entryToken = entryToken;
    }

    public static QueuePosition waiting(long rank, long totalWaiting, long throughputPerSecond) {
        long estimatedWait = (throughputPerSecond > 0) ? rank / throughputPerSecond : 0;
        return new QueuePosition(rank, totalWaiting, estimatedWait, null);
    }

    public static QueuePosition ready(String entryToken) {
        return new QueuePosition(0, 0, 0, entryToken);
    }

    public boolean isReady() {
        return entryToken != null;
    }
}
