package com.loopers.infrastructure.repository;

import com.loopers.infrastructure.entity.UserJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserJpaRepository extends JpaRepository<UserJpaEntity, Long> {
    /**
 * Finds a user by its userId.
 *
 * @param username the userId to search for
 * @return an Optional containing the matching UserJpaEntity if found, otherwise an empty Optional
 */
Optional<UserJpaEntity> findByUserId(String username);
    /**
 * Checks whether a user with the specified userId exists.
 *
 * @param userId the user identifier to check for existence
 * @return `true` if a user with the specified userId exists, `false` otherwise
 */
boolean existsByUserId(String userId);
}