package com.loopers.domain.model.product;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public class Description {

    private static final int MAX_LENGTH = 500;
    private static final Description EMPTY = new Description(null);

    private final String value;

    private Description(String value) {
        this.value = value;
    }

    public static Description of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("상품 설명은 필수 입력값입니다.");
        }
        String trimmed = value.trim();
        if (trimmed.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("상품 설명은 " + MAX_LENGTH + "자 이하여야 합니다.");
        }
        return new Description(trimmed);
    }

    public static Description empty() {
        return EMPTY;
    }

    public static Description ofNullable(String value) {
        if (value == null || value.isBlank()) {
            return EMPTY;
        }
        return of(value);
    }

    public boolean isEmpty() {
        return this.value == null;
    }

    public String getValueOrNull() {
        return this.value;
    }
}
