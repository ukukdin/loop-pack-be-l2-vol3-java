package com.loopers.domain.model.queue;

public interface QueueProperties {

    int getBatchSize();

    long getTokenTtlSeconds();

    long getThroughputPerSecond();

    long getMaxQueueSize();

    long getSchedulerIntervalMs();
}
