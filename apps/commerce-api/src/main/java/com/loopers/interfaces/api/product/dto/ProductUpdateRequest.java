package com.loopers.interfaces.api.product.dto;

import com.loopers.application.product.UpdateProductUseCase.ProductUpdateCommand;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ProductUpdateRequest(
        @NotBlank(message = "상품명은 필수입니다.")
        String name,
        @Min(value = 0, message = "가격은 0 이상이어야 합니다.")
        int price,
        Integer salePrice,
        @Min(value = 0, message = "재고는 0 이상이어야 합니다.")
        int stock,
        String description
) {
    public ProductUpdateCommand toCommand(Long productId) {
        return new ProductUpdateCommand(productId, name, price, salePrice, stock, description);
    }
}
