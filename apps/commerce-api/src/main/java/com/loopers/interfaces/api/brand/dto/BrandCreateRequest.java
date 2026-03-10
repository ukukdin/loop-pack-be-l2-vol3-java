package com.loopers.interfaces.api.brand.dto;

import jakarta.validation.constraints.NotBlank;

public record BrandCreateRequest(
        @NotBlank(message = "브랜드 이름은 필수입니다.")
        String name,
        String description
) {}
