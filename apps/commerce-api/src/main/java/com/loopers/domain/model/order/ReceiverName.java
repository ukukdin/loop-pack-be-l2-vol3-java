package com.loopers.domain.model.order;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public class ReceiverName {

    private final String value;

    private ReceiverName(String value) {
        this.value = value;
    }

    public static ReceiverName of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("수령인 이름은 필수 입력값입니다.");
        }
        return new ReceiverName(value.trim());
    }
}
