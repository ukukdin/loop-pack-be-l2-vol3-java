package com.loopers.infrastructure;

import com.loopers.domain.model.*;
import com.loopers.domain.repository.UserRepository;
import com.loopers.infrastructure.entity.UserJpaEntity;
import com.loopers.infrastructure.repository.UserJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository userJpaRepository;

    /**
 * Constructs a UserRepositoryImpl backed by the provided JPA repository.
 *
 * @param userJpaRepository the JPA repository used to perform persistence operations for User entities
 */
public UserRepositoryImpl( UserJpaRepository userJpaRepository) {this.userJpaRepository = userJpaRepository;}

    /**
     * Persist the given domain User and return the resultant domain User reflecting the stored state.
     *
     * @param user the domain User to persist
     * @return the persisted User populated with persistence-derived fields (for example generated id or timestamps)
     */
    @Override
    public User save(User user) {
        UserJpaEntity entity = toEntity(user);
        UserJpaEntity saved = userJpaRepository.save(entity);
        return toDomain(saved);
    }

    /**
     * Finds a User by its UserId.
     *
     * @param userId the identifier of the user to retrieve
     * @return an {@code Optional} containing the User if found, or an empty {@code Optional} if not
     */
    @Override
    public Optional<User> findById(UserId userId) {
        return userJpaRepository.findByUserId(userId.getValue())
                .map(this::toDomain);
    }

    /**
     * Check whether a user with the given userId exists in persistence.
     *
     * @param userId the domain user identifier to check for existence
     * @return `true` if a user with the given `userId` exists, `false` otherwise
     */
    @Override
    public boolean existsById(UserId userId) {
        return userJpaRepository.existsByUserId(userId.getValue());
    }

    /**
     * Converts a domain {@code User} into a {@code UserJpaEntity} suitable for persistence.
     *
     * @param user the domain user to convert
     * @return a {@code UserJpaEntity} populated with the user's ID, credentials, profile, and timestamps
     */
    private UserJpaEntity toEntity(User user) {
        return new UserJpaEntity(
                user.getId(),
                user.getUserId(),
                user.getEncodedPassword(),
                user.getUserName(),
                user.getBirth(),
                user.getEmail(),
                user.getCreatedAt()
        );
    }

    /**
     * Reconstructs a domain User from a persisted UserJpaEntity.
     *
     * @param entity the JPA entity containing persisted user state
     * @return a reconstituted User domain object with identity, value objects, encoded password, wrong password count initialized, and original creation timestamp
     */
    private User toDomain(UserJpaEntity entity) {
        return User.reconstitute(
                entity.getId(),
                UserId.of(entity.getUserId()),
                UserName.of(entity.getUsername()),
                entity.getEncodedPassword(),
                Birthday.of(entity.getBirthday()),
                Email.of(entity.getEmail()),
                WrongPasswordCount.init(),
                entity.getCreatedAt()
        );
    }

}