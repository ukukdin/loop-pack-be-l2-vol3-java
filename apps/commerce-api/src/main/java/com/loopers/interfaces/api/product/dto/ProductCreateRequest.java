package com.loopers.interfaces.api.product.dto;

import com.loopers.application.product.CreateProductUseCase.ProductCreateCommand;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProductCreateRequest(
        @NotNull(message = "브랜드 ID는 필수입니다.")
        Long brandId,
        @NotBlank(message = "상품명은 필수입니다.")
        String name,
        @Min(value = 0, message = "가격은 0 이상이어야 합니다.")
        int price,
        Integer salePrice,
        @Min(value = 0, message = "재고는 0 이상이어야 합니다.")
        int stock,
        String description
) {
    public ProductCreateCommand toCommand() {
        return new ProductCreateCommand(brandId, name, price, salePrice, stock, description);
    }
}
