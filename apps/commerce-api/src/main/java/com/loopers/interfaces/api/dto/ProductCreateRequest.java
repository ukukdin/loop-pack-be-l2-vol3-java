package com.loopers.interfaces.api.dto;

public record ProductCreateRequest(
        Long brandId,
        String name,
        int price,
        int stock,
        String description
) {}
