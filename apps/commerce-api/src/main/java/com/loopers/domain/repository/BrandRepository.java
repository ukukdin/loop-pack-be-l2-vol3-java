package com.loopers.domain.repository;

import com.loopers.domain.model.brand.Brand;
import com.loopers.domain.model.brand.BrandName;

import java.util.List;
import java.util.Optional;

public interface BrandRepository {

    Brand save(Brand brand);

    Optional<Brand> findById(Long id);

    List<Brand> findAll();

    boolean existsByName(BrandName name);
}
