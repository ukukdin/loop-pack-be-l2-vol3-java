package com.loopers.application.order;

import com.loopers.domain.model.order.*;
import com.loopers.domain.model.product.Product;
import com.loopers.domain.model.user.UserId;
import com.loopers.domain.repository.OrderRepository;
import com.loopers.domain.repository.ProductRepository;
import com.loopers.infrastructure.common.SpringDomainEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class OrderService implements CreateOrderUseCase, CancelOrderUseCase, UpdateDeliveryAddressUseCase {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final SpringDomainEventPublisher eventPublisher;

    public OrderService(OrderRepository orderRepository, ProductRepository productRepository,
                        SpringDomainEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void createOrder(UserId userId, OrderCommand command) {
        List<OrderLine> orderLines = command.items().stream()
                .map(itemCommand -> {
                    Product product = productRepository.findById(itemCommand.productId())
                            .filter(p -> !p.isDeleted())
                            .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다. ID: " + itemCommand.productId()));

                    Product decreased = product.decreaseStock(itemCommand.quantity());
                    productRepository.save(decreased);

                    return new OrderLine(
                            product.getId(),
                            product.getName().getValue(),
                            Money.of(product.getPrice().getValue()),
                            Quantity.of(itemCommand.quantity())
                    );
                })
                .toList();

        PaymentMethod paymentMethod = PaymentMethod.valueOf(command.paymentMethod());
        Order order = Order.create(
                userId, orderLines,
                ReceiverName.of(command.receiverName()),
                Address.of(command.address()),
                command.deliveryRequest(),
                paymentMethod,
                Money.zero(),
                command.desiredDeliveryDate()
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
        eventPublisher.publishEvents(cancelled);
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
