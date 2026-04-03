package com.loopers.domain.model.queue;

import com.loopers.domain.model.user.UserId;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Objects;
import java.util.UUID;

@Getter
@EqualsAndHashCode
public class EntryToken {

    private final String token;
    private final UserId userId;

    private EntryToken(String token, UserId userId) {
        if (token == null) {
            throw new IllegalArgumentException("token must not be blank");
        }
        String normalizedToken = token.trim();
            if (normalizedToken.isEmpty()) {
                throw new IllegalArgumentException("token must not be blank");
            }
        Objects.requireNonNull(userId, "userId must not be null");
        this.token = normalizedToken;
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
