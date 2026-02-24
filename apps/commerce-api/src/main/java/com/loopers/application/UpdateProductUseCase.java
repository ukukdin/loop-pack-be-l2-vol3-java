package com.loopers.application;

public interface UpdateProductUseCase {

    void updateProduct(Long productId, String name, int price, int stock, String description);
}
