package com.loopers.infrastructure.queue;

import com.loopers.domain.model.queue.EntryToken;
import com.loopers.domain.model.user.UserId;
import com.loopers.domain.repository.EntryTokenRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.loopers.config.redis.RedisConfig.REDIS_TEMPLATE_MASTER;

@Repository
public class RedisEntryTokenRepository implements EntryTokenRepository {

    private static final String TOKEN_KEY_PREFIX = "entry-token:";

    private final RedisTemplate<String, String> readTemplate;
    private final RedisTemplate<String, String> writeTemplate;

    public RedisEntryTokenRepository(
            RedisTemplate<String, String> readTemplate,
            @Qualifier(REDIS_TEMPLATE_MASTER) RedisTemplate<String, String> writeTemplate) {
        this.readTemplate = readTemplate;
        this.writeTemplate = writeTemplate;
    }

    @Override
    public void save(EntryToken token, long ttlSeconds) {
        String key = tokenKey(token.getUserId());
        writeTemplate.opsForValue().set(key, token.getToken(), ttlSeconds, TimeUnit.SECONDS);
    }

    @Override
    public Optional<String> findByUserId(UserId userId) {
        String token = readTemplate.opsForValue().get(tokenKey(userId));
        return Optional.ofNullable(token);
    }

    @Override
    public void delete(UserId userId) {
        writeTemplate.delete(tokenKey(userId));
    }

    @Override
    public boolean exists(UserId userId) {
        return Boolean.TRUE.equals(readTemplate.hasKey(tokenKey(userId)));
    }

    private String tokenKey(UserId userId) {
        return TOKEN_KEY_PREFIX + userId.getValue();
    }
}
