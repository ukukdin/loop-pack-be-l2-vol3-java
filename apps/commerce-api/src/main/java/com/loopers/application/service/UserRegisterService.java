package com.loopers.application.service;

import com.loopers.application.RegisterUseCase;
import com.loopers.domain.model.*;
import com.loopers.domain.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UserRegisterService implements RegisterUseCase {

    private final UserRepository userRepository;

    public UserRegisterService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

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
