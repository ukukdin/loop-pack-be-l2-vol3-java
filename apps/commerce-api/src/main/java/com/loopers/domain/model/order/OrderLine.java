package com.loopers.domain.model.order;

public record OrderLine(
        Long productId,
        String productName,
        Money unitPrice,
        Quantity quantity
) {
    public OrderLine {
        if (productId == null) {
            throw new IllegalArgumentException("상품 ID는 필수입니다.");
        }
        if (productName == null || productName.isBlank()) {
            throw new IllegalArgumentException("상품명은 필수입니다.");
        }
        if (unitPrice == null) {
            throw new IllegalArgumentException("단가는 필수입니다.");
        }
        if (quantity == null) {
            throw new IllegalArgumentException("수량은 필수입니다.");
        }
    }
}
