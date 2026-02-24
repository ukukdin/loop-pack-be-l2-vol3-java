package com.loopers.domain.model.product;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public class LikeCount {

    private final int value;

    private LikeCount(int value) {
        this.value = value;
    }

    public static LikeCount of(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("좋아요 수는 0 이상이어야 합니다.");
        }
        return new LikeCount(value);
    }

    public static LikeCount zero() {
        return new LikeCount(0);
    }

    public LikeCount increase() {
        return new LikeCount(this.value + 1);
    }

    public LikeCount decrease() {
        if (this.value <= 0) {
            throw new IllegalStateException("좋아요 수는 0 미만이 될 수 없습니다.");
        }
        return new LikeCount(this.value - 1);
    }
}
