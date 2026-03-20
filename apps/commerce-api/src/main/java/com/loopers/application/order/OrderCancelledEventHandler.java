package com.loopers.application.order;

import com.loopers.application.payment.RefundPaymentUseCase;
import com.loopers.domain.model.order.event.OrderCancelledEvent;
import com.loopers.domain.model.product.Product;
import com.loopers.domain.model.userCoupon.UserCoupon;
import com.loopers.domain.repository.ProductRepository;
import com.loopers.domain.repository.UserCouponRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class OrderCancelledEventHandler {

    private final ProductRepository productRepository;
    private final UserCouponRepository userCouponRepository;
    private final RefundPaymentUseCase refundPaymentUseCase;

    public OrderCancelledEventHandler(ProductRepository productRepository,
                                      UserCouponRepository userCouponRepository,
                                      RefundPaymentUseCase refundPaymentUseCase) {
        this.productRepository = productRepository;
        this.userCouponRepository = userCouponRepository;
        this.refundPaymentUseCase = refundPaymentUseCase;
    }

    @EventListener
    public void handle(OrderCancelledEvent event) {
        restoreStock(event);
        restoreCoupon(event);
        refundPayment(event);
    }

    private void restoreStock(OrderCancelledEvent event) {
        List<OrderCancelledEvent.CancelledItem> sortedItems = event.cancelledItems().stream()
                .sorted(Comparator.comparingLong(OrderCancelledEvent.CancelledItem::productId))
                .toList();

        for (OrderCancelledEvent.CancelledItem item : sortedItems) {
            Product product = productRepository.findActiveByIdWithLock(item.productId())
                    .orElseThrow(() -> new CoreException(ErrorType.PRODUCT_NOT_FOUND));
            Product restored = product.increaseStock(item.quantity());
            productRepository.save(restored);
        }
    }

    private void restoreCoupon(OrderCancelledEvent event) {
        if (event.userCouponId() == null) {
            return;
        }
        UserCoupon userCoupon = userCouponRepository.findByIdWithLock(event.userCouponId())
                .orElseThrow(() -> new CoreException(ErrorType.COUPON_NOT_FOUND));
        UserCoupon restored = userCoupon.restore();
        userCouponRepository.save(restored);
    }

    private void refundPayment(OrderCancelledEvent event) {
        if (!event.needsRefund()) {
            return;
        }
        refundPaymentUseCase.refundPayment(event.userId(), event.orderId());
    }
}
