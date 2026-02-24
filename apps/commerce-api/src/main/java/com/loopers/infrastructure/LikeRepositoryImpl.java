package com.loopers.infrastructure;

import com.loopers.domain.model.like.Like;
import com.loopers.domain.model.user.UserId;
import com.loopers.domain.repository.LikeRepository;
import com.loopers.infrastructure.entity.LikeJpaEntity;
import com.loopers.infrastructure.repository.LikeJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class LikeRepositoryImpl implements LikeRepository {

    private final LikeJpaRepository likeJpaRepository;

    public LikeRepositoryImpl(LikeJpaRepository likeJpaRepository) {
        this.likeJpaRepository = likeJpaRepository;
    }

    @Override
    public Like save(Like like) {
        LikeJpaEntity entity = toEntity(like);
        LikeJpaEntity saved = likeJpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Like> findByUserIdAndProductId(UserId userId, Long productId) {
        return likeJpaRepository.findByUserIdAndProductId(userId.getValue(), productId)
                .map(this::toDomain);
    }

    @Override
    public void deleteByUserIdAndProductId(UserId userId, Long productId) {
        likeJpaRepository.deleteByUserIdAndProductId(userId.getValue(), productId);
    }

    @Override
    public boolean existsByUserIdAndProductId(UserId userId, Long productId) {
        return likeJpaRepository.existsByUserIdAndProductId(userId.getValue(), productId);
    }

    private LikeJpaEntity toEntity(Like like) {
        return new LikeJpaEntity(
                like.getId(),
                like.getUserId().getValue(),
                like.getProductId(),
                like.getCreatedAt()
        );
    }

    private Like toDomain(LikeJpaEntity entity) {
        return Like.reconstitute(
                entity.getId(),
                UserId.of(entity.getUserId()),
                entity.getProductId(),
                entity.getCreatedAt()
        );
    }
}
