package com.loopers.domain.model.product;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Product {

    private final Long id;
    private final Long brandId;
    private final ProductName name;
    private final Price price;
    private final Stock stock;
    private final int likeCount;
    private final String description;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final LocalDateTime deletedAt;

    public static Product create(Long brandId, ProductName name, Price price, Stock stock, String description) {
        LocalDateTime now = LocalDateTime.now();
        return new Product(null, brandId, name, price, stock, 0, description, now, now, null);
    }

    public static Product reconstitute(Long id, Long brandId, ProductName name, Price price, Stock stock,
                                       int likeCount, String description,
                                       LocalDateTime createdAt, LocalDateTime updatedAt,
                                       LocalDateTime deletedAt) {
        return new Product(id, brandId, name, price, stock, likeCount, description, createdAt, updatedAt, deletedAt);
    }

    public Product update(ProductName name, Price price, Stock stock, String description) {
        return new Product(this.id, this.brandId, name, price, stock, this.likeCount,
                description, this.createdAt, LocalDateTime.now(), this.deletedAt);
    }

    public Product delete() {
        if (isDeleted()) {
            throw new IllegalStateException("이미 삭제된 상품입니다.");
        }
        return new Product(this.id, this.brandId, this.name, this.price, this.stock, this.likeCount,
                this.description, this.createdAt, this.updatedAt, LocalDateTime.now());
    }

    public Product decreaseStock(int quantity) {
        Stock decreased = this.stock.decrease(quantity);
        return new Product(this.id, this.brandId, this.name, this.price, decreased, this.likeCount,
                this.description, this.createdAt, LocalDateTime.now(), this.deletedAt);
    }

    public Product increaseLikeCount() {
        return new Product(this.id, this.brandId, this.name, this.price, this.stock, this.likeCount + 1,
                this.description, this.createdAt, this.updatedAt, this.deletedAt);
    }

    public Product decreaseLikeCount() {
        if (this.likeCount <= 0) {
            throw new IllegalStateException("좋아요 수는 0 미만이 될 수 없습니다.");
        }
        return new Product(this.id, this.brandId, this.name, this.price, this.stock, this.likeCount - 1,
                this.description, this.createdAt, this.updatedAt, this.deletedAt);
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }
}
