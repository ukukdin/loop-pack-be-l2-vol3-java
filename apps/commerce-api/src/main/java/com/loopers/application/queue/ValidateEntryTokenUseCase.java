package com.loopers.application.queue;

import com.loopers.domain.model.user.UserId;

public interface ValidateEntryTokenUseCase {

    void validate(UserId userId, String token);

    /**
     * 토큰을 원자적으로 검증하고 소비(삭제)한다.
     * 동일 토큰으로 동시에 두 번 요청해도 정확히 한 번만 성공한다.
     */
    void consume(UserId userId, String token);
}
