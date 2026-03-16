package com.loopers.infrastructure.cache;

import com.loopers.domain.model.brand.event.BrandProductsDeletedEvent;
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
    public void handleBrandProductsDeleted(BrandProductsDeletedEvent event) {
        event.deletedProductIds().forEach(this::evictProductDetail);
        evictProductListCache();
        evictBrandList();
    }

    private void evictProductDetail(Long productId) {
        try {
            Cache cache = cacheManager.getCache(CacheConfig.PRODUCT_DETAIL);
            if (cache != null) {
                cache.evict(productId);
                log.debug("캐시 무효화 [AFTER_COMMIT] - product::{}", productId);
            }
        } catch (RuntimeException e) {
            log.warn("캐시 무효화 실패 [AFTER_COMMIT] - product::{}, error: {}", productId, e.getMessage());
        }
    }

    private void evictProductListCache() {
        try {
            Cache listCache = cacheManager.getCache(CacheConfig.PRODUCT_LIST);
            if (listCache != null) listCache.clear();
            log.debug("캐시 무효화 [AFTER_COMMIT] - products::*");
        } catch (RuntimeException e) {
            log.warn("캐시 무효화 실패 [AFTER_COMMIT] - products, error: {}", e.getMessage());
        }
    }

    private void evictBrandList() {
        try {
            Cache cache = cacheManager.getCache(CacheConfig.BRAND_LIST);
            if (cache != null) {
                cache.clear();
                log.debug("캐시 무효화 [AFTER_COMMIT] - brands::*");
            }
        } catch (RuntimeException e) {
            log.warn("캐시 무효화 실패 [AFTER_COMMIT] - brands, error: {}", e.getMessage());
        }
    }
}
