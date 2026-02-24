package com.loopers.interfaces.api.dto;

public record ProductUpdateRequest(
        String name,
        int price,
        int stock,
        String description
) {}
