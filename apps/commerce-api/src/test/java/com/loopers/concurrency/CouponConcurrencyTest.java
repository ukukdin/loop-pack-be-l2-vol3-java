package com.loopers.concurrency;

import com.loopers.domain.model.coupon.*;
import com.loopers.domain.model.userCoupon.UserCoupon;
import com.loopers.domain.model.user.UserId;
import com.loopers.domain.repository.CouponRepository;
import com.loopers.domain.repository.UserCouponRepository;
import com.loopers.interfaces.api.brand.dto.BrandCreateRequest;
import com.loopers.interfaces.api.order.dto.OrderCreateRequest;
import com.loopers.interfaces.api.product.dto.ProductCreateRequest;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(PostgreSQLTestContainersConfig.class)
class CouponConcurrencyTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private UserCouponRepository userCouponRepository;

    private static final String LOGIN_ID = "testuser1";
    private static final String PASSWORD = "Password1!";

    @BeforeEach
    void setUp() {
        databaseCleanUp.truncateAllTables();
        registerUser(LOGIN_ID, PASSWORD, "홍길동");
        createBrand("나이키", "스포츠");
        createProduct(1L, "운동화", 50000, 100);
    }

    @Test
    @DisplayName("동일 쿠폰으로 여러 기기에서 동시 주문 시 쿠폰은 1번만 사용되어야 한다")
    void concurrent_order_with_same_coupon_should_use_coupon_once() throws InterruptedException {
        // given - 쿠폰 생성 및 발급
        Coupon coupon = createCoupon();
        UserCoupon userCoupon = issueUserCoupon(coupon.getId(), UserId.of(LOGIN_ID));

        int threadCount = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when - 동일 쿠폰으로 5개 기기에서 동시 주문
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    HttpHeaders headers = createAuthHeaders(LOGIN_ID, PASSWORD);
                    headers.setContentType(MediaType.APPLICATION_JSON);

                    var orderRequest = new OrderCreateRequest(
                            List.of(new OrderCreateRequest.OrderItemRequest(1L, 1)),
                            "홍길동", "서울시", "요청", "CARD",
                            LocalDate.now().plusDays(3), userCoupon.getId()
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

        // then - 쿠폰은 1번만 사용
        assertThat(successCount.get()).isEqualTo(1);
    }

    private Coupon createCoupon() {
        DiscountPolicy discountPolicy = DiscountPolicy.create(
                DiscountType.FIXED, BigDecimal.valueOf(5000), null, BigDecimal.ZERO);
        IssuancePolicy issuancePolicy = IssuancePolicy.create(null, null, null, null);
        CouponTarget couponTarget = CouponTarget.create(
                TargetType.ALL, Collections.emptyList());

        Coupon coupon = Coupon.create("TESTCODE1", "테스트 쿠폰", "설명",
                discountPolicy, issuancePolicy,
                LocalDateTime.now().plusDays(30), couponTarget, false);

        return couponRepository.save(coupon);
    }

    private UserCoupon issueUserCoupon(Long couponId, UserId userId) {
        UserCoupon userCoupon = UserCoupon.issue(couponId, userId);
        return userCouponRepository.save(userCoupon);
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
