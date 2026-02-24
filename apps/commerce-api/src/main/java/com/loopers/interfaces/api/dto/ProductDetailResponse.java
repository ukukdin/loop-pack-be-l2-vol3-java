package com.loopers.interfaces.api.dto;

import com.loopers.application.ProductQueryUseCase;

public record ProductDetailResponse(
        Long id,
        Long brandId,
        String brandName,
        String name,
        int price,
        int stock,
        int likeCount,
        String description
) {
    public static ProductDetailResponse from(ProductQueryUseCase.ProductDetailInfo info) {
        return new ProductDetailResponse(
                info.id(),
                info.brandId(),
                info.brandName(),
                info.name(),
                info.price(),
                info.stock(),
                info.likeCount(),
                info.description()
        );
    }
}
