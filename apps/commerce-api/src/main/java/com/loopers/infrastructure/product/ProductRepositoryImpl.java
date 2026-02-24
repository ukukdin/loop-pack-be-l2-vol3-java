package com.loopers.infrastructure.product;

import com.loopers.domain.model.product.Price;
import com.loopers.domain.model.product.Product;
import com.loopers.domain.model.product.ProductName;
import com.loopers.domain.model.product.Stock;
import com.loopers.domain.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ProductRepositoryImpl implements ProductRepository {

    private final ProductJpaRepository productJpaRepository;

    public ProductRepositoryImpl(ProductJpaRepository productJpaRepository) {
        this.productJpaRepository = productJpaRepository;
    }

    @Override
    public Product save(Product product) {
        ProductJpaEntity entity = toEntity(product);
        ProductJpaEntity saved = productJpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Product> findById(Long id) {
        return productJpaRepository.findById(id)
                .map(this::toDomain);
    }

    @Override
    public Page<Product> findAllByDeletedAtIsNull(Long brandId, Pageable pageable) {
        Page<ProductJpaEntity> page;
        if (brandId != null) {
            page = productJpaRepository.findAllByBrandIdAndDeletedAtIsNull(brandId, pageable);
        } else {
            page = productJpaRepository.findAllByDeletedAtIsNull(pageable);
        }
        return page.map(this::toDomain);
    }

    @Override
    public List<Product> findAllByBrandId(Long brandId) {
        return productJpaRepository.findAllByBrandIdAndDeletedAtIsNull(brandId).stream()
                .map(this::toDomain)
                .toList();
    }

    private ProductJpaEntity toEntity(Product product) {
        return new ProductJpaEntity(
                product.getId(),
                product.getBrandId(),
                product.getName().getValue(),
                product.getPrice().getValue(),
                product.getStock().getValue(),
                product.getLikeCount(),
                product.getDescription(),
                product.getCreatedAt(),
                product.getUpdatedAt(),
                product.getDeletedAt()
        );
    }

    private Product toDomain(ProductJpaEntity entity) {
        return Product.reconstitute(
                entity.getId(),
                entity.getBrandId(),
                ProductName.of(entity.getName()),
                Price.of(entity.getPrice()),
                Stock.of(entity.getStockQuantity()),
                entity.getLikeCount(),
                entity.getDescription(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getDeletedAt()
        );
    }
}
