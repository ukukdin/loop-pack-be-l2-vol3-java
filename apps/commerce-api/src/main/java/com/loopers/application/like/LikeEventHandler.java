package com.loopers.application.like;

import com.loopers.domain.model.like.event.ProductLikedEvent;
import com.loopers.domain.model.like.event.ProductUnlikedEvent;
import com.loopers.domain.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 좋아요 집계 핸들러.
 * AFTER_COMMIT으로 처리하여 집계 실패와 무관하게 좋아요 자체는 성공한다.
 * 각 메서드에 @Transactional을 선언하여 독립된 트랜잭션에서 count를 업데이트한다.
 */
@Component
public class LikeEventHandler {

    private static final Logger log = LoggerFactory.getLogger(LikeEventHandler.class);

    private final ProductRepository productRepository;

    public LikeEventHandler(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ProductLikedEvent event) {
        try {
            productRepository.incrementLikeCount(event.productId());
        } catch (RuntimeException e) {
            log.warn("좋아요 집계 실패 - productId: {}, error: {}", event.productId(), e.getMessage());
        }
    }

    @Transactional
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ProductUnlikedEvent event) {
        try {
            productRepository.decrementLikeCount(event.productId());
        } catch (RuntimeException e) {
            log.warn("좋아요 취소 집계 실패 - productId: {}, error: {}", event.productId(), e.getMessage());
        }
    }
}
