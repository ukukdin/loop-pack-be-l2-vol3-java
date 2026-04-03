package com.loopers.application.queue;

import com.loopers.domain.model.queue.QueuePosition;
import com.loopers.domain.model.queue.QueueProperties;
import com.loopers.domain.model.user.UserId;
import com.loopers.domain.repository.EntryTokenRepository;
import com.loopers.domain.repository.WaitingQueueRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.*;
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
        @DisplayName("대기열 진입 성공 - 순번과 대기 인원을 반환한다")
        void enter_success() {
            UserId userId = UserId.of("user0001");
            when(entryTokenRepository.exists(userId)).thenReturn(false);
            when(waitingQueueRepository.getTotalSize()).thenReturn(50L);
            when(waitingQueueRepository.enter(eq(userId), anyDouble())).thenReturn(true);
            when(waitingQueueRepository.getRank(userId)).thenReturn(Optional.of(50L));

            EnterQueueUseCase.EnterQueueResult result = queueService.enter(userId);

            assertThat(result.rank()).isEqualTo(51); // 0-based → 1-based
            verify(waitingQueueRepository).enter(eq(userId), anyDouble());
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
        @DisplayName("대기열이 가득 찬 경우 진입이 거부된다")
        void enter_fail_queue_full() {
            UserId userId = UserId.of("user0001");
            when(entryTokenRepository.exists(userId)).thenReturn(false);
            when(waitingQueueRepository.getTotalSize()).thenReturn(100_000L);

            assertThatThrownBy(() -> queueService.enter(userId))
                    .isInstanceOf(CoreException.class)
                    .satisfies(e -> assertThat(((CoreException) e).getErrorType())
                            .isEqualTo(ErrorType.QUEUE_FULL));
        }

        @Test
        @DisplayName("중복 진입 시 기존 순번을 반환한다 (Sorted Set addIfAbsent)")
        void enter_duplicate_returns_existing_rank() {
            UserId userId = UserId.of("user0001");
            when(entryTokenRepository.exists(userId)).thenReturn(false);
            when(waitingQueueRepository.getTotalSize()).thenReturn(50L);
            when(waitingQueueRepository.enter(eq(userId), anyDouble())).thenReturn(false); // 이미 존재
            when(waitingQueueRepository.getRank(userId)).thenReturn(Optional.of(10L));

            EnterQueueUseCase.EnterQueueResult result = queueService.enter(userId);

            assertThat(result.rank()).isEqualTo(11);
        }
    }

    @Nested
    @DisplayName("동시 진입 테스트")
    class ConcurrentEntry {

        @Test
        @DisplayName("중복 유저가 진입 시도하면 addIfAbsent가 false를 반환하여 중복이 방지된다")
        void duplicate_user_is_rejected_by_sorted_set() {
            UserId userId = UserId.of("user0001");
            when(entryTokenRepository.exists(userId)).thenReturn(false);
            when(waitingQueueRepository.getTotalSize()).thenReturn(10L);

            // 첫 번째 진입: 성공
            when(waitingQueueRepository.enter(eq(userId), anyDouble())).thenReturn(true);
            when(waitingQueueRepository.getRank(userId)).thenReturn(Optional.of(10L));
            EnterQueueUseCase.EnterQueueResult first = queueService.enter(userId);
            assertThat(first.rank()).isEqualTo(11);

            // 두 번째 진입: addIfAbsent false (이미 존재) → 기존 순번 반환
            when(waitingQueueRepository.enter(eq(userId), anyDouble())).thenReturn(false);
            when(waitingQueueRepository.getRank(userId)).thenReturn(Optional.of(10L));
            EnterQueueUseCase.EnterQueueResult second = queueService.enter(userId);
            assertThat(second.rank()).isEqualTo(11);

            // enter는 2번 호출되었지만, Sorted Set 특성으로 member는 1개만 존재
            verify(waitingQueueRepository, times(2)).enter(eq(userId), anyDouble());
        }

        @Test
        @DisplayName("서로 다른 유저 100명이 순차 진입하면 모두 고유한 순번을 받는다")
        void multiple_users_get_unique_ranks() {
            Set<Long> ranks = new HashSet<>();

            for (int i = 0; i < 100; i++) {
                UserId userId = UserId.of(String.format("user%04d", i));
                when(entryTokenRepository.exists(userId)).thenReturn(false);
                when(waitingQueueRepository.getTotalSize()).thenReturn((long) i);
                when(waitingQueueRepository.enter(eq(userId), anyDouble())).thenReturn(true);
                when(waitingQueueRepository.getRank(userId)).thenReturn(Optional.of((long) i));

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
    @DisplayName("토큰 검증")
    class TokenValidation {

        @Test
        @DisplayName("유효한 토큰 검증 성공")
        void validate_success() {
            UserId userId = UserId.of("user0001");
            when(entryTokenRepository.findByUserId(userId)).thenReturn(Optional.of("valid-token"));

            assertThatCode(() -> queueService.validate(userId, "valid-token"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("토큰이 없는 유저 검증 시 예외 (만료된 경우)")
        void validate_fail_token_expired() {
            UserId userId = UserId.of("user0001");
            when(entryTokenRepository.findByUserId(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> queueService.validate(userId, "any-token"))
                    .isInstanceOf(CoreException.class)
                    .satisfies(e -> assertThat(((CoreException) e).getErrorType())
                            .isEqualTo(ErrorType.QUEUE_TOKEN_NOT_FOUND));
        }

        @Test
        @DisplayName("토큰 값이 불일치하면 예외 발생")
        void validate_fail_token_mismatch() {
            UserId userId = UserId.of("user0001");
            when(entryTokenRepository.findByUserId(userId)).thenReturn(Optional.of("real-token"));

            assertThatThrownBy(() -> queueService.validate(userId, "fake-token"))
                    .isInstanceOf(CoreException.class)
                    .satisfies(e -> assertThat(((CoreException) e).getErrorType())
                            .isEqualTo(ErrorType.QUEUE_TOKEN_INVALID));
        }
    }

    @Nested
    @DisplayName("토큰 발급 (스케줄러)")
    class IssueTokens {

        @Test
        @DisplayName("배치 크기만큼 대기열에서 꺼내 토큰을 발급한다")
        void issueTokens_success() {
            List<UserId> userIds = List.of(
                    UserId.of("user0001"),
                    UserId.of("user0002"),
                    UserId.of("user0003")
            );
            when(waitingQueueRepository.popFront(18)).thenReturn(userIds);

            int issued = queueService.issueTokens();

            assertThat(issued).isEqualTo(3);
            verify(entryTokenRepository, times(3)).save(any(), eq(300L));
        }

        @Test
        @DisplayName("대기열이 비어있으면 토큰을 발급하지 않는다")
        void issueTokens_empty_queue() {
            when(waitingQueueRepository.popFront(18)).thenReturn(List.of());

            int issued = queueService.issueTokens();

            assertThat(issued).isEqualTo(0);
            verify(entryTokenRepository, never()).save(any(), anyLong());
        }

        @Test
        @DisplayName("배치 크기보다 대기 인원이 적으면 있는 만큼만 발급한다")
        void issueTokens_less_than_batch() {
            List<UserId> userIds = List.of(UserId.of("user0001"));
            when(waitingQueueRepository.popFront(18)).thenReturn(userIds);

            int issued = queueService.issueTokens();

            assertThat(issued).isEqualTo(1);
            verify(entryTokenRepository, times(1)).save(any(), eq(300L));
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

                // 처음 100명은 진입 가능, 이후는 대기열 가득 참
                long currentSize = Math.min(i, 100);
                when(waitingQueueRepository.getTotalSize()).thenReturn(currentSize);
                when(waitingQueueRepository.enter(eq(userId), anyDouble())).thenReturn(true);
                when(waitingQueueRepository.getRank(userId)).thenReturn(Optional.of((long) i));

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
            // 대기열에 1000명이 있어도 18명만 꺼냄
            List<UserId> batchUsers = new ArrayList<>();
            for (int i = 0; i < 18; i++) {
                batchUsers.add(UserId.of(String.format("user%04d", i)));
            }
            when(waitingQueueRepository.popFront(18)).thenReturn(batchUsers);

            int issued = queueService.issueTokens();

            assertThat(issued).isEqualTo(18);
            verify(waitingQueueRepository).popFront(18); // 정확히 배치 크기만큼 요청
        }
    }
}
