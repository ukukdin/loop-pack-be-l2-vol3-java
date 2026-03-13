package com.loopers.infrastructure.cache;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;

public class SafeCacheErrorHandler implements CacheErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(SafeCacheErrorHandler.class);

    private final MeterRegistry meterRegistry;

    public SafeCacheErrorHandler(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
        log.warn("Redis 캐시 조회 실패 - cache: {}", cache.getName(), exception);
        incrementErrorCounter(cache.getName(), "get");
    }

    @Override
    public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
        log.warn("Redis 캐시 저장 실패 - cache: {}", cache.getName(), exception);
        incrementErrorCounter(cache.getName(), "put");
    }

    @Override
    public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
        log.warn("Redis 캐시 삭제 실패 - cache: {}", cache.getName(), exception);
        incrementErrorCounter(cache.getName(), "evict");
    }

    @Override
    public void handleCacheClearError(RuntimeException exception, Cache cache) {
        log.warn("Redis 캐시 초기화 실패 - cache: {}", cache.getName(), exception);
        incrementErrorCounter(cache.getName(), "clear");
    }

    private void incrementErrorCounter(String cacheName, String operation) {
        Counter.builder("cache.errors")
                .tag("cache", cacheName)
                .tag("operation", operation)
                .register(meterRegistry)
                .increment();
    }
}
