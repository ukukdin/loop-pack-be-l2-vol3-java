package com.loopers.application.like;

import com.loopers.domain.model.like.event.ProductLikedEvent;
import com.loopers.domain.model.like.event.ProductUnlikedEvent;
import com.loopers.domain.model.product.Product;
import com.loopers.domain.repository.ProductRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class LikeEventHandler {

    private final ProductRepository productRepository;

    public LikeEventHandler(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @EventListener
    public void handle(ProductLikedEvent event) {
        Product product = productRepository.findActiveByIdWithLock(event.productId())
                .orElseThrow(() -> new CoreException(ErrorType.PRODUCT_NOT_FOUND));
        Product updated = product.increaseLikeCount();
        productRepository.save(updated);
    }

    @EventListener
    public void handle(ProductUnlikedEvent event) {
        Product product = productRepository.findActiveByIdWithLock(event.productId())
                .orElseThrow(() -> new CoreException(ErrorType.PRODUCT_NOT_FOUND));
        Product updated = product.decreaseLikeCount();
        productRepository.save(updated);
    }
}
