package com.loopers.infrastructure.entity;

import com.loopers.domain.model.Birthday;
import com.loopers.domain.model.Email;
import com.loopers.domain.model.UserId;
import com.loopers.domain.model.UserName;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "users")
public class UserJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String userId;

    @Column(nullable = false)
    private String encodedPassword;

    @Column(nullable = false, length = 20)
    private String username;

    @Column(nullable = false)
    private LocalDate birthday;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
 * Protected no-argument constructor required by JPA to instantiate the entity via reflection.
 */
protected UserJpaEntity() {}


    /**
     * Create a UserJpaEntity from domain value objects and primitive persistence fields.
     *
     * @param id              the primary key value (may be null for a new, unsaved entity)
     * @param userId          the domain UserId whose string value will be stored in the `userId` column
     * @param encodedPassword the already-encoded password to store
     * @param userName        the domain UserName whose string value will be stored in the `username` column
     * @param birth           the domain Birthday whose LocalDate value will be stored in the `birthday` column
     * @param email           the domain Email whose string value will be stored in the `email` column
     * @param createdAt       the creation timestamp to store in the `createdAt` column
     */
    public UserJpaEntity(Long id, UserId userId, String encodedPassword, UserName userName, Birthday birth, Email email, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId.getValue();
        this.encodedPassword = encodedPassword;
        this.username = userName.getValue();
        this.birthday = birth.getValue();
        this.email = email.getValue();
        this.createdAt = createdAt;
    }
}