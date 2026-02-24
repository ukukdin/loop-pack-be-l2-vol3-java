package com.loopers.application.service;

import com.loopers.application.OrderQueryUseCase;
import com.loopers.domain.model.order.Order;
import com.loopers.domain.model.order.OrderItem;
import com.loopers.domain.model.user.UserId;
import com.loopers.domain.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class OrderQueryService implements OrderQueryUseCase {

    private final OrderRepository orderRepository;

    public OrderQueryService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public List<OrderSummary> getMyOrders(UserId userId) {
        List<Order> orders = orderRepository.findAllByUserId(userId);

        return orders.stream()
                .map(order -> new OrderSummary(
                        order.getId(),
                        order.getStatus().name(),
                        order.getPaymentAmount().getValue(),
                        order.getCreatedAt()
                ))
                .toList();
    }

    @Override
    public OrderDetail getOrder(UserId userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .filter(o -> o.getUserId().equals(userId))
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));

        List<OrderItemDetail> itemDetails = order.getItems().stream()
                .map(this::toOrderItemDetail)
                .toList();

        return new OrderDetail(
                order.getId(),
                order.getReceiverName().getValue(),
                order.getAddress().getValue(),
                order.getDeliveryRequest(),
                order.getPaymentMethod().name(),
                order.getTotalAmount().getValue(),
                order.getDiscountAmount().getValue(),
                order.getPaymentAmount().getValue(),
                order.getStatus().name(),
                itemDetails,
                order.getCreatedAt()
        );
    }

    private OrderItemDetail toOrderItemDetail(OrderItem item) {
        return new OrderItemDetail(
                item.getProductId(),
                item.getQuantity(),
                item.getUnitPrice().getValue()
        );
    }
}
