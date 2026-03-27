package com.loopers.confg.kafka;

public final class EventTypes {
    private EventTypes() {}

    public static final String PRODUCT_LIKED = "PRODUCT_LIKED";
    public static final String PRODUCT_UNLIKED = "PRODUCT_UNLIKED";
    public static final String ORDER_CREATED = "ORDER_CREATED";
    public static final String PAYMENT_COMPLETED = "PAYMENT_COMPLETED";
    public static final String COUPON_ISSUE_REQUESTED = "COUPON_ISSUE_REQUESTED";
}
