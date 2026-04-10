package com.loopers.concurrency;

import com.loopers.confg.kafka.KafkaTopics;
import com.loopers.infrastructure.coupon.CouponIssueRequestEntity;
import com.loopers.infrastructure.coupon.CouponIssueRequestRepository;
import com.loopers.infrastructure.coupon.UserCouponEntityRepository;
import com.loopers.infrastructure.idempotency.EventHandledJpaRepository;
import com.loopers.testcontainers.KafkaTestContainersConfig;
import com.loopers.testcontainers.PostgreSQLTestContainersConfig;
import com.loopers.testcontainers.RedisTestContainersConfig;
import com.loopers.utils.DatabaseCleanUp;
import com.loopers.utils.RedisCleanUp;
import jakarta.persistence.EntityManager;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@ActiveProfiles("test")
@Import({PostgreSQLTestContainersConfig.class, RedisTestContainersConfig.class, KafkaTestContainersConfig.class})
class CouponIssueConcurrencyTest {

    private static final String TOPIC = KafkaTopics.COUPON_ISSUE_REQUESTS;
    private static final long COUPON_ID = 1L;
    private static final int MAX_ISSUANCE = 100;
    private static final int TOTAL_REQUESTS = 200;

    @Autowired
    private UserCouponEntityRepository userCouponRepository;

    @Autowired
    private CouponIssueRequestRepository issueRequestRepository;

    @Autowired
    private EventHandledJpaRepository eventHandledRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @Autowired
    private RedisCleanUp redisCleanUp;

    @Autowired
    private EntityManager entityManager;

    private KafkaTemplate<String, Object> kafkaTemplate;

    @BeforeEach
    void setUp() {
        databaseCleanUp.truncateAllTables();
        redisCleanUp.truncateAll();
        initKafkaTemplate();
        createTopicIfNotExists();
    }

