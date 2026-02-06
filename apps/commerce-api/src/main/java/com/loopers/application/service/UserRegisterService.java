package com.loopers.application.service;

import com.loopers.application.RegisterUseCase;
import com.loopers.domain.model.*;
import com.loopers.domain.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class UserRegisterService implements RegisterUseCase {

    private final UserRepository userRepository;

    public UserRegisterService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void register(UserId userId, UserName userName, String encodedPassword, Birthday birthday, Email email) {
        try {
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
            } catch (DataIntegrityViolationException ex) {
                throw new IllegalArgumentException("이미 사용중인 ID 입니다.", ex);
            }
    }
}
