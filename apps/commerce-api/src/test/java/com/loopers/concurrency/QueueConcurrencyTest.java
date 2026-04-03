package com.loopers.concurrency;

import com.loopers.application.queue.EnterQueueUseCase;
import com.loopers.application.queue.QueueService;
import com.loopers.application.queue.QueryPositionUseCase;
import com.loopers.domain.model.queue.QueuePosition;
import com.loopers.domain.model.user.UserId;
import com.loopers.domain.repository.EntryTokenRepository;
import com.loopers.domain.repository.WaitingQueueRepository;
import com.loopers.support.error.CoreException;
import com.loopers.testcontainers.PostgreSQLTestContainersConfig;
import com.loopers.testcontainers.RedisTestContainersConfig;
import com.loopers.utils.DatabaseCleanUp;
import com.loopers.utils.RedisCleanUp;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "queue.token-ttl-seconds=2",           // TTL 테스트를 위해 2초로 설정
                "queue.scheduler.interval-ms=3600000"   // 스케줄러 간섭 방지 (1시간)
        }
)
@Import({PostgreSQLTestContainersConfig.class, RedisTestContainersConfig.class})
class QueueConcurrencyTest {

    @Autowired private QueueService queueService;
    @Autowired private WaitingQueueRepository waitingQueueRepository;
    @Autowired private EntryTokenRepository entryTokenRepository;
    @Autowired private DatabaseCleanUp databaseCleanUp;
    @Autowired private RedisCleanUp redisCleanUp;
    @Autowired private RedisTemplate<String, String> redisTemplate;

    @BeforeEach
    void setUp() {
        databaseCleanUp.truncateAllTables();
        redisCleanUp.truncateAll();
    }

    // ==========================================
    // 시나리오 1: 동시 진입 — 대기열 순서 보장
    // ==========================================
    @Nested
    @DisplayName("시나리오 1: 동시 진입 시 대기열 순서 보장")
    class ConcurrentQueueEntry {

