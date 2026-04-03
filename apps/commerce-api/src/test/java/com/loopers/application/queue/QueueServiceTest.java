package com.loopers.application.queue;

import com.loopers.domain.model.queue.QueuePosition;
import com.loopers.domain.model.queue.QueueProperties;
import com.loopers.domain.model.user.UserId;
import com.loopers.domain.repository.EntryTokenRepository;
import com.loopers.domain.repository.WaitingQueueRepository;
import com.loopers.domain.repository.WaitingQueueRepository.IssuedToken;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class QueueServiceTest {

    private WaitingQueueRepository waitingQueueRepository;
    private EntryTokenRepository entryTokenRepository;
    private QueueProperties queueProperties;
    private QueueService queueService;

    @BeforeEach
    void setUp() {
        waitingQueueRepository = mock(WaitingQueueRepository.class);
        entryTokenRepository = mock(EntryTokenRepository.class);
        queueProperties = mock(QueueProperties.class);
        queueService = new QueueService(waitingQueueRepository, entryTokenRepository, queueProperties);

        when(queueProperties.getBatchSize()).thenReturn(18);
        when(queueProperties.getTokenTtlSeconds()).thenReturn(300L);
        when(queueProperties.getThroughputPerSecond()).thenReturn(175L);
        when(queueProperties.getMaxQueueSize()).thenReturn(100_000L);
    }

    @Nested
    @DisplayName("대기열 진입")
    class EnterQueue {

        @Test
        @DisplayName("대기열 진입 성공 - Lua 스크립트로 원자적 진입 후 순번을 반환한다")
        void enter_success() {
            UserId userId = UserId.of("user0001");
            when(entryTokenRepository.exists(userId)).thenReturn(false);
            when(waitingQueueRepository.enterAtomically(userId, 100_000L)).thenReturn(50L);
            when(waitingQueueRepository.getTotalSize()).thenReturn(51L);

            EnterQueueUseCase.EnterQueueResult result = queueService.enter(userId);

            assertThat(result.rank()).isEqualTo(51); // 0-based → 1-based
            verify(waitingQueueRepository).enterAtomically(userId, 100_000L);
        }

        @Test
        @DisplayName("이미 토큰을 보유한 유저는 대기열 진입 시 예외가 발생한다")
        void enter_fail_already_has_token() {
            UserId userId = UserId.of("user0001");
            when(entryTokenRepository.exists(userId)).thenReturn(true);

            assertThatThrownBy(() -> queueService.enter(userId))
                    .isInstanceOf(CoreException.class)
                    .satisfies(e -> assertThat(((CoreException) e).getErrorType())
                            .isEqualTo(ErrorType.QUEUE_ALREADY_HAS_TOKEN));
        }

        @Test
        @DisplayName("대기열이 가득 찬 경우 진입이 거부된다 (Lua 스크립트가 -1 반환)")
        void enter_fail_queue_full() {
            UserId userId = UserId.of("user0001");
            when(entryTokenRepository.exists(userId)).thenReturn(false);
            when(waitingQueueRepository.enterAtomically(userId, 100_000L)).thenReturn(-1L);

            assertThatThrownBy(() -> queueService.enter(userId))
                    .isInstanceOf(CoreException.class)
                    .satisfies(e -> assertThat(((CoreException) e).getErrorType())
                            .isEqualTo(ErrorType.QUEUE_FULL));
        }

        @Test
        @DisplayName("중복 진입 시 기존 순번을 반환한다 (Lua 스크립트가 기존 rank 반환)")
        void enter_duplicate_returns_existing_rank() {
            UserId userId = UserId.of("user0001");
            when(entryTokenRepository.exists(userId)).thenReturn(false);
            when(waitingQueueRepository.enterAtomically(userId, 100_000L)).thenReturn(10L);
            when(waitingQueueRepository.getTotalSize()).thenReturn(50L);

            EnterQueueUseCase.EnterQueueResult result = queueService.enter(userId);

            assertThat(result.rank()).isEqualTo(11);
        }
    }

    @Nested
    @DisplayName("동시 진입 테스트")
    class ConcurrentEntry {

        @Test
        @DisplayName("중복 유저가 진입 시도하면 Lua 스크립트가 기존 rank를 반환한다")
        void duplicate_user_returns_existing_rank() {
            UserId userId = UserId.of("user0001");
            when(entryTokenRepository.exists(userId)).thenReturn(false);

            // 첫 번째 진입: rank 10 반환
            when(waitingQueueRepository.enterAtomically(userId, 100_000L)).thenReturn(10L);
            when(waitingQueueRepository.getTotalSize()).thenReturn(11L);

            EnterQueueUseCase.EnterQueueResult first = queueService.enter(userId);
            assertThat(first.rank()).isEqualTo(11);

            // 두 번째 진입: 이미 존재하므로 동일 rank 반환
            EnterQueueUseCase.EnterQueueResult second = queueService.enter(userId);
            assertThat(second.rank()).isEqualTo(11);

            verify(waitingQueueRepository, times(2)).enterAtomically(userId, 100_000L);
        }

        @Test
        @DisplayName("서로 다른 유저 100명이 순차 진입하면 모두 고유한 순번을 받는다")
        void multiple_users_get_unique_ranks() {
            Set<Long> ranks = new HashSet<>();

            for (int i = 0; i < 100; i++) {
                UserId userId = UserId.of(String.format("user%04d", i));
                when(entryTokenRepository.exists(userId)).thenReturn(false);
                when(waitingQueueRepository.enterAtomically(eq(userId), eq(100_000L))).thenReturn((long) i);
                when(waitingQueueRepository.getTotalSize()).thenReturn((long) (i + 1));

                EnterQueueUseCase.EnterQueueResult result = queueService.enter(userId);
                ranks.add(result.rank());
            }

            // 100명 모두 고유한 순번
            assertThat(ranks).hasSize(100);
        }
    }

    @Nested
    @DisplayName("순번 조회")
    class QueryPosition {

        @Test
        @DisplayName("대기 중인 유저의 순번과 예상 대기 시간을 반환한다")
        void getPosition_waiting() {
            UserId userId = UserId.of("user0001");
            when(entryTokenRepository.findByUserId(userId)).thenReturn(Optional.empty());
            when(waitingQueueRepository.getRank(userId)).thenReturn(Optional.of(174L));
            when(waitingQueueRepository.getTotalSize()).thenReturn(500L);

            QueuePosition position = queueService.getPosition(userId);

            assertThat(position.getRank()).isEqualTo(175); // 0-based → 1-based
            assertThat(position.getTotalWaiting()).isEqualTo(500);
            assertThat(position.getEstimatedWaitSeconds()).isEqualTo(1); // 175 / 175 = 1초
            assertThat(position.isReady()).isFalse();
        }

        @Test
        @DisplayName("토큰이 발급된 유저는 ready 상태와 토큰을 반환한다")
        void getPosition_ready_with_token() {
            UserId userId = UserId.of("user0001");
            when(entryTokenRepository.findByUserId(userId)).thenReturn(Optional.of("abc-token-123"));

            QueuePosition position = queueService.getPosition(userId);

            assertThat(position.isReady()).isTrue();
            assertThat(position.getEntryToken()).isEqualTo("abc-token-123");
            assertThat(position.getRank()).isEqualTo(0);
        }

        @Test
        @DisplayName("대기열에 없는 유저가 순번 조회 시 예외가 발생한다")
        void getPosition_not_in_queue() {
            UserId userId = UserId.of("user0001");
            when(entryTokenRepository.findByUserId(userId)).thenReturn(Optional.empty());
            when(waitingQueueRepository.getRank(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> queueService.getPosition(userId))
                    .isInstanceOf(CoreException.class)
                    .satisfies(e -> assertThat(((CoreException) e).getErrorType())
                            .isEqualTo(ErrorType.QUEUE_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("토큰 원자적 소비")
    class TokenConsumption {

        @Test
        @DisplayName("유효한 토큰 소비 성공 (consumeIfMatches 반환 true)")
        void consume_success() {
            UserId userId = UserId.of("user0001");
            when(entryTokenRepository.consumeIfMatches(userId, "valid-token")).thenReturn(true);

            assertThatCode(() -> queueService.consume(userId, "valid-token"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("토큰 미존재 시 예외 (consumeIfMatches 반환 null)")
        void consume_fail_token_not_found() {
            UserId userId = UserId.of("user0001");
            when(entryTokenRepository.consumeIfMatches(userId, "any-token")).thenReturn(null);

            assertThatThrownBy(() -> queueService.consume(userId, "any-token"))
                    .isInstanceOf(CoreException.class)
                    .satisfies(e -> assertThat(((CoreException) e).getErrorType())
                            .isEqualTo(ErrorType.QUEUE_TOKEN_NOT_FOUND));
        }

        @Test
        @DisplayName("토큰 불일치 시 예외 (consumeIfMatches 반환 false)")
        void consume_fail_token_mismatch() {
            UserId userId = UserId.of("user0001");
            when(entryTokenRepository.consumeIfMatches(userId, "fake-token")).thenReturn(false);

            assertThatThrownBy(() -> queueService.consume(userId, "fake-token"))
                    .isInstanceOf(CoreException.class)
                    .satisfies(e -> assertThat(((CoreException) e).getErrorType())
                            .isEqualTo(ErrorType.QUEUE_TOKEN_INVALID));
        }
    }

    @Nested
    @DisplayName("토큰 발급 (스케줄러) - Lua 스크립트 원자적 발급")
    class IssueTokens {

        @Test
        @DisplayName("배치 크기만큼 대기열에서 꺼내 토큰을 원자적으로 발급한다")
        void issueTokens_success() {
            List<IssuedToken> issued = List.of(
                    new IssuedToken(UserId.of("user0001"), "token-1"),
                    new IssuedToken(UserId.of("user0002"), "token-2"),
                    new IssuedToken(UserId.of("user0003"), "token-3")
            );
            when(waitingQueueRepository.popAndIssueTokens(18, 300L)).thenReturn(issued);

            int count = queueService.issueTokens();

            assertThat(count).isEqualTo(3);
            verify(waitingQueueRepository).popAndIssueTokens(18, 300L);
        }

        @Test
        @DisplayName("대기열이 비어있으면 토큰을 발급하지 않는다")
        void issueTokens_empty_queue() {
            when(waitingQueueRepository.popAndIssueTokens(18, 300L)).thenReturn(List.of());

            int count = queueService.issueTokens();

            assertThat(count).isEqualTo(0);
        }

        @Test
        @DisplayName("배치 크기보다 대기 인원이 적으면 있는 만큼만 발급한다")
        void issueTokens_less_than_batch() {
            List<IssuedToken> issued = List.of(
                    new IssuedToken(UserId.of("user0001"), "token-1")
            );
            when(waitingQueueRepository.popAndIssueTokens(18, 300L)).thenReturn(issued);

            int count = queueService.issueTokens();

            assertThat(count).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("처리량 초과 테스트")
    class ThroughputExceed {

        @Test
        @DisplayName("대기열 최대 크기 초과 시 추가 진입이 거부된다")
        void reject_when_queue_exceeds_max_size() {
            when(queueProperties.getMaxQueueSize()).thenReturn(100L);

            AtomicInteger rejectedCount = new AtomicInteger(0);
            for (int i = 0; i < 110; i++) {
                UserId userId = UserId.of(String.format("user%04d", i));
                when(entryTokenRepository.exists(userId)).thenReturn(false);

                // 처음 100명은 진입 가능, 이후는 Lua가 -1 반환
                if (i < 100) {
                    when(waitingQueueRepository.enterAtomically(userId, 100L)).thenReturn((long) i);
                    when(waitingQueueRepository.getTotalSize()).thenReturn((long) (i + 1));
                } else {
                    when(waitingQueueRepository.enterAtomically(userId, 100L)).thenReturn(-1L);
                }

                try {
                    queueService.enter(userId);
                } catch (CoreException e) {
                    if (e.getErrorType() == ErrorType.QUEUE_FULL) {
                        rejectedCount.incrementAndGet();
                    }
                }
            }

            assertThat(rejectedCount.get()).isEqualTo(10);
        }

        @Test
        @DisplayName("스케줄러는 배치 크기 이상을 한 번에 발급하지 않는다")
        void scheduler_respects_batch_size() {
            List<IssuedToken> batchIssued = new ArrayList<>();
            for (int i = 0; i < 18; i++) {
                batchIssued.add(new IssuedToken(
                        UserId.of(String.format("user%04d", i)),
                        "token-" + i
                ));
            }
            when(waitingQueueRepository.popAndIssueTokens(18, 300L)).thenReturn(batchIssued);

            int issued = queueService.issueTokens();

            assertThat(issued).isEqualTo(18);
            verify(waitingQueueRepository).popAndIssueTokens(18, 300L);
        }
    }
}
