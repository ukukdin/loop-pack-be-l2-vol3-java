package com.loopers.domain.repository;

import com.loopers.domain.model.product.Product;

import java.util.Optional;

public interface ProductRepository {

    Product save(Product product);

    Optional<Product> findById(Long id);
}
