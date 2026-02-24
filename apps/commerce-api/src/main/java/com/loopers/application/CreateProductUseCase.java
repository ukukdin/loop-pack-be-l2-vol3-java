package com.loopers.application;

public interface CreateProductUseCase {

    void createProduct(Long brandId, String name, int price, int stock, String description);
}
