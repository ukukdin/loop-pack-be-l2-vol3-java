package com.loopers.application.order;

import com.loopers.domain.model.order.*;
import com.loopers.domain.model.product.Product;
import com.loopers.domain.model.user.UserId;
import com.loopers.domain.repository.OrderRepository;
import com.loopers.domain.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class OrderService implements CreateOrderUseCase, CancelOrderUseCase, UpdateDeliveryAddressUseCase {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public OrderService(OrderRepository orderRepository, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    @Override
    public void createOrder(UserId userId, OrderCommand command) {
        List<OrderItem> orderItems = new ArrayList<>();
        StringBuilder snapshotBuilder = new StringBuilder();

        for (OrderItemCommand itemCommand : command.items()) {
            Product product = productRepository.findById(itemCommand.productId())
                    .filter(p -> !p.isDeleted())
                    .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다. ID: " + itemCommand.productId()));

            Product decreased = product.decreaseStock(itemCommand.quantity());
            productRepository.save(decreased);

            OrderItem orderItem = OrderItem.create(
                    product.getId(),
                    itemCommand.quantity(),
                    Money.of(product.getPrice().getValue())
            );
            orderItems.add(orderItem);

            snapshotBuilder.append(product.getName().getValue())
                    .append(":")
                    .append(product.getPrice().getValue())
                    .append(",");
        }

        OrderSnapshot snapshot = OrderSnapshot.create(snapshotBuilder.toString());
        PaymentMethod paymentMethod = PaymentMethod.valueOf(command.paymentMethod());

        Order order = Order.create(
                userId,
                orderItems,
                ReceiverName.of(command.receiverName()),
                Address.of(command.address()),
                command.deliveryRequest(),
                paymentMethod,
                Money.zero(),
                command.desiredDeliveryDate(),
                snapshot
        );

        orderRepository.save(order);
    }

    @Override
    public void cancelOrder(UserId userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .filter(o -> o.getUserId().equals(userId))
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));

        Order cancelled = order.cancel();
        orderRepository.save(cancelled);

        for (OrderItem item : order.getItems()) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));
            Product restored = product.increaseStock(item.getQuantity());
            productRepository.save(restored);
        }
    }

    @Override
    public void updateDeliveryAddress(UserId userId, Long orderId, String newAddress) {
        Order order = orderRepository.findById(orderId)
                .filter(o -> o.getUserId().equals(userId))
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));

        Order updated = order.updateDeliveryAddress(Address.of(newAddress));
        orderRepository.save(updated);
    }
}