    @Test
    @DisplayName("200명이 선착순 100명 쿠폰에 동시 요청하면 정확히 100명만 발급된다")
    void concurrentIssue_onlyMaxIssuanceSucceed() {
        // given: 200건의 발급 요청 데이터 생성
        List<Long> requestIds = insertIssueRequests(TOTAL_REQUESTS, /*sameUser=*/false);

        // when: 200건의 Kafka 메시지 발행
        for (int i = 0; i < TOTAL_REQUESTS; i++) {
            long eventId = i + 1;
            String userId = "user" + String.format("%04d", i);

            Map<String, Object> message = Map.of(
                    "eventId", eventId,
                    "requestId", requestIds.get(i),
                    "couponId", COUPON_ID,
                    "userId", userId,
                    "maxIssuance", MAX_ISSUANCE
            );
            kafkaTemplate.send(TOPIC, String.valueOf(COUPON_ID), message);
        }

        // then: Consumer가 처리 완료될 때까지 대기 후 검증
        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            long handledCount = eventHandledRepository.count();
            assertThat(handledCount).isEqualTo(TOTAL_REQUESTS);
        });

        long issuedCount = userCouponRepository.count();
        assertThat(issuedCount).isEqualTo(MAX_ISSUANCE);

        long successCount = issueRequestRepository.findAll().stream()
                .filter(r -> CouponIssueRequestEntity.STATUS_SUCCESS.equals(r.getStatus()))
                .count();
        long rejectedCount = issueRequestRepository.findAll().stream()
                .filter(r -> CouponIssueRequestEntity.STATUS_REJECTED.equals(r.getStatus()))
                .count();
        assertThat(successCount).isEqualTo(MAX_ISSUANCE);
        assertThat(rejectedCount).isEqualTo(TOTAL_REQUESTS - MAX_ISSUANCE);
    }

    @Test
    @DisplayName("같은 유저가 여러 번 요청하면 1번만 발급된다")
    void duplicateRequest_onlyOneIssued() {
        // given: 같은 유저로 5건의 발급 요청
        int duplicateCount = 5;
        String sameUserId = "user0001";
        List<Long> requestIds = insertIssueRequestsForUser(duplicateCount, sameUserId);

        // when: 5건의 Kafka 메시지 발행 (각각 다른 eventId)
        for (int i = 0; i < duplicateCount; i++) {
            long eventId = 1000L + i;
            Map<String, Object> message = Map.of(
                    "eventId", eventId,
                    "requestId", requestIds.get(i),
                    "couponId", COUPON_ID,
                    "userId", sameUserId,
                    "maxIssuance", MAX_ISSUANCE
            );
            kafkaTemplate.send(TOPIC, String.valueOf(COUPON_ID), message);
        }

        // then
        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            long handledCount = eventHandledRepository.count();
            assertThat(handledCount).isEqualTo(duplicateCount);
        });

        long issuedCount = userCouponRepository.countByCouponIdAndUserId(COUPON_ID, sameUserId);
        assertThat(issuedCount).isEqualTo(1);
    }

    @Test
    @DisplayName("수량 소진 후 추가 요청은 전부 거절된다")
    void afterExhausted_allRejected() {
        // given: 먼저 100명 발급 완료
        int firstBatch = MAX_ISSUANCE;
        List<Long> firstRequestIds = insertIssueRequests(firstBatch, /*sameUser=*/false);

        for (int i = 0; i < firstBatch; i++) {
            long eventId = i + 1;
            String userId = "user" + String.format("%04d", i);
            Map<String, Object> message = Map.of(
                    "eventId", eventId,
                    "requestId", firstRequestIds.get(i),
                    "couponId", COUPON_ID,
                    "userId", userId,
                    "maxIssuance", MAX_ISSUANCE
            );
            kafkaTemplate.send(TOPIC, String.valueOf(COUPON_ID), message);
        }

        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            long handledCount = eventHandledRepository.count();
            assertThat(handledCount).isEqualTo(firstBatch);
        });

        assertThat(userCouponRepository.count()).isEqualTo(MAX_ISSUANCE);

        // when: 추가 50명 요청
        int extraBatch = 50;
        List<Long> extraRequestIds = insertIssueRequests(extraBatch, /*sameUser=*/false, firstBatch);

        for (int i = 0; i < extraBatch; i++) {
            long eventId = firstBatch + i + 1;
            String userId = "extra" + String.format("%04d", i);
            Map<String, Object> message = Map.of(
                    "eventId", eventId,
                    "requestId", extraRequestIds.get(i),
                    "couponId", COUPON_ID,
                    "userId", userId,
                    "maxIssuance", MAX_ISSUANCE
            );
            kafkaTemplate.send(TOPIC, String.valueOf(COUPON_ID), message);
        }

        // then: 추가 요청은 전부 거절
        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            long handledCount = eventHandledRepository.count();
            assertThat(handledCount).isEqualTo(firstBatch + extraBatch);
        });

        assertThat(userCouponRepository.count()).isEqualTo(MAX_ISSUANCE);

        long extraRejected = issueRequestRepository.findAll().stream()
                .filter(r -> CouponIssueRequestEntity.STATUS_REJECTED.equals(r.getStatus()))
                .filter(r -> r.getUserId().startsWith("extra"))
                .count();
        assertThat(extraRejected).isEqualTo(extraBatch);
    }

    @Transactional
    List<Long> insertIssueRequests(int count, boolean sameUser) {
        return insertIssueRequests(count, sameUser, 0);
    }

    @Transactional
    List<Long> insertIssueRequests(int count, boolean sameUser, int startIndex) {
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String userId = sameUser ? "user0001" : "user" + String.format("%04d", startIndex + i);
            Long id = (Long) entityManager.createNativeQuery(
                            "INSERT INTO coupon_issue_requests (coupon_id, user_id, status, created_at) " +
                                    "VALUES (:couponId, :userId, 'PENDING', :createdAt) RETURNING id")
                    .setParameter("couponId", COUPON_ID)
                    .setParameter("userId", userId)
                    .setParameter("createdAt", LocalDateTime.now())
                    .getSingleResult();
            ids.add(id);
        }
        return ids;
    }

    @Transactional
    List<Long> insertIssueRequestsForUser(int count, String userId) {
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Long id = (Long) entityManager.createNativeQuery(
                            "INSERT INTO coupon_issue_requests (coupon_id, user_id, status, created_at) " +
                                    "VALUES (:couponId, :userId, 'PENDING', :createdAt) RETURNING id")
                    .setParameter("couponId", COUPON_ID)
                    .setParameter("userId", userId)
                    .setParameter("createdAt", LocalDateTime.now())
                    .getSingleResult();
            ids.add(id);
        }
        return ids;
    }

    private void initKafkaTemplate() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, System.getProperty("BOOTSTRAP_SERVERS"));
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        DefaultKafkaProducerFactory<String, Object> factory = new DefaultKafkaProducerFactory<>(props);
        this.kafkaTemplate = new KafkaTemplate<>(factory);
    }

    private void createTopicIfNotExists() {
        Map<String, Object> config = Map.of(
                "bootstrap.servers", System.getProperty("BOOTSTRAP_SERVERS")
        );
        try (AdminClient admin = AdminClient.create(config)) {
            Set<String> existingTopics = admin.listTopics().names().get(5, TimeUnit.SECONDS);
            if (!existingTopics.contains(TOPIC)) {
                admin.createTopics(List.of(new NewTopic(TOPIC, 1, (short) 1)))
                        .all().get(5, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            throw new RuntimeException("토픽 생성 실패", e);
        }
    }
}
