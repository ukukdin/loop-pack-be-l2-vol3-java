package com.loopers.domain.model.order;

import com.loopers.domain.model.user.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    private Order createOrder() {
        List<OrderLine> orderLines = List.of(
                new OrderLine(1L, "상품A", Money.of(10000), Quantity.of(2)),
                new OrderLine(2L, "상품B", Money.of(20000), Quantity.of(1))
        );

        return Order.create(
                UserId.of("testuser1"),
                orderLines,
                ReceiverName.of("홍길동"),
                Address.of("서울시 강남구"),
                "부재시 문 앞에 놓아주세요",
                PaymentMethod.CARD,
                Money.zero(),
                LocalDate.now().plusDays(3)
        );
    }

    @Test
    @DisplayName("주문 생성 성공 - 금액 자동 계산")
    void create_success() {
        Order order = createOrder();

        assertThat(order.getId()).isNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYMENT_COMPLETED);
        assertThat(order.getTotalAmount().getValue()).isEqualTo(40000); // 10000*2 + 20000*1
        assertThat(order.getPaymentAmount().getValue()).isEqualTo(40000);
        assertThat(order.getItems()).hasSize(2);
        assertThat(order.getSnapshot()).isNotNull();
    }

    @Test
    @DisplayName("주문 생성 - 할인 적용")
    void create_with_discount() {
        List<OrderLine> orderLines = List.of(
                new OrderLine(1L, "상품A", Money.of(50000), Quantity.of(1))
        );

        Order order = Order.create(
                UserId.of("testuser1"), orderLines,
                ReceiverName.of("홍길동"), Address.of("서울시"),
                null, PaymentMethod.CARD,
                Money.of(5000), null
        );

        assertThat(order.getTotalAmount().getValue()).isEqualTo(50000);
        assertThat(order.getDiscountAmount().getValue()).isEqualTo(5000);
        assertThat(order.getPaymentAmount().getValue()).isEqualTo(45000);
    }

    @Test
    @DisplayName("userId null이면 예외")
    void create_fail_null_userId() {
        List<OrderLine> orderLines = List.of(
                new OrderLine(1L, "상품A", Money.of(10000), Quantity.of(1))
        );

        assertThatThrownBy(() -> Order.create(null, orderLines,
                ReceiverName.of("홍길동"), Address.of("서울시"),
                null, PaymentMethod.CARD, Money.zero(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("사용자 ID는 필수입니다.");
    }

    @Test
    @DisplayName("주문 항목 비어있으면 예외")
    void create_fail_empty_items() {
        assertThatThrownBy(() -> Order.create(UserId.of("testuser1"), List.of(),
                ReceiverName.of("홍길동"), Address.of("서울시"),
                null, PaymentMethod.CARD, Money.zero(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1개 이상");
    }

    @Test
    @DisplayName("PAYMENT_COMPLETED 상태에서 취소 가능")
    void cancel_success() {
        Order order = createOrder();
        assertThat(order.isCancellable()).isTrue();

        Order cancelled = order.cancel();
        assertThat(cancelled.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(cancelled.getDomainEvents()).hasSize(1);
    }

    @Test
    @DisplayName("SHIPPING 상태에서 취소 불가")
    void cancel_fail_shipping() {
        Order order = Order.reconstitute(
                1L, UserId.of("testuser1"),
                List.of(OrderItem.create(1L, Quantity.of(1), Money.of(10000))),
                null, ReceiverName.of("홍길동"), Address.of("서울시"), null,
                PaymentMethod.CARD, Money.of(10000), Money.zero(), Money.of(10000),
                OrderStatus.SHIPPING, null, LocalDateTime.now(), LocalDateTime.now()
        );

        assertThat(order.isCancellable()).isFalse();
        assertThatThrownBy(order::cancel)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("취소할 수 없습니다");
    }

    @Test
    @DisplayName("DELIVERED 상태에서 취소 불가")
    void cancel_fail_delivered() {
        Order order = Order.reconstitute(
                1L, UserId.of("testuser1"),
                List.of(OrderItem.create(1L, Quantity.of(1), Money.of(10000))),
                null, ReceiverName.of("홍길동"), Address.of("서울시"), null,
                PaymentMethod.CARD, Money.of(10000), Money.zero(), Money.of(10000),
                OrderStatus.DELIVERED, null, LocalDateTime.now(), LocalDateTime.now()
        );

        assertThatThrownBy(order::cancel)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("배송지 변경 성공 (PAYMENT_COMPLETED)")
    void updateDeliveryAddress_success() {
        Order order = createOrder();
        Order updated = order.updateDeliveryAddress(Address.of("부산시 해운대구"));

        assertThat(updated.getAddress().getValue()).isEqualTo("부산시 해운대구");
    }

    @Test
    @DisplayName("SHIPPING 상태에서 배송지 변경 불가")
    void updateDeliveryAddress_fail_shipping() {
        Order order = Order.reconstitute(
                1L, UserId.of("testuser1"),
                List.of(OrderItem.create(1L, Quantity.of(1), Money.of(10000))),
                null, ReceiverName.of("홍길동"), Address.of("서울시"), null,
                PaymentMethod.CARD, Money.of(10000), Money.zero(), Money.of(10000),
                OrderStatus.SHIPPING, null, LocalDateTime.now(), LocalDateTime.now()
        );

        assertThatThrownBy(() -> order.updateDeliveryAddress(Address.of("부산시")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("배송지를 변경할 수 없습니다");
    }
}
