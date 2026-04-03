package com.loopers.application.queue;

import com.loopers.domain.model.queue.EntryToken;
import com.loopers.domain.model.queue.QueuePosition;
import com.loopers.domain.model.queue.QueueProperties;
import com.loopers.domain.model.user.UserId;
import com.loopers.domain.repository.EntryTokenRepository;
import com.loopers.domain.repository.WaitingQueueRepository;
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

        long totalSize = waitingQueueRepository.getTotalSize();
        if (totalSize >= queueProperties.getMaxQueueSize()) {
            throw new CoreException(ErrorType.QUEUE_FULL);
        }

        // Sorted Set의 Set 특성으로 중복 진입 자동 방지 (score만 갱신됨)
        double score = System.currentTimeMillis();
        boolean added = waitingQueueRepository.enter(userId, score);

        if (!added) {
            // 이미 대기열에 있는 유저 → 현재 순번 반환
            return buildEnterResult(userId);
        }

        return buildEnterResult(userId);
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
    public void validate(UserId userId, String token) {
        String storedToken = entryTokenRepository.findByUserId(userId)
                .orElseThrow(() -> new CoreException(ErrorType.QUEUE_TOKEN_NOT_FOUND));

        if (!storedToken.equals(token)) {
            throw new CoreException(ErrorType.QUEUE_TOKEN_INVALID);
        }
    }

    @Override
    public void consume(UserId userId) {
        entryTokenRepository.delete(userId);
    }

    public int issueTokens() {
        int batchSize = queueProperties.getBatchSize();
        List<UserId> userIds = waitingQueueRepository.popFront(batchSize);

        for (UserId userId : userIds) {
            EntryToken token = EntryToken.issue(userId);
            entryTokenRepository.save(token, queueProperties.getTokenTtlSeconds());
        }

        return userIds.size();
    }

    private EnterQueueResult buildEnterResult(UserId userId) {
        long rank = waitingQueueRepository.getRank(userId)
                .orElse(0L);
        long position = rank + 1;
        long totalWaiting = waitingQueueRepository.getTotalSize();
        long throughput = queueProperties.getThroughputPerSecond();
        long estimatedWait = (throughput > 0) ? position / throughput : 0;

        return new EnterQueueResult(position, totalWaiting, estimatedWait);
    }
}
