package com.loopers.concurrency;

import com.loopers.application.queue.EnterQueueUseCase;
import com.loopers.application.queue.QueueService;
import com.loopers.application.queue.QueryPositionUseCase;
import com.loopers.domain.model.queue.QueuePosition;
import com.loopers.domain.model.user.UserId;
import com.loopers.domain.repository.EntryTokenRepository;
import com.loopers.domain.repository.WaitingQueueRepository;
import com.loopers.testcontainers.PostgreSQLTestContainersConfig;
import com.loopers.testcontainers.RedisTestContainersConfig;
import com.loopers.utils.DatabaseCleanUp;
import com.loopers.utils.RedisCleanUp;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({PostgreSQLTestContainersConfig.class, RedisTestContainersConfig.class})
class QueueConcurrencyTest {

    @Autowired private QueueService queueService;
    @Autowired private WaitingQueueRepository waitingQueueRepository;
    @Autowired private EntryTokenRepository entryTokenRepository;
    @Autowired private DatabaseCleanUp databaseCleanUp;
    @Autowired private RedisCleanUp redisCleanUp;

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
                        // ignore
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            readyLatch.await();
            startLatch.countDown(); // 동시 출발
            doneLatch.await(30, TimeUnit.SECONDS);
            executor.shutdown();

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
                        // ignore
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            readyLatch.await();
            startLatch.countDown();
            doneLatch.await(10, TimeUnit.SECONDS);
            executor.shutdown();

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

            // 순번 조회 시 토큰이 포함되어야 함
            QueuePosition position = queueService.getPosition(firstUser);
            assertThat(position.isReady()).isTrue();
            assertThat(position.getEntryToken()).isNotNull();
        }

        @Test
        @DisplayName("토큰을 수동 삭제하면 순번 조회 시 QUEUE_NOT_FOUND 예외가 발생한다")
        void deleted_token_and_not_in_queue_should_throw() {
            UserId userId = UserId.of("del00001");
            queueService.enter(userId);

            // 토큰 발급
            queueService.issueTokens();
            assertThat(entryTokenRepository.exists(userId)).isTrue();

            // 토큰 소비 (주문 완료 시뮬레이션)
            entryTokenRepository.delete(userId);
            assertThat(entryTokenRepository.exists(userId)).isFalse();

            // 대기열에도 없고 토큰도 없으면 → QUEUE_NOT_FOUND
            org.junit.jupiter.api.Assertions.assertThrows(
                    com.loopers.support.error.CoreException.class,
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
                        // ignore
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
                        // ignore
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            readyLatch.await();
            startLatch.countDown();
            doneLatch.await(30, TimeUnit.SECONDS);
            executor.shutdown();

            // 정합성 검증: 대기열 잔여 + 토큰 발급 수 = 진입 성공 수
            long remainingInQueue = waitingQueueRepository.getTotalSize();
            assertThat(remainingInQueue + totalIssued.get()).isEqualTo(entrySuccess.get());
        }
    }
}
