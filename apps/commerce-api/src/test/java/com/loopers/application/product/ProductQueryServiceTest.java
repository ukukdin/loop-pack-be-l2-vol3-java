package com.loopers.application.product;

import com.loopers.domain.model.brand.Brand;
import com.loopers.domain.model.brand.BrandName;
import com.loopers.domain.model.product.Price;
import com.loopers.domain.model.product.Product;
import com.loopers.domain.model.product.ProductName;
import com.loopers.domain.model.product.Stock;
import com.loopers.domain.repository.BrandRepository;
import com.loopers.domain.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ProductQueryServiceTest {

    private ProductRepository productRepository;
    private BrandRepository brandRepository;
    private ProductQueryService service;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        brandRepository = mock(BrandRepository.class);
        service = new ProductQueryService(productRepository, brandRepository);
    }

    @Nested
    @DisplayName("상품 단건 조회")
    class GetProduct {

        @Test
        @DisplayName("상품 상세 조회 성공")
        void getProduct_success() {
            // given
            Product product = createProduct(1L, 1L, "운동화", 50000);
            Brand brand = createBrand(1L, "나이키");

            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));

            // when
            var result = service.getProduct(1L);

            // then
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.brandName()).isEqualTo("나이키");
            assertThat(result.name()).isEqualTo("운동화");
            assertThat(result.price()).isEqualTo(50000);
        }

        @Test
        @DisplayName("삭제된 상품 조회시 예외")
        void getProduct_fail_deleted() {
            // given
            Product deleted = Product.reconstitute(1L, 1L, ProductName.of("삭제됨"), Price.of(10000),
                    Stock.of(0), 0, "설명", LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now());
            when(productRepository.findById(1L)).thenReturn(Optional.of(deleted));

            // when & then
            assertThatThrownBy(() -> service.getProduct(1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("상품을 찾을 수 없습니다");
        }

        @Test
        @DisplayName("존재하지 않는 상품 조회시 예외")
        void getProduct_fail_notFound() {
            // given
            when(productRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> service.getProduct(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("상품을 찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("상품 목록 조회")
    class GetProducts {

        @Test
        @DisplayName("전체 목록 조회 성공")
        void getProducts_success() {
            // given
            Product product1 = createProduct(1L, 1L, "운동화", 50000);
            Product product2 = createProduct(2L, 1L, "슬리퍼", 30000);
            Brand brand = createBrand(1L, "나이키");

            Page<Product> page = new PageImpl<>(List.of(product1, product2), PageRequest.of(0, 20), 2);
            when(productRepository.findAllByDeletedAtIsNull(eq(null), any())).thenReturn(page);
            when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));

            // when
            var result = service.getProducts(null, null, 0, 20);

            // then
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent().get(0).brandName()).isEqualTo("나이키");
        }

        @Test
        @DisplayName("브랜드 필터 조회")
        void getProducts_withBrandFilter() {
            // given
            Product product = createProduct(1L, 1L, "운동화", 50000);
            Brand brand = createBrand(1L, "나이키");

            Page<Product> page = new PageImpl<>(List.of(product), PageRequest.of(0, 20), 1);
            when(productRepository.findAllByDeletedAtIsNull(eq(1L), any())).thenReturn(page);
            when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));

            // when
            var result = service.getProducts(1L, null, 0, 20);

            // then
            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("가격 오름차순 정렬")
        void getProducts_sortByPriceAsc() {
            // given
            Page<Product> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
            when(productRepository.findAllByDeletedAtIsNull(eq(null), any())).thenReturn(page);

            // when
            service.getProducts(null, "price_asc", 0, 20);

            // then
            verify(productRepository).findAllByDeletedAtIsNull(eq(null), any());
        }
    }

    private Product createProduct(Long id, Long brandId, String name, int price) {
        return Product.reconstitute(id, brandId, ProductName.of(name), Price.of(price),
                Stock.of(100), 5, "설명", LocalDateTime.now(), LocalDateTime.now(), null);
    }

    private Brand createBrand(Long id, String name) {
        return Brand.reconstitute(id, BrandName.of(name), "설명",
                LocalDateTime.now(), LocalDateTime.now(), null);
    }
}
