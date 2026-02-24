package com.loopers.application.service;

import com.loopers.application.ProductQueryUseCase;
import com.loopers.domain.model.brand.Brand;
import com.loopers.domain.model.product.Product;
import com.loopers.domain.repository.BrandRepository;
import com.loopers.domain.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProductQueryService implements ProductQueryUseCase {

    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;

    public ProductQueryService(ProductRepository productRepository, BrandRepository brandRepository) {
        this.productRepository = productRepository;
        this.brandRepository = brandRepository;
    }

    @Override
    public ProductDetailInfo getProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

        Brand brand = brandRepository.findById(product.getBrandId())
                .orElseThrow(() -> new IllegalArgumentException("브랜드를 찾을 수 없습니다."));

        return new ProductDetailInfo(
                product.getId(),
                brand.getId(),
                brand.getName().getValue(),
                product.getName().getValue(),
                product.getPrice().getValue(),
                product.getStock().getValue(),
                product.getLikeCount(),
                product.getDescription()
        );
    }
}
