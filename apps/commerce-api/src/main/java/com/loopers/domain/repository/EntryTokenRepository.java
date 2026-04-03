package com.loopers.domain.repository;

import com.loopers.domain.model.queue.EntryToken;
import com.loopers.domain.model.user.UserId;

import java.util.Optional;

public interface EntryTokenRepository {

    void save(EntryToken token, long ttlSeconds);

    Optional<String> findByUserId(UserId userId);

    void delete(UserId userId);

    boolean exists(UserId userId);
}
