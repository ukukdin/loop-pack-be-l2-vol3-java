package com.loopers.application.product;

import com.loopers.domain.model.product.Price;
import com.loopers.domain.model.product.Product;
import com.loopers.domain.model.product.ProductName;
import com.loopers.domain.model.product.Stock;
import com.loopers.domain.repository.BrandRepository;
import com.loopers.domain.repository.ProductRepository;
import com.loopers.infrastructure.cache.CacheConfig;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class ProductService implements CreateProductUseCase, UpdateProductUseCase, DeleteProductUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final CacheManager cacheManager;

    public ProductService(ProductRepository productRepository, BrandRepository brandRepository,
                          CacheManager cacheManager) {
        this.productRepository = productRepository;
        this.brandRepository = brandRepository;
        this.cacheManager = cacheManager;
    }

    @Transactional
    @Override
    public void createProduct(ProductCreateCommand command) {
        brandRepository.findActiveById(command.brandId())
                .orElseThrow(() -> new CoreException(ErrorType.BRAND_NOT_FOUND));

        Price salePriceVo = command.salePrice() != null ? Price.of(command.salePrice()) : null;
        Product product = Product.create(command.brandId(), ProductName.of(command.name()),
                Price.of(command.price()), salePriceVo, Stock.of(command.stock()), command.description());
        productRepository.save(product);

        evictProductListAfterCommit();
    }

    @Transactional
    @Override
    public void updateProduct(ProductUpdateCommand command) {
        Product product = findProduct(command.productId());
        Price salePriceVo = command.salePrice() != null ? Price.of(command.salePrice()) : null;
        Product updated = product.update(ProductName.of(command.name()), Price.of(command.price()),
                salePriceVo, Stock.of(command.stock()), command.description());
        productRepository.save(updated);

        evictProductAfterCommit(command.productId());
        evictProductListAfterCommit();
    }

    @Transactional
    @Override
    public void deleteProduct(Long productId) {
        Product product = findProduct(productId);
        Product deleted = product.delete();
        productRepository.save(deleted);

        evictProductAfterCommit(productId);
        evictProductListAfterCommit();
    }

    private Product findProduct(Long productId) {
        return productRepository.findActiveById(productId)
                .orElseThrow(() -> new CoreException(ErrorType.PRODUCT_NOT_FOUND));
    }

    private void evictProductAfterCommit(Long productId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            Cache cache = cacheManager.getCache(CacheConfig.PRODUCT_DETAIL);
            if (cache != null) cache.evict(productId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    Cache cache = cacheManager.getCache(CacheConfig.PRODUCT_DETAIL);
                    if (cache != null) cache.evict(productId);
                } catch (RuntimeException e) {
                    log.warn("상품 캐시 무효화 실패 - productId: {}, error: {}", productId, e.getMessage());
                }
            }
        });
    }

    private void evictProductListAfterCommit() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            Cache cache = cacheManager.getCache(CacheConfig.PRODUCT_LIST);
            if (cache != null) cache.clear();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    Cache cache = cacheManager.getCache(CacheConfig.PRODUCT_LIST);
                    if (cache != null) cache.clear();
                } catch (RuntimeException e) {
                    log.warn("상품 목록 캐시 무효화 실패 - error: {}", e.getMessage());
                }
            }
        });
    }
}
