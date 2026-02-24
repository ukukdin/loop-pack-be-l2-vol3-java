package com.loopers.application.like;

import com.loopers.domain.model.common.DomainEventPublisher;
import com.loopers.domain.model.like.Like;
import com.loopers.domain.model.product.*;
import com.loopers.domain.model.user.UserId;
import com.loopers.domain.repository.LikeRepository;
import com.loopers.domain.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LikeServiceTest {

    private LikeRepository likeRepository;
    private ProductRepository productRepository;
    private DomainEventPublisher domainEventPublisher;
    private LikeService service;

    @BeforeEach
    void setUp() {
        likeRepository = mock(LikeRepository.class);
        productRepository = mock(ProductRepository.class);
        domainEventPublisher = mock(DomainEventPublisher.class);
        service = new LikeService(likeRepository, productRepository, domainEventPublisher);
    }

    @Nested
    @DisplayName("좋아요")
    class LikeTest {

        @Test
        @DisplayName("좋아요 성공")
        void like_success() {
            // given
            UserId userId = UserId.of("test1234");
            Product product = createProduct(1L, 0);

            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(likeRepository.existsByUserIdAndProductId(userId, 1L)).thenReturn(false);

            // when
            service.like(userId, 1L);

            // then
            verify(likeRepository).save(any(Like.class));
            verify(domainEventPublisher).publishEvents(any(Like.class));
        }

        @Test
        @DisplayName("이미 좋아요한 경우 무시 (멱등성)")
        void like_alreadyLiked_ignored() {
            // given
            UserId userId = UserId.of("test1234");
            Product product = createProduct(1L, 1);

            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(likeRepository.existsByUserIdAndProductId(userId, 1L)).thenReturn(true);

            // when
            service.like(userId, 1L);

            // then
            verify(likeRepository, never()).save(any(Like.class));
            verify(domainEventPublisher, never()).publishEvents(any());
        }

        @Test
        @DisplayName("존재하지 않는 상품에 좋아요시 예외")
        void like_fail_productNotFound() {
            // given
            UserId userId = UserId.of("test1234");
            when(productRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> service.like(userId, 999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("상품을 찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("좋아요 취소")
    class UnlikeTest {

        @Test
        @DisplayName("좋아요 취소 성공")
        void unlike_success() {
            // given
            UserId userId = UserId.of("test1234");
            Product product = createProduct(1L, 1);
            Like like = Like.reconstitute(1L, userId, 1L, LocalDateTime.now());

            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(likeRepository.findByUserIdAndProductId(userId, 1L)).thenReturn(Optional.of(like));

            // when
            service.unlike(userId, 1L);

            // then
            verify(domainEventPublisher).publishEvents(any(Like.class));
            verify(likeRepository).deleteByUserIdAndProductId(userId, 1L);
        }

        @Test
        @DisplayName("좋아요하지 않은 경우 무시")
        void unlike_notLiked_ignored() {
            // given
            UserId userId = UserId.of("test1234");
            Product product = createProduct(1L, 0);

            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(likeRepository.findByUserIdAndProductId(userId, 1L)).thenReturn(Optional.empty());

            // when
            service.unlike(userId, 1L);

            // then
            verify(likeRepository, never()).deleteByUserIdAndProductId(any(), any());
            verify(domainEventPublisher, never()).publishEvents(any());
        }
    }

    @Nested
    @DisplayName("좋아요 목록 조회")
    class GetMyLikes {

        @Test
        @DisplayName("좋아요 목록 조회 성공")
        void getMyLikes_success() {
            // given
            UserId userId = UserId.of("test1234");
            Like like1 = Like.reconstitute(1L, userId, 1L, LocalDateTime.now());
            Like like2 = Like.reconstitute(2L, userId, 2L, LocalDateTime.now());
            Product product1 = createProduct(1L, 5);
            Product product2 = createProduct(2L, 3);

            when(likeRepository.findAllByUserId(userId)).thenReturn(List.of(like1, like2));
            when(productRepository.findById(1L)).thenReturn(Optional.of(product1));
            when(productRepository.findById(2L)).thenReturn(Optional.of(product2));

            // when
            var result = service.getMyLikes(userId);

            // then
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("삭제된 상품은 목록에서 제외")
        void getMyLikes_excludeDeletedProducts() {
            // given
            UserId userId = UserId.of("test1234");
            Like like1 = Like.reconstitute(1L, userId, 1L, LocalDateTime.now());
            Like like2 = Like.reconstitute(2L, userId, 2L, LocalDateTime.now());

            Product activeProduct = createProduct(1L, 5);
            Product deletedProduct = Product.reconstitute(2L, 1L, ProductName.of("삭제됨"),
                    Price.of(10000), Stock.of(0), LikeCount.zero(), Description.ofNullable("설명"),
                    LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now());

            when(likeRepository.findAllByUserId(userId)).thenReturn(List.of(like1, like2));
            when(productRepository.findById(1L)).thenReturn(Optional.of(activeProduct));
            when(productRepository.findById(2L)).thenReturn(Optional.of(deletedProduct));

            // when
            var result = service.getMyLikes(userId);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).productId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("좋아요 목록이 비어있는 경우")
        void getMyLikes_empty() {
            // given
            UserId userId = UserId.of("test1234");
            when(likeRepository.findAllByUserId(userId)).thenReturn(List.of());

            // when
            var result = service.getMyLikes(userId);

            // then
            assertThat(result).isEmpty();
        }
    }

    private Product createProduct(Long id, int likeCount) {
        return Product.reconstitute(id, 1L, ProductName.of("상품" + id), Price.of(10000),
                Stock.of(100), LikeCount.of(likeCount), Description.ofNullable("설명"),
                LocalDateTime.now(), LocalDateTime.now(), null);
    }
}
