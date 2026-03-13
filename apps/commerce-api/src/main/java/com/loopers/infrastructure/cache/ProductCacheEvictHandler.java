package com.loopers.infrastructure.cache;

import com.loopers.domain.model.brand.event.BrandDeletedEvent;
import com.loopers.domain.model.like.event.ProductLikedEvent;
import com.loopers.domain.model.like.event.ProductUnlikedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ProductCacheEvictHandler {

    private static final Logger log = LoggerFactory.getLogger(ProductCacheEvictHandler.class);

    private final CacheManager cacheManager;

    public ProductCacheEvictHandler(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleLiked(ProductLikedEvent event) {
        evictProductDetail(event.productId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUnliked(ProductUnlikedEvent event) {
        evictProductDetail(event.productId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBrandDeleted(BrandDeletedEvent event) {
        evictAllProductCaches();
        evictBrandList();
    }

    private void evictProductDetail(Long productId) {
        Cache cache = cacheManager.getCache(CacheConfig.PRODUCT_DETAIL);
        if (cache != null) {
            cache.evict(productId);
            log.debug("캐시 무효화 [AFTER_COMMIT] - product::{}", productId);
        }
    }

    private void evictAllProductCaches() {
        Cache detailCache = cacheManager.getCache(CacheConfig.PRODUCT_DETAIL);
        Cache listCache = cacheManager.getCache(CacheConfig.PRODUCT_LIST);
        if (detailCache != null) detailCache.clear();
        if (listCache != null) listCache.clear();
        log.debug("캐시 무효화 [AFTER_COMMIT] - product::*, products::*");
    }

    private void evictBrandList() {
        Cache cache = cacheManager.getCache(CacheConfig.BRAND_LIST);
        if (cache != null) {
            cache.clear();
            log.debug("캐시 무효화 [AFTER_COMMIT] - brands::*");
        }
    }
}
