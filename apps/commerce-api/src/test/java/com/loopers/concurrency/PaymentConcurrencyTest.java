package com.loopers.concurrency;

import com.loopers.application.order.CreateOrderUseCase;
import com.loopers.application.order.UpdateOrderPaymentUseCase;
import com.loopers.application.payment.PaymentCallbackUseCase;
import com.loopers.application.payment.PaymentQueryUseCase;
import com.loopers.application.payment.RequestPaymentUseCase;
import com.loopers.domain.model.order.Order;
import com.loopers.domain.model.order.OrderStatus;
import com.loopers.domain.model.product.Product;
import com.loopers.domain.model.user.UserId;
import com.loopers.domain.repository.OrderRepository;
import com.loopers.domain.repository.ProductRepository;
import com.loopers.infrastructure.pg.PaymentGatewayClient;
import com.loopers.infrastructure.pg.PaymentGatewayResponse;
import com.loopers.testcontainers.PostgreSQLTestContainersConfig;
import com.loopers.testcontainers.RedisTestContainersConfig;
import com.loopers.utils.DatabaseCleanUp;
import com.loopers.interfaces.api.brand.dto.BrandCreateRequest;
import com.loopers.interfaces.api.product.dto.ProductCreateRequest;
import com.loopers.interfaces.api.user.dto.UserRegisterRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "pg-simulator.url=http://localhost:9999",
                "payment.callback-url=http://localhost:8080/api/v1/payments/callback"
        }
)
@Import({PostgreSQLTestContainersConfig.class, RedisTestContainersConfig.class})
class PaymentConcurrencyTest {

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private DatabaseCleanUp databaseCleanUp;
    @Autowired private OrderRepository orderRepository;
    @Autowired private CreateOrderUseCase createOrderUseCase;
    @Autowired private PaymentCallbackUseCase paymentCallbackUseCase;
    @Autowired private PaymentQueryUseCase paymentQueryUseCase;
    @Autowired private RequestPaymentUseCase requestPaymentUseCase;
    @Autowired private ProductRepository productRepository;
    @Autowired private com.loopers.infrastructure.order.OrderJpaRepository orderJpaRepository;

    @MockitoBean private PaymentGatewayClient pgClient;

    @BeforeEach
    void setUp() {
        databaseCleanUp.truncateAllTables();
        createBrand("나이키", "스포츠");
        createProduct(1L, "운동화", 50000, 20);
    }

    // ==========================================
    // 시나리오 1: 동시 주문 → PG 결제 요청
    // ==========================================
    @Nested
    @DisplayName("시나리오 1: 동시 주문 시 PG 결제 요청")
    class ConcurrentOrderPayment {

        @Test
        @DisplayName("10명이 동시에 주문하면 각각 독립적으로 주문이 생성되고 PAYMENT_PENDING 상태여야 한다")
        void concurrent_orders_should_create_independent_payments() throws InterruptedException {
            // given - PG 성공 응답 설정
            when(pgClient.createPayment(anyLong(), any())).thenAnswer(invocation -> {
                Thread.sleep(50);
                return new PaymentGatewayResponse.ApiResponse<>("OK",
                        new PaymentGatewayResponse.Transaction("txn-" + System.nanoTime(), "PENDING", null));
            });

            int threadCount = 10;
            for (int i = 0; i < threadCount; i++) {
                registerUser("user" + String.format("%04d", i), "Password1!", "사용자" + i);
            }

            ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);

