package com.loopers.application.service;

import com.loopers.application.RegisterUseCase;
import com.loopers.domain.model.*;
import com.loopers.domain.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UserRegisterService implements RegisterUseCase {

    private final UserRepository userRepository;

    /**
     * Creates a UserRegisterService with the given UserRepository.
     *
     * @param userRepository the repository used to check for existing users and persist new users
     */
    public UserRegisterService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Registers a new user and saves it to the repository.
     *
     * @param userId the identifier for the new user
     * @param userName the display name for the new user
     * @param encodedPassword the user's password in encoded form
     * @param birthday the user's birthday
     * @param email the user's email address
     * @throws IllegalArgumentException if a user with the given userId already exists
     */
    @Override
    public void register(UserId userId, UserName userName, String encodedPassword, Birthday birthday, Email email) {
        if (userRepository.existsById(userId)) {
            throw new IllegalArgumentException("이미 사용중인 ID 입니다.");
        }

        User user = User.register(
                userId,
                userName,
                encodedPassword,
                birthday,
                email,
                WrongPasswordCount.init(),
                LocalDateTime.now()
        );

        userRepository.save(user);
    }
}