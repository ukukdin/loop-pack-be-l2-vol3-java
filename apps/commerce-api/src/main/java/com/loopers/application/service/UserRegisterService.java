package com.loopers.application.service;

import com.loopers.application.RegisterUseCase;
import com.loopers.domain.model.UserId;
import com.loopers.domain.repository.UserRepository;

public class UserRegisterService implements RegisterUseCase {

    private final UserRepository userRepository;

    public UserRegisterService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void register(UserId userId) {
        if (userRepository.existsById(userId)) {
            throw new IllegalArgumentException("이미 사용중인 ID 입니다.");
        }
    }
}
