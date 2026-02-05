package com.loopers.application.service;

import com.loopers.application.PasswordUpdateUseCase;
import com.loopers.domain.model.Password;
import com.loopers.domain.model.User;
import com.loopers.domain.model.UserId;
import com.loopers.domain.repository.UserRepository;
import com.loopers.domain.service.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PasswordUpdateService implements PasswordUpdateUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Creates a PasswordUpdateService with the given dependencies.
     *
     * @param userRepository repository used to load and save users
     * @param passwordEncoder encoder used to verify and encode passwords
     */
    public PasswordUpdateService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Updates a user's password after validating the current password and ensuring the new password differs.
     *
     * @param userId the identifier of the user whose password will be updated
     * @param currentPassword the user's current password used for verification
     * @param newPassword the new password to set for the user
     * @throws IllegalArgumentException if the user is not found, if the current password does not match, or if the new password equals the current password
     */
    @Override
    public void updatePassword(UserId userId, Password currentPassword, Password newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 기존 비밀번호 확인
        if (!passwordEncoder.matches(currentPassword.getValue(), user.getEncodedPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }

        // 새 비밀번호가 현재 비밀번호와 동일한지 확인
        if (passwordEncoder.matches(newPassword.getValue(), user.getEncodedPassword())) {
            throw new IllegalArgumentException("현재 비밀번호는 사용할 수 없습니다.");
        }

        // 비밀번호 암호화 후 저장
        String encodedNewPassword = passwordEncoder.encrypt(newPassword.getValue());
        User updatedUser = user.changePassword(encodedNewPassword);
        userRepository.save(updatedUser);
    }
}