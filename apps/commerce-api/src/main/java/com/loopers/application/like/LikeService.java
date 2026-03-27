package com.loopers.application.like;

import com.loopers.domain.model.common.DomainEventPublisher;
import com.loopers.domain.model.like.Like;
import com.loopers.domain.model.user.UserId;
import com.loopers.domain.repository.LikeRepository;
import com.loopers.domain.repository.ProductRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LikeService implements LikeUseCase, UnlikeUseCase {

    private final LikeRepository likeRepository;
    private final ProductRepository productRepository;
    private final DomainEventPublisher domainEventPublisher;

    public LikeService(LikeRepository likeRepository, ProductRepository productRepository,
                       DomainEventPublisher domainEventPublisher) {
        this.likeRepository = likeRepository;
        this.productRepository = productRepository;
        this.domainEventPublisher = domainEventPublisher;
    }

    @Transactional
    @Override
    public void like(UserId userId, Long productId) {
        validateProductExists(productId);

        if (likeRepository.existsByUserIdAndProductId(userId, productId)) {
            return;
        }

        Like like = Like.create(userId, productId);
        likeRepository.save(like);
        domainEventPublisher.publishEvents(like);
    }

    @Transactional
    @Override
    public void unlike(UserId userId, Long productId) {
        validateProductExists(productId);

        likeRepository.findByUserIdAndProductId(userId, productId)
                .ifPresent(like -> {
                    Like unliked = like.markUnliked();
                    domainEventPublisher.publishEvents(unliked);
                    likeRepository.deleteByUserIdAndProductId(userId, productId);
                });
    }

    private void validateProductExists(Long productId) {
        productRepository.findActiveById(productId)
                .orElseThrow(() -> new CoreException(ErrorType.PRODUCT_NOT_FOUND));
    }
}
