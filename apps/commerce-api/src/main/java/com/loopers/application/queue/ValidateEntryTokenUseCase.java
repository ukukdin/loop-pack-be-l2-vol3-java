package com.loopers.application.queue;

import com.loopers.domain.model.user.UserId;

public interface ValidateEntryTokenUseCase {

    void validate(UserId userId, String token);

    void consume(UserId userId);
}
