package com.loopers.interfaces.api.dto;

import com.loopers.application.BrandQueryUseCase;

public record BrandResponse(
        Long id,
        String name,
        String description
) {
    public static BrandResponse from(BrandQueryUseCase.BrandInfo brandInfo) {
        return new BrandResponse(
                brandInfo.id(),
                brandInfo.name(),
                brandInfo.description()
        );
    }
}
