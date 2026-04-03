package com.loopers.application.queue;

import com.loopers.domain.model.queue.QueuePosition;
import com.loopers.domain.model.queue.QueueProperties;
import com.loopers.domain.model.user.UserId;
import com.loopers.domain.repository.EntryTokenRepository;
import com.loopers.domain.repository.WaitingQueueRepository;
import com.loopers.domain.repository.WaitingQueueRepository.IssuedToken;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QueueService implements EnterQueueUseCase, QueryPositionUseCase, ValidateEntryTokenUseCase {

    private final WaitingQueueRepository waitingQueueRepository;
    private final EntryTokenRepository entryTokenRepository;
    private final QueueProperties queueProperties;

    public QueueService(WaitingQueueRepository waitingQueueRepository,
                        EntryTokenRepository entryTokenRepository,
                        QueueProperties queueProperties) {
        this.waitingQueueRepository = waitingQueueRepository;
        this.entryTokenRepository = entryTokenRepository;
        this.queueProperties = queueProperties;
    }

    @Override
    public EnterQueueResult enter(UserId userId) {
        // 이미 토큰을 보유한 유저는 대기열 진입 불필요
        if (entryTokenRepository.exists(userId)) {
            throw new CoreException(ErrorType.QUEUE_ALREADY_HAS_TOKEN);
        }

        // Lua 스크립트로 원자적 진입: maxQueueSize 검사 + INCR score + ZADD NX
        long rank = waitingQueueRepository.enterAtomically(userId, queueProperties.getMaxQueueSize());

        if (rank == -1) {
            throw new CoreException(ErrorType.QUEUE_FULL);
        }

        long position = rank + 1; // 0-based → 1-based
        long totalWaiting = waitingQueueRepository.getTotalSize();
        long throughput = queueProperties.getThroughputPerSecond();
        long estimatedWait = (throughput > 0) ? position / throughput : 0;

        return new EnterQueueResult(position, totalWaiting, estimatedWait);
    }

    @Override
    public QueuePosition getPosition(UserId userId) {
        // 토큰이 이미 발급된 경우
        return entryTokenRepository.findByUserId(userId)
                .map(QueuePosition::ready)
                .orElseGet(() -> {
                    Long rank = waitingQueueRepository.getRank(userId)
                            .orElseThrow(() -> new CoreException(ErrorType.QUEUE_NOT_FOUND));
                    long position = rank + 1; // 0-based → 1-based
                    long totalWaiting = waitingQueueRepository.getTotalSize();
                    return QueuePosition.waiting(position, totalWaiting, queueProperties.getThroughputPerSecond());
                });
    }

    @Override
    public void consume(UserId userId, String token) {
        Boolean result = entryTokenRepository.consumeIfMatches(userId, token);
        if (result == null) {
            throw new CoreException(ErrorType.QUEUE_TOKEN_NOT_FOUND);
        }
        if (!result) {
            throw new CoreException(ErrorType.QUEUE_TOKEN_INVALID);
        }
    }

    /**
     * 대기열에서 batchSize명을 pop하고 토큰을 원자적으로 발급한다.
     * pop + token save + TTL 설정이 Lua 스크립트로 원자화되어
     * 중간 실패 시 사용자 유실을 방지한다.
     */
    public int issueTokens() {
        int batchSize = queueProperties.getBatchSize();
        long ttlSeconds = queueProperties.getTokenTtlSeconds();

        List<IssuedToken> issuedTokens = waitingQueueRepository.popAndIssueTokens(batchSize, ttlSeconds);
        return issuedTokens.size();
    }
}
