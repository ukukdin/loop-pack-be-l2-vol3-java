package com.loopers.infrastructure;

import com.loopers.domain.model.product.Price;
import com.loopers.domain.model.product.Product;
import com.loopers.domain.model.product.ProductName;
import com.loopers.domain.model.product.Stock;
import com.loopers.domain.repository.ProductRepository;
import com.loopers.infrastructure.entity.ProductJpaEntity;
import com.loopers.infrastructure.repository.ProductJpaRepository;
import org.springframework.stereotype.Repository;

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
