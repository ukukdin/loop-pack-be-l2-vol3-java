package com.loopers.application.like;

import com.loopers.domain.model.common.DomainEventPublisher;
import com.loopers.domain.model.like.Like;
import com.loopers.domain.model.product.Product;
import com.loopers.domain.model.user.UserId;
import com.loopers.domain.repository.LikeRepository;
import com.loopers.domain.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class LikeService implements LikeUseCase, UnlikeUseCase, LikeQueryUseCase {

    private final LikeRepository likeRepository;
    private final ProductRepository productRepository;
    private final DomainEventPublisher domainEventPublisher;

    public LikeService(LikeRepository likeRepository, ProductRepository productRepository,
                       DomainEventPublisher domainEventPublisher) {
        this.likeRepository = likeRepository;
        this.productRepository = productRepository;
        this.domainEventPublisher = domainEventPublisher;
    }

    @Override
    public void like(UserId userId, Long productId) {
        findProduct(productId);

        if (likeRepository.existsByUserIdAndProductId(userId, productId)) {
            return;
        }

        Like like = Like.create(userId, productId);
        likeRepository.save(like);
        domainEventPublisher.publishEvents(like);
    }

    @Override
    public void unlike(UserId userId, Long productId) {
        findProduct(productId);

        likeRepository.findByUserIdAndProductId(userId, productId)
                .ifPresent(like -> {
                    like.markUnliked();
                    domainEventPublisher.publishEvents(like);
                    likeRepository.deleteByUserIdAndProductId(userId, productId);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public List<LikeInfo> getMyLikes(UserId userId) {
        List<Like> likes = likeRepository.findAllByUserId(userId);

        return likes.stream()
                .flatMap(like -> productRepository.findById(like.getProductId())
                        .filter(p -> !p.isDeleted())
                        .map(product -> new LikeInfo(
                                product.getId(),
                                product.getName().getValue(),
                                product.getPrice().getValue(),
                                like.getCreatedAt()
                        ))
                        .stream())
                .toList();
    }

    private Product findProduct(Long productId) {
        return productRepository.findById(productId)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));
    }
}
