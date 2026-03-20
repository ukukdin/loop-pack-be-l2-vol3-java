package com.loopers.support.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorType {
    /** 범용 에러 */
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), "일시적인 오류가 발생했습니다."),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.getReasonPhrase(), "잘못된 요청입니다."),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "입력값이 올바르지 않습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, HttpStatus.NOT_FOUND.getReasonPhrase(), "존재하지 않는 요청입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Unauthorized", "인증에 실패했습니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "Forbidden", "접근 권한이 없습니다."),
    CONFLICT(HttpStatus.CONFLICT, HttpStatus.CONFLICT.getReasonPhrase(), "이미 존재하는 리소스입니다."),

    /** 쿠폰 에러 */
    COUPON_NOT_FOUND(HttpStatus.NOT_FOUND, "COUPON_NOT_FOUND", "쿠폰을 찾을 수 없습니다."),
    COUPON_NOT_AVAILABLE(HttpStatus.BAD_REQUEST, "COUPON_NOT_AVAILABLE", "발급할 수 없는 쿠폰입니다."),
    COUPON_EXCEEDED(HttpStatus.BAD_REQUEST, "COUPON_EXCEEDED", "발급 가능 수량을 초과했습니다."),
    COUPON_EXPIRED(HttpStatus.BAD_REQUEST, "COUPON_EXPIRED", "만료된 쿠폰입니다."),
    COUPON_NOT_OWNED(HttpStatus.FORBIDDEN, "COUPON_NOT_OWNED", "본인의 쿠폰만 사용할 수 있습니다."),
    COUPON_NOT_USABLE(HttpStatus.BAD_REQUEST, "COUPON_NOT_USABLE", "사용할 수 없는 쿠폰입니다."),
    COUPON_MIN_ORDER_AMOUNT(HttpStatus.BAD_REQUEST, "COUPON_MIN_ORDER_AMOUNT", "최소 주문금액 미달입니다."),

    COUPON_INVALID_CODE(HttpStatus.BAD_REQUEST, "COUPON_INVALID_CODE", "쿠폰 코드는 필수입니다."),
    COUPON_INVALID_CODE_FORMAT(HttpStatus.BAD_REQUEST, "COUPON_INVALID_CODE_FORMAT", "쿠폰 코드는 영문 대문자와 숫자만 가능합니다."),
    COUPON_INVALID_NAME(HttpStatus.BAD_REQUEST, "COUPON_INVALID_NAME", "쿠폰 이름은 필수입니다."),
    COUPON_INVALID_NAME_LENGTH(HttpStatus.BAD_REQUEST, "COUPON_INVALID_NAME_LENGTH", "쿠폰 이름은 50자 이하여야 합니다."),
    COUPON_INVALID_DISCOUNT_POLICY(HttpStatus.BAD_REQUEST, "COUPON_INVALID_DISCOUNT_POLICY", "할인 정책은 필수입니다."),
    COUPON_INVALID_DISCOUNT_VALUE(HttpStatus.BAD_REQUEST, "COUPON_INVALID_DISCOUNT_VALUE", "할인 값은 0보다 커야 합니다."),
    COUPON_INVALID_DISCOUNT_RATE(HttpStatus.BAD_REQUEST, "COUPON_INVALID_DISCOUNT_RATE", "정률 할인은 100%를 초과할 수 없습니다."),
    COUPON_INVALID_ISSUANCE_POLICY(HttpStatus.BAD_REQUEST, "COUPON_INVALID_ISSUANCE_POLICY", "발급 정책은 필수입니다."),
    COUPON_INVALID_ISSUANCE_COUNT(HttpStatus.BAD_REQUEST, "COUPON_INVALID_ISSUANCE_COUNT", "최대 발급 수량은 0보다 커야 합니다."),
    COUPON_INVALID_ISSUANCE_PER_USER(HttpStatus.BAD_REQUEST, "COUPON_INVALID_ISSUANCE_PER_USER", "1인당 발급 수량은 총 발급 수량보다 클 수 없습니다."),
    COUPON_INVALID_TARGET(HttpStatus.BAD_REQUEST, "COUPON_INVALID_TARGET", "적용 대상은 필수입니다."),
    COUPON_INVALID_TARGET_IDS(HttpStatus.BAD_REQUEST, "COUPON_INVALID_TARGET_IDS", "특정 대상 쿠폰은 적용 대상 ID가 필요합니다."),
    COUPON_INVALID_EXPIRED_AT(HttpStatus.BAD_REQUEST, "COUPON_INVALID_EXPIRED_AT", "만료일은 현재 시각 이후여야 합니다."),

    /** 주문 에러 */
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "주문을 찾을 수 없습니다."),

    /** 상품 에러 */
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", "상품을 찾을 수 없습니다."),

    /** 브랜드 에러 */
    BRAND_NOT_FOUND(HttpStatus.NOT_FOUND, "BRAND_NOT_FOUND", "브랜드를 찾을 수 없습니다."),

    /**결제 에러*/
    PAYMENT_REQUEST_FAILED(HttpStatus.BAD_GATEWAY, "PAYMENT_REQUEST_FAILED", "PG 결제요청 실패");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
