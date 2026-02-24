package com.loopers.interfaces.api.order;

import com.loopers.application.order.CreateOrderUseCase;
import com.loopers.application.order.OrderQueryUseCase;
import com.loopers.domain.model.user.UserId;
import com.loopers.interfaces.api.order.dto.OrderCreateRequest;
import com.loopers.interfaces.api.order.dto.OrderDetailResponse;
import com.loopers.interfaces.api.order.dto.OrderSummaryResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final CreateOrderUseCase createOrderUseCase;
    private final OrderQueryUseCase orderQueryUseCase;

    public OrderController(CreateOrderUseCase createOrderUseCase, OrderQueryUseCase orderQueryUseCase) {
        this.createOrderUseCase = createOrderUseCase;
        this.orderQueryUseCase = orderQueryUseCase;
    }

    @PostMapping
    public ResponseEntity<Void> createOrder(HttpServletRequest request,
                                            @RequestBody OrderCreateRequest orderCreateRequest) {
        UserId userId = (UserId) request.getAttribute("authenticatedUserId");
        createOrderUseCase.createOrder(userId, orderCreateRequest.toCommand());
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<OrderSummaryResponse>> getMyOrders(HttpServletRequest request) {
        UserId userId = (UserId) request.getAttribute("authenticatedUserId");
        List<OrderSummaryResponse> orders = orderQueryUseCase.getMyOrders(userId).stream()
                .map(OrderSummaryResponse::from)
                .toList();
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDetailResponse> getOrder(HttpServletRequest request,
                                                        @PathVariable Long orderId) {
        UserId userId = (UserId) request.getAttribute("authenticatedUserId");
        OrderQueryUseCase.OrderDetail detail = orderQueryUseCase.getOrder(userId, orderId);
        return ResponseEntity.ok(OrderDetailResponse.from(detail));
    }
}
