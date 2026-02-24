package com.loopers.domain.repository;

import com.loopers.domain.model.product.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {

    Product save(Product product);

    Optional<Product> findById(Long id);

    Page<Product> findAllByDeletedAtIsNull(Long brandId, Pageable pageable);

    List<Product> findAllByBrandId(Long brandId);
}
