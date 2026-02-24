package com.loopers.application;

public interface ProductQueryUseCase {

    ProductDetailInfo getProduct(Long productId);

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
}