            // when - 10명 동시 주문 (application layer 직접 호출)
            for (int i = 0; i < threadCount; i++) {
                String loginId = "user" + String.format("%04d", i);
                executorService.submit(() -> {
                    try {
                        CreateOrderUseCase.CreateOrderResult result = createOrderUseCase.createOrder(
                                UserId.of(loginId),
                                new CreateOrderUseCase.OrderCommand(
                                        List.of(new CreateOrderUseCase.OrderItemCommand(1L, 1)),
                                        "사용자", "서울시", "요청", "CARD",
                                        LocalDate.now().plusDays(3), null
                                )
                        );

                        requestPaymentUseCase.requestPayment(
                                UserId.of(loginId),
                                new RequestPaymentUseCase.PaymentCommand(
                                        result.orderId(), "VISA", "1234567890123456", result.paymentAmount()
                                )
                        );

                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        // 재고 부족 등 예외 발생 가능
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            executorService.shutdown();

            // then
            assertThat(successCount.get()).isEqualTo(10);

            // 모든 주문이 PAYMENT_PENDING 상태
            var orders = orderJpaRepository.findAll();
            assertThat(orders).hasSize(10);
            assertThat(orders).allMatch(o -> "PAYMENT_PENDING".equals(o.getStatus()));

            // 재고 정확성: 20 - 10 = 10
            Product product = productRepository.findById(1L).orElseThrow();
            assertThat(product.getStock().getValue()).isEqualTo(10);
        }
    }

    // ==========================================
    // 시나리오 2: 동일 주문에 콜백 중복 수신 (멱등성)
    // ==========================================
    @Nested
    @DisplayName("시나리오 2: 콜백 멱등성")
    class CallbackIdempotency {

        @Test
        @DisplayName("같은 주문에 SUCCESS 콜백이 5번 동시에 오면 상태 전이는 1번만 일어나야 한다")
        void duplicate_callbacks_should_transition_once() throws InterruptedException {
            // given
            Long orderId = createOrderDirectly("user0001");

            int threadCount = 5;
            ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger errorCount = new AtomicInteger(0);

            // when - 동일 orderId로 5번 동시 콜백
            for (int i = 0; i < threadCount; i++) {
                executorService.submit(() -> {
                    try {
                        paymentCallbackUseCase.handleCallback(
                                new PaymentCallbackUseCase.CallbackCommand(
                                        "txn0001", String.valueOf(orderId), "SUCCESS", null
                                )
                        );
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            executorService.shutdown();

            // then - 최종 상태는 PAYMENT_COMPLETED
            var orderEntity = orderJpaRepository.findById(orderId).orElseThrow();
            assertThat(orderEntity.getStatus()).isEqualTo("PAYMENT_COMPLETED");

            // 멱등성: 모든 호출이 에러 없이 완료 (이미 처리된 건은 무시)
            assertThat(successCount.get()).isEqualTo(5);
            assertThat(errorCount.get()).isEqualTo(0);
        }
    }

    // ==========================================
    // 시나리오 3: 결제 콜백 + 상태 조회 동시 접근
    // ==========================================
    @Nested
    @DisplayName("시나리오 3: 콜백과 상태 조회 동시 접근")
    class CallbackAndQueryRace {

        @Test
        @DisplayName("콜백 처리 중 상태 조회가 동시에 일어나도 데이터 정합성이 유지된다")
        void callback_and_query_concurrent_should_maintain_consistency() throws InterruptedException {
            // given
            registerUser("race0001", "Password1!", "경쟁테스트");
            Long orderId = createOrderDirectly("race0001");

            when(pgClient.getTransactionsByOrder(anyLong(), any())).thenReturn(
                    new PaymentGatewayResponse.ApiResponse<>("OK",
                            new PaymentGatewayResponse.Order(String.valueOf(orderId),
                                    List.of(new PaymentGatewayResponse.Transaction("txnrace1", "SUCCESS", null))))
            );

            int threadCount = 10;
            ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);

            // when - 5개 콜백 + 5개 상태 조회 동시 실행
            for (int i = 0; i < threadCount; i++) {
                final int index = i;
                executorService.submit(() -> {
                    try {
                        if (index < 5) {
                            paymentCallbackUseCase.handleCallback(
                                    new PaymentCallbackUseCase.CallbackCommand(
                                            "txnrace1", String.valueOf(orderId), "SUCCESS", null
                                    )
                            );
                        } else {
                            paymentQueryUseCase.getPaymentStatus(UserId.of("race0001"), orderId);
                        }
                    } catch (Exception e) {
                        // PENDING이 아닌 상태에서의 전이 시도 → 예외 발생 가능 (정상)
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            executorService.shutdown();

            // then - 최종 상태는 반드시 PAYMENT_COMPLETED
            var orderEntity = orderJpaRepository.findById(orderId).orElseThrow();
            assertThat(orderEntity.getStatus()).isEqualTo("PAYMENT_COMPLETED");
        }
    }

    // ==========================================
    // 시나리오 4: 결제 실패 콜백 → 재고 복원 동시성
    // ==========================================
    @Nested
    @DisplayName("시나리오 4: 동시 실패 콜백 시 재고 복원")
    class ConcurrentFailureRestoration {

        @Test
        @DisplayName("5개 주문이 동시에 실패 콜백을 받으면 재고가 정확히 복원되어야 한다")
        void concurrent_failure_callbacks_should_restore_stock_accurately() throws InterruptedException {
            // given - 5명 주문 생성 (재고 20 → 15)
            List<Long> orderIds = Collections.synchronizedList(new ArrayList<>());
            for (int i = 0; i < 5; i++) {
                String loginId = "fail" + String.format("%04d", i);
                registerUser(loginId, "Password1!", "실패사용자" + i);
                Long orderId = createOrderDirectly(loginId);
                orderIds.add(orderId);
            }

            Product beforeProduct = productRepository.findById(1L).orElseThrow();
            assertThat(beforeProduct.getStock().getValue()).isEqualTo(15);

            int threadCount = 5;
            ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);

            // when - 5건 동시 실패 콜백
            for (int i = 0; i < threadCount; i++) {
                Long orderId = orderIds.get(i);
                executorService.submit(() -> {
                    try {
                        paymentCallbackUseCase.handleCallback(
                                new PaymentCallbackUseCase.CallbackCommand(
                                        "txnfail" + orderId, String.valueOf(orderId), "FAILED", "카드 한도 초과"
                                )
                        );
                        successCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            executorService.shutdown();

            // then
            assertThat(successCount.get()).isEqualTo(5);

            for (Long orderId : orderIds) {
                var orderEntity = orderJpaRepository.findById(orderId).orElseThrow();
                assertThat(orderEntity.getStatus()).isEqualTo("PAYMENT_FAILED");
            }

            // 재고 복원: 15 + 5 = 20
            Product afterProduct = productRepository.findById(1L).orElseThrow();
            assertThat(afterProduct.getStock().getValue()).isEqualTo(20);
        }
    }

    // ==========================================
    // 시나리오 5: 서킷 브레이커 동작 검증
    // ==========================================
    @Nested
    @DisplayName("시나리오 5: 서킷 브레이커 동작")
    class CircuitBreakerBehavior {

        @Test
        @DisplayName("PG 장애 상황에서 결제 요청하면 fallback이 동작하고 PENDING 상태를 반환한다")
        void circuit_breaker_should_fallback_on_pg_failure() throws InterruptedException {
            // given - PG 호출 시 항상 예외
            when(pgClient.createPayment(anyLong(), any()))
                    .thenThrow(new feign.RetryableException(
                            503, "PG 서버 장애", feign.Request.HttpMethod.POST, (Throwable) null, (java.util.Date) null,
                            feign.Request.create(feign.Request.HttpMethod.POST, "/api/v1/payments",
                                    java.util.Collections.emptyMap(), null, null, null)));

            when(pgClient.getTransactionsByOrder(anyLong(), any()))
                    .thenThrow(new feign.RetryableException(
                            503, "PG 서버 장애", feign.Request.HttpMethod.GET, (Throwable) null, (java.util.Date) null,
                            feign.Request.create(feign.Request.HttpMethod.GET, "/api/v1/payments",
                                    java.util.Collections.emptyMap(), null, null, null)));

            int threadCount = 10;
            for (int i = 0; i < threadCount; i++) {
                registerUser("cb" + String.format("%06d", i), "Password1!", "서킷사용자" + i);
            }

            ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger pendingCount = new AtomicInteger(0);

            // when - PG 장애 상태에서 10건 동시 결제 요청
            for (int i = 0; i < threadCount; i++) {
                String loginId = "cb" + String.format("%06d", i);
                executorService.submit(() -> {
                    try {
                        CreateOrderUseCase.CreateOrderResult result = createOrderUseCase.createOrder(
                                UserId.of(loginId),
                                new CreateOrderUseCase.OrderCommand(
                                        List.of(new CreateOrderUseCase.OrderItemCommand(1L, 1)),
                                        "사용자", "서울시", "요청", "CARD",
                                        LocalDate.now().plusDays(3), null
                                )
                        );

                        RequestPaymentUseCase.PaymentResult paymentResult =
                                requestPaymentUseCase.requestPayment(
                                        UserId.of(loginId),
                                        new RequestPaymentUseCase.PaymentCommand(
                                                result.orderId(), "VISA", "1234567890123456", result.paymentAmount()
                                        )
                                );

                        if ("PENDING".equals(paymentResult.status())) {
                            pendingCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        // 재고 부족 등
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            executorService.shutdown();

            // then - fallback으로 PENDING 반환
            assertThat(pendingCount.get()).isGreaterThan(0);

            // 주문들은 PAYMENT_PENDING 상태 유지
            var orders = orderJpaRepository.findAll();
            assertThat(orders).allMatch(o -> "PAYMENT_PENDING".equals(o.getStatus()));
        }
    }

    // ==========================================
    // 헬퍼 메서드
    // ==========================================

    private Long createOrderDirectly(String loginId) {
        CreateOrderUseCase.CreateOrderResult result = createOrderUseCase.createOrder(
                UserId.of(loginId),
                new CreateOrderUseCase.OrderCommand(
                        List.of(new CreateOrderUseCase.OrderItemCommand(1L, 1)),
                        "사용자", "서울시", "요청", "CARD",
                        LocalDate.now().plusDays(3), null
                )
        );
        return result.orderId();
    }

    private HttpHeaders createAdminHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Loopers-Ldap", "loopers.admin");
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private void registerUser(String loginId, String password, String name) {
        var request = new UserRegisterRequest(loginId, password, name,
                LocalDate.of(1990, 5, 15), loginId + "@test.com");
        restTemplate.postForEntity("/api/v1/users", request, Void.class);
    }

    private void createBrand(String name, String description) {
        var request = new BrandCreateRequest(name, description);
        restTemplate.exchange("/api-admin/v1/brands", HttpMethod.POST,
                new HttpEntity<>(request, createAdminHeaders()), Void.class);
    }

    private void createProduct(Long brandId, String name, int price, int stock) {
        var request = new ProductCreateRequest(brandId, name, price, null, stock, "설명");
        restTemplate.exchange("/api-admin/v1/products", HttpMethod.POST,
                new HttpEntity<>(request, createAdminHeaders()), Void.class);
    }
}
