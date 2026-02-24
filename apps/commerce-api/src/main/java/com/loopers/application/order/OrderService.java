package com.loopers.application.order;

import com.loopers.application.order.CreateOrderUseCase;
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
public class OrderService implements CreateOrderUseCase {

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
}
