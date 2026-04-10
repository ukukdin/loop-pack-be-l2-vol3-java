package com.loopers.infrastructure.queue;

import com.loopers.domain.model.queue.QueueProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "queue")
public class QueuePropertiesConfig implements QueueProperties {

    private int batchSize = 18;
    private long tokenTtlSeconds = 300;
    private long throughputPerSecond = 175;
    private long maxQueueSize = 100_000;
    private long schedulerIntervalMs = 100;

    @Override
    public int getBatchSize() {
        return batchSize;
    }

    @Override
    public long getTokenTtlSeconds() {
        return tokenTtlSeconds;
    }

    @Override
    public long getThroughputPerSecond() {
        return throughputPerSecond;
    }

    @Override
    public long getMaxQueueSize() {
        return maxQueueSize;
    }

    @Override
    public long getSchedulerIntervalMs() {
        return schedulerIntervalMs;
    }

    public void setBatchSize(int batchSize) {
        if (batchSize < 1) {
            throw new IllegalArgumentException("batchSize must be >= 1");
        }
        this.batchSize = batchSize;
    }

    public void setTokenTtlSeconds(long tokenTtlSeconds) {
        if (tokenTtlSeconds < 1) {
            throw new IllegalArgumentException("tokenTtlSeconds must be >= 1");
        }
        this.tokenTtlSeconds = tokenTtlSeconds;
    }

    public void setThroughputPerSecond(long throughputPerSecond) {
        if (throughputPerSecond < 1) {
            throw new IllegalArgumentException("throughputPerSecond must be >= 1");
        }
        this.throughputPerSecond = throughputPerSecond;
    }

    public void setMaxQueueSize(long maxQueueSize) {
        if (maxQueueSize < 0) {
            throw new IllegalArgumentException("maxQueueSize must be >= 0");
        }
        this.maxQueueSize = maxQueueSize;
    }

    public void setSchedulerIntervalMs(long schedulerIntervalMs) {
        if (schedulerIntervalMs < 1) {
            throw new IllegalArgumentException("schedulerIntervalMs must be >= 1");
        }
        this.schedulerIntervalMs = schedulerIntervalMs;
    }
}
