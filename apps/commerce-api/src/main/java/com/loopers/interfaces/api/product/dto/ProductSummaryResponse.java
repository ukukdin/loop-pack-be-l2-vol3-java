package com.loopers.interfaces.api.product.dto;

import com.loopers.application.product.ProductQueryUseCase;

public record ProductSummaryResponse(
        Long id,
        Long brandId,
        String brandName,
        String name,
        int price,
        int likeCount
) {
    public static ProductSummaryResponse from(ProductQueryUseCase.ProductSummaryInfo info) {
        return new ProductSummaryResponse(
                info.id(),
                info.brandId(),
                info.brandName(),
                info.name(),
                info.price(),
                info.likeCount()
        );
    }
}
