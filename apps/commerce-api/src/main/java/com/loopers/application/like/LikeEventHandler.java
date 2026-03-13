package com.loopers.application.like;

import com.loopers.domain.model.like.event.ProductLikedEvent;
import com.loopers.domain.model.like.event.ProductUnlikedEvent;
import com.loopers.domain.repository.ProductRepository;
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
        productRepository.incrementLikeCount(event.productId());
    }

    @EventListener
    public void handle(ProductUnlikedEvent event) {
        productRepository.decrementLikeCount(event.productId());
    }
}
