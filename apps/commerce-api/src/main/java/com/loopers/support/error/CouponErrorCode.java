package com.loopers.support.error;

import lombok.Getter;

@Getter
public enum CouponErrorCode {
    // 코드
    INVALID_CODE("쿠폰 코드는 필수입니다."),
    INVALID_CODE_FORMAT("쿠폰 코드는 영문 대문자와 숫자만 가능합니다."),

    // 이름
    INVALID_NAME("쿠폰 이름은 필수입니다."),
    INVALID_NAME_LENGTH("쿠폰 이름은 50자 이하여야 합니다."),

    // 설명
    INVALID_DESCRIPTION_LENGTH("쿠폰 설명은 255자 이하여야 합니다."),

    // 할인 정책
    INVALID_DISCOUNT_POLICY("할인 정책은 필수입니다."),
    INVALID_DISCOUNT_VALUE("할인 값은 0보다 커야 합니다."),
    INVALID_DISCOUNT_RATE("정률 할인은 100%를 초과할 수 없습니다."),

    // 발급 정책
    INVALID_ISSUANCE_POLICY("발급 정책은 필수입니다."),
    INVALID_ISSUANCE_COUNT("최대 발급 수량은 0보다 커야 합니다."),
    INVALID_ISSUANCE_PER_USER("1인당 발급 수량은 총 발급 수량보다 클 수 없습니다."),

    // 적용 대상
    INVALID_APPLICATION_TARGET("적용 대상은 필수입니다."),
    INVALID_TARGET_IDS("특정 대상 쿠폰은 적용 대상 ID가 필요합니다."),

    // 만료일
    INVALID_EXPIRED_AT("만료일은 현재 시각 이후여야 합니다."),

    // 상태 변경
    ALREADY_INACTIVE("이미 비활성화된 쿠폰입니다."),
    ALREADY_EXHAUSTED("이미 소진된 쿠폰입니다."),
    EXCEEDED_ISSUANCE("발급 가능 수량을 초과했습니다.");

    private final String message;

    CouponErrorCode(String message) {
        this.message = message;
    }
}
