package com.loopers.application.service;

import com.loopers.application.PasswordUpdateUseCase;
import com.loopers.application.RegisterUseCase;
import com.loopers.application.UserQueryUseCase;
import com.loopers.domain.model.*;
import com.loopers.domain.repository.UserRepository;
import com.loopers.domain.service.PasswordEncoder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@Transactional(readOnly = true)
public class UserService implements RegisterUseCase, PasswordUpdateUseCase, UserQueryUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void register(String loginId, String name, String rawPassword, LocalDate birthday, String email) {
        UserId userId = UserId.of(loginId);
        UserName userName = UserName.of(name);
        Birthday birth = Birthday.of(birthday);
        Email userEmail = Email.of(email);
        Password password = Password.of(rawPassword, birthday);
        String encodedPassword = passwordEncoder.encrypt(password.getValue());

        try {
            User user = User.register(
                    userId, userName, encodedPassword, birth,
                    userEmail, WrongPasswordCount.init(), LocalDateTime.now()
            );
            userRepository.save(user);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException("이미 사용중인 ID 입니다.", ex);
        }
    }

    @Override
    @Transactional
    public void updatePassword(UserId userId, String currentRawPassword, String newRawPassword) {
        User user = findUser(userId);

        LocalDate birthday = user.getBirth().getValue();
        Password currentPassword = Password.of(currentRawPassword, birthday);
        Password newPassword = Password.of(newRawPassword, birthday);

        if (!passwordEncoder.matches(currentPassword.getValue(), user.getEncodedPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }

        if (passwordEncoder.matches(newPassword.getValue(), user.getEncodedPassword())) {
            throw new IllegalArgumentException("현재 비밀번호는 사용할 수 없습니다.");
        }

        String encodedNewPassword = passwordEncoder.encrypt(newPassword.getValue());
        User updatedUser = user.changePassword(encodedNewPassword);
        userRepository.save(updatedUser);
    }

    @Override
    public UserInfoResponse getUserInfo(UserId userId) {
        User user = findUser(userId);

        return new UserInfoResponse(
                user.getUserId().getValue(),
                maskName(user.getUserName().getValue()),
                user.getBirth().getValue(),
                user.getEmail().getValue()
        );
    }

    private User findUser(UserId userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }

    private String maskName(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        if (name.length() == 1) {
            return "*";
        }
        return name.substring(0, name.length() - 1) + "*";
    }
}
