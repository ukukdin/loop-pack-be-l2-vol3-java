package com.loopers.application.order;

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

/**
 * 주문 취소 시 재고/쿠폰 복구를 동기적으로 처리한다.
 * 같은 트랜잭션 내에서 실행되어 데이터 정합성을 보장한다.
 * PG 환불은 {@link OrderRefundEventHandler}에서 AFTER_COMMIT으로 별도 처리한다.
 */
@Component
public class OrderCancelledEventHandler {

    private final ProductRepository productRepository;
    private final UserCouponRepository userCouponRepository;

    public OrderCancelledEventHandler(ProductRepository productRepository,
                                      UserCouponRepository userCouponRepository) {
        this.productRepository = productRepository;
        this.userCouponRepository = userCouponRepository;
    }

    @EventListener
    public void handle(OrderCancelledEvent event) {
        restoreStock(event);
        restoreCoupon(event);
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
}
