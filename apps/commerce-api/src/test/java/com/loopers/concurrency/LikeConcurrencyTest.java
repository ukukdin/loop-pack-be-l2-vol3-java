package com.loopers.concurrency;

import com.loopers.interfaces.api.brand.dto.BrandCreateRequest;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(PostgreSQLTestContainersConfig.class)
class LikeConcurrencyTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @BeforeEach
    void setUp() {
        databaseCleanUp.truncateAllTables();
        createBrand("나이키", "스포츠");
        createProduct(1L, "운동화", 50000, 100);
    }

    @Test
    @DisplayName("동시에 10명이 좋아요를 누르면 likeCount가 정확히 10이어야 한다")
    void concurrent_like_count_should_be_accurate() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            String loginId = "user" + String.format("%04d", i);
            registerUser(loginId, "Password1!", "사용자" + i);
        }

        for (int i = 0; i < threadCount; i++) {
            String loginId = "user" + String.format("%04d", i);
            executorService.submit(() -> {
                try {
                    HttpHeaders headers = createAuthHeaders(loginId, "Password1!");
                    ResponseEntity<Void> response = restTemplate.exchange(
                            "/api/v1/products/1/likes",
                            HttpMethod.POST,
                            new HttpEntity<>(headers),
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

        assertThat(productResponse.getBody().likeCount()).isEqualTo(successCount.get());
    }

    @Test
    @DisplayName("동시에 5명이 좋아요, 5명이 좋아요 취소하면 정확한 likeCount 반영")
    void concurrent_like_and_unlike_should_be_accurate() throws InterruptedException {
        // 먼저 10명이 좋아요를 등록
        for (int i = 0; i < 10; i++) {
            String loginId = "user" + String.format("%04d", i);
            registerUser(loginId, "Password1!", "사용자" + i);
            HttpHeaders headers = createAuthHeaders(loginId, "Password1!");
            restTemplate.exchange("/api/v1/products/1/likes",
                    HttpMethod.POST, new HttpEntity<>(headers), Void.class);
        }

        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // 5명은 좋아요 취소, 5명은 새로 좋아요
        for (int i = 0; i < 5; i++) {
            String loginId = "user" + String.format("%04d", i);
            executorService.submit(() -> {
                try {
                    HttpHeaders headers = createAuthHeaders(loginId, "Password1!");
                    restTemplate.exchange("/api/v1/products/1/likes",
                            HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);
                } finally {
                    latch.countDown();
                }
            });
        }

        for (int i = 10; i < 15; i++) {
            String loginId = "user" + String.format("%04d", i);
            registerUser(loginId, "Password1!", "사용자" + i);
        }

        for (int i = 10; i < 15; i++) {
            String loginId = "user" + String.format("%04d", i);
            executorService.submit(() -> {
                try {
                    HttpHeaders headers = createAuthHeaders(loginId, "Password1!");
                    restTemplate.exchange("/api/v1/products/1/likes",
                            HttpMethod.POST, new HttpEntity<>(headers), Void.class);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        ResponseEntity<ProductDetailResponse> productResponse = restTemplate.getForEntity(
                "/api/v1/products/1", ProductDetailResponse.class);

        // 10명 좋아요 - 5명 취소 + 5명 새 좋아요 = 10
        assertThat(productResponse.getBody().likeCount()).isEqualTo(10);
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
