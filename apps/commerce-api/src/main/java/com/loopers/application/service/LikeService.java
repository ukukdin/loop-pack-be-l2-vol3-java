package com.loopers.application.service;

import com.loopers.application.LikeUseCase;
import com.loopers.application.UnlikeUseCase;
import com.loopers.domain.model.like.Like;
import com.loopers.domain.model.product.Product;
import com.loopers.domain.model.user.UserId;
import com.loopers.domain.repository.LikeRepository;
import com.loopers.domain.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class LikeService implements LikeUseCase, UnlikeUseCase {

    private final LikeRepository likeRepository;
    private final ProductRepository productRepository;

    public LikeService(LikeRepository likeRepository, ProductRepository productRepository) {
        this.likeRepository = likeRepository;
        this.productRepository = productRepository;
    }

    @Override
    public void like(UserId userId, Long productId) {
        Product product = findProduct(productId);

        if (likeRepository.existsByUserIdAndProductId(userId, productId)) {
            return; // Idempotency — 이미 좋아요한 경우 무시
        }

        Like like = Like.create(userId, productId);
        likeRepository.save(like);

        Product updated = product.increaseLikeCount();
        productRepository.save(updated);
    }

    @Override
    public void unlike(UserId userId, Long productId) {
        Product product = findProduct(productId);

        if (!likeRepository.existsByUserIdAndProductId(userId, productId)) {
            return; // 좋아요하지 않은 경우 무시
        }

        likeRepository.deleteByUserIdAndProductId(userId, productId);

        Product updated = product.decreaseLikeCount();
        productRepository.save(updated);
    }

    private Product findProduct(Long productId) {
        return productRepository.findById(productId)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));
    }
}
