package com.loopers.domain.model.brand;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Brand {

    private final Long id;
    private final BrandName name;
    private final String description;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final LocalDateTime deletedAt;

    public static Brand create(BrandName name, String description) {
        LocalDateTime now = LocalDateTime.now();
        return new Brand(null, name, description, now, now, null);
    }

    public static Brand reconstitute(Long id, BrandName name, String description,
                                     LocalDateTime createdAt, LocalDateTime updatedAt,
                                     LocalDateTime deletedAt) {
        return new Brand(id, name, description, createdAt, updatedAt, deletedAt);
    }

    public Brand update(BrandName name, String description) {
        return new Brand(this.id, name, description, this.createdAt, LocalDateTime.now(), this.deletedAt);
    }

    public Brand delete() {
        if (isDeleted()) {
            throw new IllegalStateException("이미 삭제된 브랜드입니다.");
        }
        return new Brand(this.id, this.name, this.description, this.createdAt, this.updatedAt, LocalDateTime.now());
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }
}