        @Test
        @DisplayName("50명이 동시에 대기열에 진입하면 모두 고유한 순번을 받아야 한다")
        void concurrent_enter_should_assign_unique_positions() throws InterruptedException {
            int threadCount = 50;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch readyLatch = new CountDownLatch(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);

            Set<Long> ranks = Collections.synchronizedSet(new HashSet<>());
            AtomicInteger successCount = new AtomicInteger(0);
            ConcurrentLinkedQueue<Throwable> errors = new ConcurrentLinkedQueue<>();

            for (int i = 0; i < threadCount; i++) {
                String loginId = String.format("user%04d", i);
                executor.submit(() -> {
                    try {
                        readyLatch.countDown();
                        startLatch.await(); // 모든 스레드가 동시에 시작
                        EnterQueueUseCase.EnterQueueResult result =
                                queueService.enter(UserId.of(loginId));
                        ranks.add(result.rank());
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        errors.add(e);
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            readyLatch.await();
            startLatch.countDown(); // 동시 출발
            assertThat(doneLatch.await(30, TimeUnit.SECONDS)).isTrue();
            executor.shutdown();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();

            // 예외가 발생하지 않아야 한다
            assertThat(errors).isEmpty();
            // 50명 모두 성공
            assertThat(successCount.get()).isEqualTo(threadCount);
            // 모든 순번이 고유
            assertThat(ranks).hasSize(threadCount);
            // Redis Sorted Set에 정확히 50명
            assertThat(waitingQueueRepository.getTotalSize()).isEqualTo(threadCount);
        }

        @Test
        @DisplayName("같은 유저가 동시에 10번 진입해도 대기열에는 1건만 존재한다")
        void duplicate_concurrent_enter_should_store_once() throws InterruptedException {
            int threadCount = 10;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch readyLatch = new CountDownLatch(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);

            UserId userId = UserId.of("dupl0001");
            AtomicInteger successCount = new AtomicInteger(0);

            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        readyLatch.countDown();
                        startLatch.await();
                        queueService.enter(userId);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        // 중복 진입은 예외 없이 기존 rank를 반환하므로 성공으로 간주
                        successCount.incrementAndGet();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            readyLatch.await();
            startLatch.countDown();
            assertThat(doneLatch.await(10, TimeUnit.SECONDS)).isTrue();
            executor.shutdown();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();

            // Sorted Set 특성: member가 동일하면 1건만 저장
            assertThat(waitingQueueRepository.getTotalSize()).isEqualTo(1);
            assertThat(waitingQueueRepository.exists(userId)).isTrue();
        }
    }

    // ==========================================
    // 시나리오 2: 토큰 만료 — TTL 검증
    // ==========================================
    @Nested
    @DisplayName("시나리오 2: 토큰 TTL 만료 검증")
    class TokenExpiry {

        @Test
        @DisplayName("대기열에서 토큰이 발급된 후 TTL이 지나면 토큰이 사라진다")
        void token_should_expire_after_ttl() throws InterruptedException {
            // 5명 대기열 진입
            for (int i = 0; i < 5; i++) {
                queueService.enter(UserId.of(String.format("ttl%05d", i)));
            }
            assertThat(waitingQueueRepository.getTotalSize()).isEqualTo(5);

            // 스케줄러 수동 실행 → 토큰 발급
            int issued = queueService.issueTokens();
            assertThat(issued).isEqualTo(5);
            assertThat(waitingQueueRepository.getTotalSize()).isEqualTo(0);

            // 토큰 존재 확인
            UserId firstUser = UserId.of("ttl00000");
            assertThat(entryTokenRepository.exists(firstUser)).isTrue();

            // 발급 직후 TTL이 양수인지 확인
            Long ttl = redisTemplate.getExpire("entry-token:" + firstUser.getValue());
            assertThat(ttl).isNotNull().isPositive();

            // 순번 조회 시 토큰이 포함되어야 함
            QueuePosition position = queueService.getPosition(firstUser);
            assertThat(position.isReady()).isTrue();
            assertThat(position.getEntryToken()).isNotNull();

            // TTL 만료 대기 (테스트용 TTL = 2초)
            Thread.sleep(2500);

            // 만료 후 토큰이 사라져야 한다
            assertThat(entryTokenRepository.exists(firstUser)).isFalse();

            // 대기열에도 없고 토큰도 없으면 → QUEUE_NOT_FOUND
            Assertions.assertThrows(
                    CoreException.class,
                    () -> queueService.getPosition(firstUser)
            );
        }

        @Test
        @DisplayName("토큰을 소비(consume)하면 순번 조회 시 QUEUE_NOT_FOUND 예외가 발생한다")
        void consumed_token_should_throw_not_found() {
            UserId userId = UserId.of("del00001");
            queueService.enter(userId);

            // 토큰 발급
            queueService.issueTokens();
            assertThat(entryTokenRepository.exists(userId)).isTrue();

            // 토큰 조회 후 소비
            String token = entryTokenRepository.findByUserId(userId).orElseThrow();
            queueService.consume(userId, token);
            assertThat(entryTokenRepository.exists(userId)).isFalse();

            // 대기열에도 없고 토큰도 없으면 → QUEUE_NOT_FOUND
            Assertions.assertThrows(
                    CoreException.class,
                    () -> queueService.getPosition(userId)
            );
        }
    }

    // ==========================================
    // 시나리오 3: 처리량 초과 — 시스템 안정성
    // ==========================================
    @Nested
    @DisplayName("시나리오 3: 처리량 초과 시 시스템 안정성")
    class ThroughputExceed {

        @Test
        @DisplayName("배치 크기(18) 이상 대기 중이어도 스케줄러는 정확히 배치 크기만큼만 발급한다")
        void scheduler_should_issue_exactly_batch_size() {
            // 100명 대기열 진입
            for (int i = 0; i < 100; i++) {
                queueService.enter(UserId.of(String.format("bat%05d", i)));
            }
            assertThat(waitingQueueRepository.getTotalSize()).isEqualTo(100);

            // 1회 스케줄러 실행 → 배치 크기(18)만큼만 발급
            int issued = queueService.issueTokens();
            assertThat(issued).isEqualTo(18);

            // 대기열에 82명 남아있어야 함
            assertThat(waitingQueueRepository.getTotalSize()).isEqualTo(82);

            // 발급된 18명은 토큰을 보유
            for (int i = 0; i < 18; i++) {
                assertThat(entryTokenRepository.exists(
                        UserId.of(String.format("bat%05d", i)))).isTrue();
            }

            // 대기열에 남은 유저는 토큰 미보유
            for (int i = 18; i < 100; i++) {
                assertThat(entryTokenRepository.exists(
                        UserId.of(String.format("bat%05d", i)))).isFalse();
            }
        }

        @Test
        @DisplayName("스케줄러를 여러 번 실행하면 순차적으로 모든 대기자에게 토큰이 발급된다")
        void multiple_scheduler_runs_should_drain_queue() {
            // 50명 진입
            for (int i = 0; i < 50; i++) {
                queueService.enter(UserId.of(String.format("drn%05d", i)));
            }

            int totalIssued = 0;
            int round = 0;

            // 대기열이 빌 때까지 스케줄러 반복 실행
            while (waitingQueueRepository.getTotalSize() > 0) {
                int issued = queueService.issueTokens();
                totalIssued += issued;
                round++;
                assertThat(round).isLessThan(10); // 무한루프 방지
            }

            // 50명 모두 토큰 발급 완료
            assertThat(totalIssued).isEqualTo(50);
            assertThat(waitingQueueRepository.getTotalSize()).isEqualTo(0);

            // 모든 유저가 토큰 보유
            for (int i = 0; i < 50; i++) {
                assertThat(entryTokenRepository.exists(
                        UserId.of(String.format("drn%05d", i)))).isTrue();
            }
        }

        @Test
        @DisplayName("50명이 동시 진입 + 스케줄러 동시 실행 시 데이터 정합성이 유지된다")
        void concurrent_enter_and_scheduler_should_maintain_consistency() throws InterruptedException {
            int entryThreads = 50;
            int schedulerRuns = 5;
            ExecutorService executor = Executors.newFixedThreadPool(entryThreads + schedulerRuns);
            CountDownLatch readyLatch = new CountDownLatch(entryThreads + schedulerRuns);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(entryThreads + schedulerRuns);

            AtomicInteger entrySuccess = new AtomicInteger(0);
            AtomicInteger totalIssued = new AtomicInteger(0);
            ConcurrentLinkedQueue<Throwable> errors = new ConcurrentLinkedQueue<>();

            // 50명 동시 진입
            for (int i = 0; i < entryThreads; i++) {
                String loginId = String.format("mix%05d", i);
                executor.submit(() -> {
                    try {
                        readyLatch.countDown();
                        startLatch.await();
                        queueService.enter(UserId.of(loginId));
                        entrySuccess.incrementAndGet();
                    } catch (Exception e) {
                        errors.add(e);
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            // 스케줄러 5회 동시 실행
            for (int i = 0; i < schedulerRuns; i++) {
                executor.submit(() -> {
                    try {
                        readyLatch.countDown();
                        startLatch.await();
                        int issued = queueService.issueTokens();
                        totalIssued.addAndGet(issued);
                    } catch (Exception e) {
                        errors.add(e);
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            readyLatch.await();
            startLatch.countDown();
            assertThat(doneLatch.await(30, TimeUnit.SECONDS)).isTrue();
            executor.shutdown();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();

            // 예외가 발생하지 않아야 한다
            assertThat(errors).isEmpty();
            // 정합성 검증: 대기열 잔여 + 토큰 발급 수 = 진입 성공 수
            long remainingInQueue = waitingQueueRepository.getTotalSize();
            assertThat(remainingInQueue + totalIssued.get()).isEqualTo(entrySuccess.get());
        }
    }

    // ==========================================
    // 시나리오 4: 토큰 동시 소비 — Double-Spend 방지
    // ==========================================
    @Nested
    @DisplayName("시나리오 4: 동일 토큰 동시 소비 시 정확히 하나만 성공")
    class ConcurrentTokenConsumption {

        @Test
        @DisplayName("동일 토큰으로 동시에 10번 소비해도 정확히 1번만 성공한다")
        void concurrent_consume_should_succeed_exactly_once() throws InterruptedException {
            // 토큰 발급
            UserId userId = UserId.of("consume001");
            queueService.enter(userId);
            queueService.issueTokens();
            String token = entryTokenRepository.findByUserId(userId).orElseThrow();

            int threadCount = 10;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch readyLatch = new CountDownLatch(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);

            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failCount = new AtomicInteger(0);

            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        readyLatch.countDown();
                        startLatch.await();
                        queueService.consume(userId, token);
                        successCount.incrementAndGet();
                    } catch (CoreException e) {
                        failCount.incrementAndGet();
                    } catch (Exception e) {
                        // 예상치 못한 예외
                        failCount.incrementAndGet();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            readyLatch.await();
            startLatch.countDown();
            assertThat(doneLatch.await(10, TimeUnit.SECONDS)).isTrue();
            executor.shutdown();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();

            // 정확히 1번만 성공
            assertThat(successCount.get()).isEqualTo(1);
            assertThat(failCount.get()).isEqualTo(threadCount - 1);
            // 토큰이 삭제되었는지 확인
            assertThat(entryTokenRepository.exists(userId)).isFalse();
        }
    }
}
