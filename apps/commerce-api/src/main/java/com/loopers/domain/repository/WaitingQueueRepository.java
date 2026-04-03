package com.loopers.domain.repository;

import com.loopers.domain.model.user.UserId;

import java.util.List;
import java.util.Optional;

public interface WaitingQueueRepository {

    boolean enter(UserId userId, double score);

    Optional<Long> getRank(UserId userId);

    long getTotalSize();

    List<UserId> popFront(int count);

    boolean exists(UserId userId);

    void remove(UserId userId);
}
