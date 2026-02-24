package com.loopers.domain.model.order;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public class Address {

    private final String value;

    private Address(String value) {
        this.value = value;
    }

    public static Address of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("배송지 주소는 필수 입력값입니다.");
        }
        return new Address(value.trim());
    }
}
