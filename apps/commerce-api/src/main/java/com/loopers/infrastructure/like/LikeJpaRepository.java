package com.loopers.infrastructure.like;

import com.loopers.infrastructure.like.LikeJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LikeJpaRepository extends JpaRepository<LikeJpaEntity, Long> {

    Optional<LikeJpaEntity> findByUserIdAndProductId(String userId, Long productId);

    boolean existsByUserIdAndProductId(String userId, Long productId);

    void deleteByUserIdAndProductId(String userId, Long productId);
}
