package com.loopers.domain.model.order;

import com.loopers.domain.model.user.UserId;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Order {

    private final Long id;
    private final UserId userId;
    private final List<OrderItem> items;
    private final OrderSnapshot snapshot;
    private final ReceiverName receiverName;
    private final Address address;
    private final String deliveryRequest;
    private final PaymentMethod paymentMethod;
    private final Money totalAmount;
    private final Money discountAmount;
    private final Money paymentAmount;
    private final OrderStatus status;
    private final LocalDate desiredDeliveryDate;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public static Order create(UserId userId, List<OrderItem> items, ReceiverName receiverName,
                               Address address, String deliveryRequest, PaymentMethod paymentMethod,
                               Money discountAmount, LocalDate desiredDeliveryDate,
                               OrderSnapshot snapshot) {
        if (userId == null) {
            throw new IllegalArgumentException("사용자 ID는 필수입니다.");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("주문 항목은 1개 이상이어야 합니다.");
        }
        if (paymentMethod == null) {
            throw new IllegalArgumentException("결제 수단은 필수입니다.");
        }

        Money totalAmount = calculateTotalAmount(items);
        Money paymentAmount = totalAmount.subtract(discountAmount);
        LocalDateTime now = LocalDateTime.now();

        return new Order(null, userId, items, snapshot, receiverName, address, deliveryRequest,
                paymentMethod, totalAmount, discountAmount, paymentAmount,
                OrderStatus.PAYMENT_COMPLETED, desiredDeliveryDate, now, now);
    }

    public static Order reconstitute(Long id, UserId userId, List<OrderItem> items, OrderSnapshot snapshot,
                                     ReceiverName receiverName, Address address, String deliveryRequest,
                                     PaymentMethod paymentMethod, Money totalAmount, Money discountAmount,
                                     Money paymentAmount, OrderStatus status, LocalDate desiredDeliveryDate,
                                     LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new Order(id, userId, items, snapshot, receiverName, address, deliveryRequest,
                paymentMethod, totalAmount, discountAmount, paymentAmount, status,
                desiredDeliveryDate, createdAt, updatedAt);
    }

    public Order cancel() {
        if (!isCancellable()) {
            throw new IllegalStateException("현재 상태에서는 주문을 취소할 수 없습니다. 현재 상태: " + status.getDescription());
        }
        return new Order(this.id, this.userId, this.items, this.snapshot, this.receiverName,
                this.address, this.deliveryRequest, this.paymentMethod, this.totalAmount,
                this.discountAmount, this.paymentAmount, OrderStatus.PAYMENT_COMPLETED,
                this.desiredDeliveryDate, this.createdAt, LocalDateTime.now());
    }

    public Order updateDeliveryAddress(Address newAddress) {
        if (!status.isAddressChangeable()) {
            throw new IllegalStateException("현재 상태에서는 배송지를 변경할 수 없습니다. 현재 상태: " + status.getDescription());
        }
        return new Order(this.id, this.userId, this.items, this.snapshot, this.receiverName,
                newAddress, this.deliveryRequest, this.paymentMethod, this.totalAmount,
                this.discountAmount, this.paymentAmount, this.status,
                this.desiredDeliveryDate, this.createdAt, LocalDateTime.now());
    }

    public boolean isCancellable() {
        return status.isCancellable();
    }

    private static Money calculateTotalAmount(List<OrderItem> items) {
        return items.stream()
                .map(OrderItem::calculateAmount)
                .reduce(Money.zero(), Money::add);
    }
}
