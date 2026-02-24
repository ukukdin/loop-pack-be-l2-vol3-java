package com.loopers.domain.model.like;

import com.loopers.domain.model.user.UserId;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Like {

    private final Long id;
    private final UserId userId;
    private final Long productId;
    private final LocalDateTime createdAt;

    public static Like create(UserId userId, Long productId) {
        if (userId == null) {
            throw new IllegalArgumentException("사용자 ID는 필수입니다.");
        }
        if (productId == null) {
            throw new IllegalArgumentException("상품 ID는 필수입니다.");
        }
        return new Like(null, userId, productId, LocalDateTime.now());
    }

    public static Like reconstitute(Long id, UserId userId, Long productId, LocalDateTime createdAt) {
        return new Like(id, userId, productId, createdAt);
    }
}
