package com.loopers.infrastructure.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductJpaRepository extends JpaRepository<ProductJpaEntity, Long> {

    Page<ProductJpaEntity> findAllByBrandIdAndDeletedAtIsNull(Long brandId, Pageable pageable);

    Page<ProductJpaEntity> findAllByDeletedAtIsNull(Pageable pageable);

    List<ProductJpaEntity> findAllByBrandIdAndDeletedAtIsNull(Long brandId);
}
