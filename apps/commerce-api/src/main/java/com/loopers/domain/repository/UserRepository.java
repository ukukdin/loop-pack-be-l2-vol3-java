package com.loopers.domain.repository;

import com.loopers.domain.model.User;
import com.loopers.domain.model.UserId;

import java.util.Optional;

public interface UserRepository {

    /**
 * Persist the given user and return the persisted instance.
 *
 * @param member the user to persist; may be updated with persistence-generated attributes (for example, an assigned identifier)
 * @return the persisted User instance reflecting any changes made during persistence
 */
User save(User member);

    /**
 * Locates a User by its UserId.
 *
 * @param userId the identifier of the User to find
 * @return an Optional containing the User if found, otherwise Optional.empty()
 */
Optional<User> findById(UserId userId);

    /**
 * Checks whether a user with the given UserId exists.
 *
 * @param userId the identifier of the user to check
 * @return `true` if a user with the given `userId` exists, `false` otherwise
 */
boolean existsById(UserId userId);


}