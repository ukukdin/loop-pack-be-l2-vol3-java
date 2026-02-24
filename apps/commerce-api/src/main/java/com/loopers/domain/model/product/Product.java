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
    private final LikeCount likeCount;
    private final Description description;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final LocalDateTime deletedAt;

    public static Product create(Long brandId, ProductName name, Price price, Stock stock, String description) {
        LocalDateTime now = LocalDateTime.now();
        return new Product(null, brandId, name, price, stock, LikeCount.zero(),
                Description.ofNullable(description), now, now, null);
    }

    public static Product reconstitute(Long id, Long brandId, ProductName name, Price price, Stock stock,
                                       LikeCount likeCount, Description description,
                                       LocalDateTime createdAt, LocalDateTime updatedAt,
                                       LocalDateTime deletedAt) {
        return new Product(id, brandId, name, price, stock, likeCount, description, createdAt, updatedAt, deletedAt);
    }

    public Product update(ProductName name, Price price, Stock stock, String description) {
        return new Product(this.id, this.brandId, name, price, stock, this.likeCount,
                Description.ofNullable(description), this.createdAt, LocalDateTime.now(), this.deletedAt);
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

    public Product increaseStock(int quantity) {
        Stock increased = this.stock.increase(quantity);
        return new Product(this.id, this.brandId, this.name, this.price, increased, this.likeCount,
                this.description, this.createdAt, LocalDateTime.now(), this.deletedAt);
    }

    public Product increaseLikeCount() {
        return new Product(this.id, this.brandId, this.name, this.price, this.stock, this.likeCount.increase(),
                this.description, this.createdAt, this.updatedAt, this.deletedAt);
    }

    public Product decreaseLikeCount() {
        return new Product(this.id, this.brandId, this.name, this.price, this.stock, this.likeCount.decrease(),
                this.description, this.createdAt, this.updatedAt, this.deletedAt);
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }
}
