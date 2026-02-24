package com.loopers.interfaces.api.product.dto;

public record ProductUpdateRequest(
        String name,
        int price,
        int stock,
        String description
) {}
