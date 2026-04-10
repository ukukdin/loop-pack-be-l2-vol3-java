package com.loopers.application.queue;

import com.loopers.domain.model.queue.QueuePosition;
import com.loopers.domain.model.user.UserId;

public interface QueryPositionUseCase {

    QueuePosition getPosition(UserId userId);
}
