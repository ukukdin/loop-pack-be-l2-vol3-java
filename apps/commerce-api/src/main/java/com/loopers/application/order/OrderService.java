package com.loopers.application.order;

import com.loopers.domain.model.common.DomainEventPublisher;
import com.loopers.domain.model.coupon.Coupon;
import com.loopers.domain.model.coupon.DiscountType;
import com.loopers.domain.model.order.*;
import com.loopers.domain.model.product.Product;
import com.loopers.domain.model.user.UserId;
import com.loopers.domain.model.userCoupon.UserCoupon;
import com.loopers.domain.repository.CouponRepository;
import com.loopers.domain.repository.OrderRepository;
import com.loopers.domain.repository.ProductRepository;
import com.loopers.domain.repository.UserCouponRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional
public class OrderService implements CreateOrderUseCase, CancelOrderUseCase, UpdateDeliveryAddressUseCase {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final DomainEventPublisher eventPublisher;

    public OrderService(OrderRepository orderRepository, ProductRepository productRepository,
                        CouponRepository couponRepository, UserCouponRepository userCouponRepository,
                        DomainEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.couponRepository = couponRepository;
        this.userCouponRepository = userCouponRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void createOrder(UserId userId, OrderCommand command) {
        // 1. 재고 확인 및 차감 (비관적 락 - productId 순으로 정렬하여 데드락 방지)
        List<CreateOrderUseCase.OrderItemCommand> sortedItems = command.items().stream()
                .sorted(Comparator.comparingLong(CreateOrderUseCase.OrderItemCommand::productId))
                .toList();

        List<OrderLine> orderLines = sortedItems.stream()
                .map(itemCommand -> {
                    Product product = productRepository.findActiveByIdWithLock(itemCommand.productId())
                            .orElseThrow(() -> new CoreException(ErrorType.PRODUCT_NOT_FOUND));

                    Product decreased = product.decreaseStock(itemCommand.quantity());
                    productRepository.save(decreased);

                    return new OrderLine(
                            product.getId(),
                            product.getName().getValue(),
                            Money.of(product.getPrice().getValue()),
                            itemCommand.quantity()
                    );
                })
                .toList();

        // 2. 쿠폰 유효성 검증 및 사용 처리
        Money discountAmount = Money.zero();
        if (command.couponId() != null) {
            discountAmount = processCoupon(userId, command.couponId(), orderLines);
        }

        // 3. 주문 생성
        DeliveryInfo deliveryInfo = DeliveryInfo.of(
                command.receiverName(),
                command.address(),
                command.deliveryRequest(),
                command.desiredDeliveryDate()
        );

        PaymentMethod paymentMethod = PaymentMethod.valueOf(command.paymentMethod());
        Order order = Order.create(userId, orderLines, deliveryInfo, paymentMethod,
                discountAmount, command.couponId());

        orderRepository.save(order);
    }

    private Money processCoupon(UserId userId, Long userCouponId, List<OrderLine> orderLines) {
        // 비관적 락으로 쿠폰 조회 (동시 사용 방지)
        UserCoupon userCoupon = userCouponRepository.findByIdWithLock(userCouponId)
                .orElseThrow(() -> new CoreException(ErrorType.COUPON_NOT_FOUND));

        if (!userCoupon.getUserId().equals(userId)) {
            throw new CoreException(ErrorType.COUPON_NOT_OWNED);
        }

        if (!userCoupon.isUsable()) {
            throw new CoreException(ErrorType.COUPON_NOT_USABLE);
        }

        Coupon coupon = couponRepository.findById(userCoupon.getCouponId())
                .orElseThrow(() -> new CoreException(ErrorType.COUPON_NOT_FOUND));

        if (coupon.isExpired()) {
            throw new CoreException(ErrorType.COUPON_EXPIRED);
        }

        // 총 주문금액 계산
        int totalAmount = orderLines.stream()
                .mapToInt(line -> line.unitPrice().getValue() * line.quantity())
                .sum();

        BigDecimal orderAmount = BigDecimal.valueOf(totalAmount);

        // 최소 주문금액 검증
        if (coupon.getDiscountPolicy().getMinOrderAmount() != null
                && orderAmount.compareTo(coupon.getDiscountPolicy().getMinOrderAmount()) < 0) {
            throw new CoreException(ErrorType.COUPON_MIN_ORDER_AMOUNT,
                    "최소 주문금액 " + coupon.getDiscountPolicy().getMinOrderAmount() + "원 이상이어야 쿠폰 사용이 가능합니다.");
        }

        // 할인 금액 계산
        BigDecimal discount = coupon.getDiscountPolicy().calculate(orderAmount);

        // 할인 금액이 주문 금액을 초과하지 않도록
        int discountInt = discount.intValue();
        if (discountInt > totalAmount) {
            discountInt = totalAmount;
        }

        // 쿠폰 사용 처리
        UserCoupon usedCoupon = userCoupon.use();
        userCouponRepository.save(usedCoupon);

        return Money.of(discountInt);
    }

    @Override
    public void cancelOrder(UserId userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .filter(o -> o.getUserId().equals(userId))
                .orElseThrow(() -> new CoreException(ErrorType.ORDER_NOT_FOUND));

        Order cancelled = order.cancel();
        orderRepository.save(cancelled);
        eventPublisher.publishEvents(cancelled);
    }

    @Override
    public void updateDeliveryAddress(UserId userId, Long orderId, String newAddress) {
        Order order = orderRepository.findById(orderId)
                .filter(o -> o.getUserId().equals(userId))
                .orElseThrow(() -> new CoreException(ErrorType.ORDER_NOT_FOUND));

        Order updated = order.updateDeliveryAddress(newAddress);
        orderRepository.save(updated);
    }
}
