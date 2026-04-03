package com.loopers.domain.model.queue;

import com.loopers.domain.model.user.UserId;
import lombok.Getter;

import java.util.UUID;

@Getter
public class EntryToken {

    private final String token;
    private final UserId userId;

    private EntryToken(String token, UserId userId) {
        this.token = token;
        this.userId = userId;
    }

    public static EntryToken issue(UserId userId) {
        String token = UUID.randomUUID().toString();
        return new EntryToken(token, userId);
    }

    public static EntryToken of(String token, UserId userId) {
        return new EntryToken(token, userId);
    }

    public boolean belongsTo(UserId userId) {
        return this.userId.equals(userId);
    }
}
