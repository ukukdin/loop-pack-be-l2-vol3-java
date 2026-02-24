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

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProductServiceTest {

    private ProductRepository productRepository;
    private BrandRepository brandRepository;
    private ProductService service;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        brandRepository = mock(BrandRepository.class);
        service = new ProductService(productRepository, brandRepository);
    }

    @Nested
    @DisplayName("상품 생성")
    class CreateProduct {

        @Test
        @DisplayName("상품 생성 성공")
        void createProduct_success() {
            // given
            Brand brand = createBrand(1L);
            when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));

            // when & then
            assertThatNoException()
                    .isThrownBy(() -> service.createProduct(1L, "운동화", 50000, 100, "좋은 운동화"));

            verify(productRepository).save(any(Product.class));
        }

        @Test
        @DisplayName("존재하지 않는 브랜드로 생성시 예외")
        void createProduct_fail_brandNotFound() {
            // given
            when(brandRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> service.createProduct(999L, "운동화", 50000, 100, "좋은 운동화"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("존재하지 않는 브랜드");

            verify(productRepository, never()).save(any(Product.class));
        }

        @Test
        @DisplayName("삭제된 브랜드로 생성시 예외")
        void createProduct_fail_deletedBrand() {
            // given
            Brand deleted = Brand.reconstitute(1L, BrandName.of("삭제됨"), "설명",
                    LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now());
            when(brandRepository.findById(1L)).thenReturn(Optional.of(deleted));

            // when & then
            assertThatThrownBy(() -> service.createProduct(1L, "운동화", 50000, 100, "좋은 운동화"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("존재하지 않는 브랜드");
        }
    }

    @Nested
    @DisplayName("상품 수정")
    class UpdateProduct {

        @Test
        @DisplayName("상품 수정 성공")
        void updateProduct_success() {
            // given
            Product product = createProduct(1L, 1L);
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));

            // when & then
            assertThatNoException()
                    .isThrownBy(() -> service.updateProduct(1L, "새 이름", 60000, 200, "변경된 설명"));

            verify(productRepository).save(any(Product.class));
        }

        @Test
        @DisplayName("존재하지 않는 상품 수정시 예외")
        void updateProduct_fail_notFound() {
            // given
            when(productRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> service.updateProduct(999L, "새 이름", 60000, 200, "설명"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("상품을 찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("상품 삭제")
    class DeleteProduct {

        @Test
        @DisplayName("상품 삭제 성공")
        void deleteProduct_success() {
            // given
            Product product = createProduct(1L, 1L);
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));

            // when
            service.deleteProduct(1L);

            // then
            verify(productRepository).save(any(Product.class));
        }

        @Test
        @DisplayName("존재하지 않는 상품 삭제시 예외")
        void deleteProduct_fail_notFound() {
            // given
            when(productRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> service.deleteProduct(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("상품을 찾을 수 없습니다");
        }
    }

    private Brand createBrand(Long id) {
        return Brand.reconstitute(id, BrandName.of("나이키"), "스포츠 브랜드",
                LocalDateTime.now(), LocalDateTime.now(), null);
    }

    private Product createProduct(Long id, Long brandId) {
        return Product.reconstitute(id, brandId, ProductName.of("운동화"), Price.of(50000),
                Stock.of(100), 0, "좋은 운동화", LocalDateTime.now(), LocalDateTime.now(), null);
    }
}
