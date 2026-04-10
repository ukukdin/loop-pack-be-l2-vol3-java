package com.loopers.infrastructure.queue;

import com.loopers.domain.model.user.UserId;
import com.loopers.domain.repository.WaitingQueueRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.loopers.config.redis.RedisConfig.REDIS_TEMPLATE_MASTER;

@Repository
public class RedisWaitingQueueRepository implements WaitingQueueRepository {

    private static final String QUEUE_KEY = "waiting-queue";
    private static final String QUEUE_SEQ_KEY = "waiting-queue:seq";
    private static final String TOKEN_KEY_PREFIX = "entry-token:";

    private final RedisTemplate<String, String> readTemplate;
    private final RedisTemplate<String, String> writeTemplate;

    /**
     * 대기열 진입 Lua 스크립트.
     * KEYS[1] = waiting-queue (Sorted Set)
     * KEYS[2] = waiting-queue:seq (INCR 기반 단조 증가 score)
     * ARGV[1] = userId
     * ARGV[2] = maxQueueSize
     * ARGV[3] = token key prefix (e.g. "entry-token:")
     *
     * 반환: -2 (이미 토큰 보유), -1 (대기열 초과), 또는 0-based rank
     */
    private static final DefaultRedisScript<Long> ENTER_SCRIPT = new DefaultRedisScript<>(
            """
            -- 이미 토큰을 보유했는지 확인 (스케줄러 경합 방지)
            local tokenKey = ARGV[3] .. ARGV[1]
            if redis.call('EXISTS', tokenKey) == 1 then
                return -2
            end
            -- 이미 대기열에 있는지 확인
            local existingRank = redis.call('ZRANK', KEYS[1], ARGV[1])
            if existingRank then
                return existingRank
            end
            -- 대기열 크기 확인
            local size = redis.call('ZCARD', KEYS[1])
            if size >= tonumber(ARGV[2]) then
                return -1
            end
            -- 단조 증가 score로 FIFO 보장
            local score = redis.call('INCR', KEYS[2])
            redis.call('ZADD', KEYS[1], 'NX', score, ARGV[1])
            return redis.call('ZRANK', KEYS[1], ARGV[1])
            """,
            Long.class
    );

    /**
     * 대기열 pop + 토큰 발급 Lua 스크립트.
     * KEYS[1] = waiting-queue (Sorted Set)
     * ARGV[1] = count (pop할 개수)
     * ARGV[2] = ttlSeconds
     * ARGV[3] = token key prefix (e.g. "entry-token:")
     * ARGV[4..] = 각 유저에 할당할 UUID 토큰 (count개)
     *
     * 반환: {userId1, token1, userId2, token2, ...}
     */
    private static final DefaultRedisScript<List> POP_AND_ISSUE_SCRIPT = new DefaultRedisScript<>(
            """
            local count = tonumber(ARGV[1])
            local ttl = tonumber(ARGV[2])
            local prefix = ARGV[3]
            local popped = redis.call('ZPOPMIN', KEYS[1], count)
            local result = {}
            local poppedCount = #popped / 2
            for i = 1, poppedCount do
                local userId = popped[(i - 1) * 2 + 1]
                local token = ARGV[3 + i]
                local tokenKey = prefix .. userId
                redis.call('SET', tokenKey, token, 'EX', ttl)
                table.insert(result, userId)
                table.insert(result, token)
            end
            return result
            """,
            List.class
    );

    public RedisWaitingQueueRepository(
            RedisTemplate<String, String> readTemplate,
            @Qualifier(REDIS_TEMPLATE_MASTER) RedisTemplate<String, String> writeTemplate) {
        this.readTemplate = readTemplate;
        this.writeTemplate = writeTemplate;
    }

    @Override
    public long enterAtomically(UserId userId, long maxQueueSize) {
        Long result = writeTemplate.execute(
                ENTER_SCRIPT,
                List.of(QUEUE_KEY, QUEUE_SEQ_KEY),
                userId.getValue(),
                String.valueOf(maxQueueSize),
                TOKEN_KEY_PREFIX
        );
        return result != null ? result : -1;
    }

    @Override
    public Optional<Long> getRank(UserId userId) {
        Long rank = writeTemplate.opsForZSet().rank(QUEUE_KEY, userId.getValue());
        return Optional.ofNullable(rank);
    }

    @Override
    public long getTotalSize() {
        Long size = writeTemplate.opsForZSet().zCard(QUEUE_KEY);
        return size != null ? size : 0;
    }

    @Override
    public List<IssuedToken> popAndIssueTokens(int count, long ttlSeconds) {
        if (count <= 0) {
            return List.of();
        }

        // 각 유저에 할당할 토큰을 미리 생성
        String[] tokens = new String[count];
        for (int i = 0; i < count; i++) {
            tokens[i] = UUID.randomUUID().toString();
        }

        // ARGV: count, ttlSeconds, tokenKeyPrefix, token1, token2, ...
        String[] args = new String[3 + count];
        args[0] = String.valueOf(count);
        args[1] = String.valueOf(ttlSeconds);
        args[2] = TOKEN_KEY_PREFIX;
        System.arraycopy(tokens, 0, args, 3, count);

        @SuppressWarnings("unchecked")
        List<String> result = writeTemplate.execute(
                POP_AND_ISSUE_SCRIPT,
                List.of(QUEUE_KEY),
                (Object[]) args
        );

        if (result == null || result.isEmpty()) {
            return List.of();
        }

        List<IssuedToken> issuedTokens = new ArrayList<>(result.size() / 2);
        for (int i = 0; i < result.size(); i += 2) {
            issuedTokens.add(new IssuedToken(
                    UserId.of(result.get(i)),
                    result.get(i + 1)
            ));
        }
        return issuedTokens;
    }

    @Override
    public boolean exists(UserId userId) {
        Long rank = writeTemplate.opsForZSet().rank(QUEUE_KEY, userId.getValue());
        return rank != null;
    }

    @Override
    public void remove(UserId userId) {
        writeTemplate.opsForZSet().remove(QUEUE_KEY, userId.getValue());
    }
}
