package com.loopers.concurrency;

import com.loopers.interfaces.api.brand.dto.BrandCreateRequest;
import com.loopers.interfaces.api.order.dto.OrderCreateRequest;
import com.loopers.interfaces.api.product.dto.ProductCreateRequest;
import com.loopers.interfaces.api.product.dto.ProductDetailResponse;
import com.loopers.interfaces.api.user.dto.UserRegisterRequest;
import com.loopers.testcontainers.PostgreSQLTestContainersConfig;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(PostgreSQLTestContainersConfig.class)
class StockConcurrencyTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @BeforeEach
    void setUp() {
        databaseCleanUp.truncateAllTables();
        createBrand("나이키", "스포츠");
        createProduct(1L, "운동화", 50000, 10);
    }

    @Test
    @DisplayName("재고 10개 상품에 10명이 동시에 1개씩 주문하면 재고가 정확히 0이어야 한다")
    void concurrent_orders_should_decrease_stock_accurately() throws InterruptedException {
        int threadCount = 10;

        for (int i = 0; i < threadCount; i++) {
            String loginId = "user" + String.format("%04d", i);
            registerUser(loginId, "Password1!", "사용자" + i);
        }

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            String loginId = "user" + String.format("%04d", i);
            executorService.submit(() -> {
                try {
                    HttpHeaders headers = createAuthHeaders(loginId, "Password1!");
                    headers.setContentType(MediaType.APPLICATION_JSON);

                    var orderRequest = new OrderCreateRequest(
                            List.of(new OrderCreateRequest.OrderItemRequest(1L, 1)),
                            "사용자", "서울시", "요청", "CARD", LocalDate.now().plusDays(3), null
                    );

                    ResponseEntity<Void> response = restTemplate.exchange(
                            "/api/v1/orders",
                            HttpMethod.POST,
                            new HttpEntity<>(orderRequest, headers),
                            Void.class
                    );

                    if (response.getStatusCode().is2xxSuccessful()) {
                        successCount.incrementAndGet();
                    } else {
                        failCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        ResponseEntity<ProductDetailResponse> productResponse = restTemplate.getForEntity(
                "/api/v1/products/1", ProductDetailResponse.class);

        // 성공한 주문 수 + 남은 재고 = 초기 재고 (10)
        int remainingStock = productResponse.getBody().stock();
        assertThat(successCount.get() + remainingStock).isEqualTo(10);
        assertThat(successCount.get()).isEqualTo(10);
        assertThat(remainingStock).isEqualTo(0);
    }

    @Test
    @DisplayName("재고 5개 상품에 10명이 동시 주문하면 5명만 성공한다")
    void concurrent_orders_exceed_stock_should_fail_properly() throws InterruptedException {
        databaseCleanUp.truncateAllTables();
        createBrand("나이키", "스포츠");
        createProduct(1L, "운동화", 50000, 5);

        int threadCount = 10;

        for (int i = 0; i < threadCount; i++) {
            String loginId = "user" + String.format("%04d", i);
            registerUser(loginId, "Password1!", "사용자" + i);
        }

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            String loginId = "user" + String.format("%04d", i);
            executorService.submit(() -> {
                try {
                    HttpHeaders headers = createAuthHeaders(loginId, "Password1!");
                    headers.setContentType(MediaType.APPLICATION_JSON);

                    var orderRequest = new OrderCreateRequest(
                            List.of(new OrderCreateRequest.OrderItemRequest(1L, 1)),
                            "사용자", "서울시", "요청", "CARD", LocalDate.now().plusDays(3), null
                    );

                    ResponseEntity<Void> response = restTemplate.exchange(
                            "/api/v1/orders",
                            HttpMethod.POST,
                            new HttpEntity<>(orderRequest, headers),
                            Void.class
                    );

                    if (response.getStatusCode().is2xxSuccessful()) {
                        successCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        ResponseEntity<ProductDetailResponse> productResponse = restTemplate.getForEntity(
                "/api/v1/products/1", ProductDetailResponse.class);

        assertThat(successCount.get()).isEqualTo(5);
        assertThat(productResponse.getBody().stock()).isEqualTo(0);
    }

    private HttpHeaders createAuthHeaders(String loginId, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Loopers-LoginId", loginId);
        headers.set("X-Loopers-LoginPw", password);
        return headers;
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
