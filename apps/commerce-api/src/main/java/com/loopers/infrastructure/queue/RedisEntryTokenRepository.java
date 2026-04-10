package com.loopers.infrastructure.queue;

import com.loopers.domain.model.queue.EntryToken;
import com.loopers.domain.model.user.UserId;
import com.loopers.domain.repository.EntryTokenRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.loopers.config.redis.RedisConfig.REDIS_TEMPLATE_MASTER;

@Repository
public class RedisEntryTokenRepository implements EntryTokenRepository {

    private static final String TOKEN_KEY_PREFIX = "entry-token:";

    private final RedisTemplate<String, String> writeTemplate;

    /**
     * 토큰 검증 + 삭제 원자 연산 Lua 스크립트.
     * KEYS[1] = entry-token:{userId}
     * ARGV[1] = 기대하는 토큰 값
     *
     * 반환: 1 (일치하여 삭제), 0 (불일치), -1 (토큰 미존재)
     */
    private static final DefaultRedisScript<Long> CONSUME_IF_MATCHES_SCRIPT = new DefaultRedisScript<>(
            """
            local stored = redis.call('GET', KEYS[1])
            if not stored then
                return -1
            end
            if stored == ARGV[1] then
                redis.call('DEL', KEYS[1])
                return 1
            end
            return 0
            """,
            Long.class
    );

    public RedisEntryTokenRepository(
            @Qualifier(REDIS_TEMPLATE_MASTER) RedisTemplate<String, String> writeTemplate) {
        this.writeTemplate = writeTemplate;
    }

    @Override
    public void save(EntryToken token, long ttlSeconds) {
        String key = tokenKey(token.getUserId());
        writeTemplate.opsForValue().set(key, token.getToken(), ttlSeconds, TimeUnit.SECONDS);
    }

    @Override
    public Optional<String> findByUserId(UserId userId) {
        String token = writeTemplate.opsForValue().get(tokenKey(userId));
        return Optional.ofNullable(token);
    }

    @Override
    public void delete(UserId userId) {
        writeTemplate.delete(tokenKey(userId));
    }

    @Override
    public boolean exists(UserId userId) {
        return Boolean.TRUE.equals(writeTemplate.hasKey(tokenKey(userId)));
    }

    @Override
    public Boolean consumeIfMatches(UserId userId, String token) {
        Long result = writeTemplate.execute(
                CONSUME_IF_MATCHES_SCRIPT,
                List.of(tokenKey(userId)),
                token
        );
        if (result == null || result == -1) {
            return null; // 토큰 미존재
        }
        return result == 1; // 1: 일치하여 삭제, 0: 불일치
    }

    private String tokenKey(UserId userId) {
        return TOKEN_KEY_PREFIX + userId.getValue();
    }
}
