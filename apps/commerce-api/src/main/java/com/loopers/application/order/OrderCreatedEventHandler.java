package com.loopers.application.order;

import com.loopers.domain.model.order.event.OrderCreatedEvent;
import com.loopers.infrastructure.cache.CacheConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OrderCreatedEventHandler {

    private static final Logger log = LoggerFactory.getLogger(OrderCreatedEventHandler.class);

    private final CacheManager cacheManager;

    public OrderCreatedEventHandler(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(OrderCreatedEvent event) {
        evictProductCache(event);
    }

    private void evictProductCache(OrderCreatedEvent event) {
        try {
            Cache cache = cacheManager.getCache(CacheConfig.PRODUCT_DETAIL);
            if (cache != null) {
                event.productIds().forEach(cache::evict);
            }
        } catch (RuntimeException e) {
            log.warn("주문 후 캐시 무효화 실패 - orderId: {}, productIds: {}, error: {}",
                    event.orderId(), event.productIds(), e.getMessage());
        }
    }
}
