package com.loopers.domain.repository;

import com.loopers.domain.model.queue.EntryToken;
import com.loopers.domain.model.user.UserId;

import java.util.Optional;

public interface EntryTokenRepository {

    void save(EntryToken token, long ttlSeconds);

    Optional<String> findByUserId(UserId userId);

    void delete(UserId userId);

    boolean exists(UserId userId);

    /**
     * 토큰 검증과 삭제를 원자적으로 수행한다.
     * 저장된 토큰이 주어진 토큰과 일치하면 삭제하고 true를 반환한다.
     *
     * @return 일치하여 삭제되었으면 true, 토큰 미존재 시 null, 불일치 시 false
     */
    Boolean consumeIfMatches(UserId userId, String token);
}
