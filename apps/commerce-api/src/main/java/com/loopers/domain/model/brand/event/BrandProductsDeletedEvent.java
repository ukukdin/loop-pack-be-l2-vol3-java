package com.loopers.domain.model.brand.event;

import java.util.List;

public record BrandProductsDeletedEvent(
        Long brandId,
        List<Long> deletedProductIds
) {
}
