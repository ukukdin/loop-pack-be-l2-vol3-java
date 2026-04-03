package com.loopers.infrastructure.queue;

import com.loopers.domain.model.queue.QueueProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "queue")
public class QueuePropertiesConfig implements QueueProperties {

    private int batchSize = 18;
    private long tokenTtlSeconds = 300;
    private long throughputPerSecond = 175;
    private long maxQueueSize = 100_000;

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

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public void setTokenTtlSeconds(long tokenTtlSeconds) {
        this.tokenTtlSeconds = tokenTtlSeconds;
    }

    public void setThroughputPerSecond(long throughputPerSecond) {
        this.throughputPerSecond = throughputPerSecond;
    }

    public void setMaxQueueSize(long maxQueueSize) {
        this.maxQueueSize = maxQueueSize;
    }
}
