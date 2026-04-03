package com.loopers.domain.repository;

import com.loopers.domain.model.user.UserId;

import java.util.List;
import java.util.Optional;

public interface WaitingQueueRepository {

    /**
     * 대기열 진입을 원자적으로 수행한다.
     * maxQueueSize 검사 + 단조 증가 score 생성 + ZADD NX를 Lua 스크립트로 통합.
     *
     * @return 진입 성공 시 0-based rank, 이미 존재 시 기존 rank, 대기열 초과 시 -1
     */
    long enterAtomically(UserId userId, long maxQueueSize);

    Optional<Long> getRank(UserId userId);

    long getTotalSize();

    /**
     * 대기열에서 count명을 pop하고 각각에게 토큰을 발급한다.
     * pop + token save + TTL 설정을 Lua 스크립트로 원자화.
     *
     * @return 발급된 토큰 목록 (userId → token)
     */
    List<IssuedToken> popAndIssueTokens(int count, long ttlSeconds);

    boolean exists(UserId userId);

    void remove(UserId userId);

    record IssuedToken(UserId userId, String token) {}
}
