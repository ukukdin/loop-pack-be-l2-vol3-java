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

    /** 주문 에러 */
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "주문을 찾을 수 없습니다."),

    /** 상품 에러 */
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", "상품을 찾을 수 없습니다."),

    /** 브랜드 에러 */
    BRAND_NOT_FOUND(HttpStatus.NOT_FOUND, "BRAND_NOT_FOUND", "브랜드를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
