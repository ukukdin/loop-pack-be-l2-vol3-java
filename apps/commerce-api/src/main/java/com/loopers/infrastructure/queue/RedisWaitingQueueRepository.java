package com.loopers.infrastructure.queue;

import com.loopers.domain.model.user.UserId;
import com.loopers.domain.repository.WaitingQueueRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.loopers.config.redis.RedisConfig.REDIS_TEMPLATE_MASTER;

@Repository
public class RedisWaitingQueueRepository implements WaitingQueueRepository {

    private static final String QUEUE_KEY = "waiting-queue";

    private final RedisTemplate<String, String> readTemplate;
    private final RedisTemplate<String, String> writeTemplate;

    public RedisWaitingQueueRepository(
            RedisTemplate<String, String> readTemplate,
            @Qualifier(REDIS_TEMPLATE_MASTER) RedisTemplate<String, String> writeTemplate) {
        this.readTemplate = readTemplate;
        this.writeTemplate = writeTemplate;
    }

    @Override
    public boolean enter(UserId userId, double score) {
        Boolean added = writeTemplate.opsForZSet().addIfAbsent(QUEUE_KEY, userId.getValue(), score);
        return Boolean.TRUE.equals(added);
    }

    @Override
    public Optional<Long> getRank(UserId userId) {
        Long rank = readTemplate.opsForZSet().rank(QUEUE_KEY, userId.getValue());
        return Optional.ofNullable(rank);
    }

    @Override
    public long getTotalSize() {
        Long size = readTemplate.opsForZSet().zCard(QUEUE_KEY);
        return size != null ? size : 0;
    }

    @Override
    public List<UserId> popFront(int count) {
        Set<ZSetOperations.TypedTuple<String>> popped =
                writeTemplate.opsForZSet().popMin(QUEUE_KEY, count);

        if (popped == null || popped.isEmpty()) {
            return List.of();
        }

        List<UserId> userIds = new ArrayList<>(popped.size());
        for (ZSetOperations.TypedTuple<String> tuple : popped) {
            String value = tuple.getValue();
            if (value != null) {
                userIds.add(UserId.of(value));
            }
        }
        return userIds;
    }

    @Override
    public boolean exists(UserId userId) {
        Long rank = readTemplate.opsForZSet().rank(QUEUE_KEY, userId.getValue());
        return rank != null;
    }

    @Override
    public void remove(UserId userId) {
        writeTemplate.opsForZSet().remove(QUEUE_KEY, userId.getValue());
    }
}
