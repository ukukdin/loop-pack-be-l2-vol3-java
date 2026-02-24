package com.loopers.infrastructure.repository;

import com.loopers.infrastructure.entity.BrandJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BrandJpaRepository extends JpaRepository<BrandJpaEntity, Long> {

    boolean existsByName(String name);

    List<BrandJpaEntity> findAllByDeletedAtIsNull();
}
