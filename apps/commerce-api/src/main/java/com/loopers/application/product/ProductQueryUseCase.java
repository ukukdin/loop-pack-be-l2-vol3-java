package com.loopers.application.product;

import org.springframework.data.domain.Page;

public interface ProductQueryUseCase {

    ProductDetailInfo getProduct(Long productId);

    Page<ProductSummaryInfo> getProducts(Long brandId, String sort, int page, int size);

    record ProductDetailInfo(
            Long id,
            Long brandId,
            String brandName,
            String name,
            int price,
            int stock,
            int likeCount,
            String description
    ) {}

    record ProductSummaryInfo(
            Long id,
            Long brandId,
            String brandName,
            String name,
            int price,
            int likeCount
    ) {}
}
