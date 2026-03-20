package com.loopers.application.payment;

import com.loopers.domain.model.order.Order;
import com.loopers.domain.model.order.OrderStatus;
import com.loopers.domain.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class PendingPaymentRecoveryScheduler {

    private static final Logger log = LoggerFactory.getLogger(PendingPaymentRecoveryScheduler.class);
    private static final int PENDING_THRESHOLD_MINUTES = 5;

    private final OrderRepository orderRepository;
    private final PaymentQueryUseCase paymentQueryUseCase;
    private static final int BATCH_SIZE = 100;
    public PendingPaymentRecoveryScheduler(OrderRepository orderRepository,
                                           PaymentQueryUseCase paymentQueryUseCase) {
        this.orderRepository = orderRepository;
        this.paymentQueryUseCase = paymentQueryUseCase;
    }

    @Scheduled(fixedDelay = 300_000)
    public void recoverPendingPayments() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(PENDING_THRESHOLD_MINUTES);
        List<Order> pendingOrders = orderRepository.findByStatusAndCreatedAtBefore(
                OrderStatus.PAYMENT_PENDING, threshold, PageRequest.of(0, BATCH_SIZE)
        );

        if (pendingOrders.isEmpty()) {
            return;
        }

        log.info("미결제 주문 복구 시작 - {}건", pendingOrders.size());

        for (Order order : pendingOrders) {
            try {
                paymentQueryUseCase.getPaymentStatus(order.getUserId(), order.getId());
                log.info("미결제 주문 복구 완료 - orderId: {}", order.getId());
            } catch (Exception e) {
                log.warn("미결제 주문 복구 실패 - orderId: {}, error: {}", order.getId(), e.getMessage());
            }
        }
    }
}
