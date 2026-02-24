package com.loopers.domain.model.order;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public class Quantity {

    private final int value;

    private Quantity(int value) {
        this.value = value;
    }

    public static Quantity of(int value) {
        if (value < 1) {
            throw new IllegalArgumentException("주문 수량은 1 이상이어야 합니다.");
        }
        return new Quantity(value);
    }
}
