package com.loopers.application.service;

import com.loopers.domain.model.*;
import com.loopers.domain.repository.UserRepository;
import com.loopers.domain.service.PasswordEncoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PasswordUpdateServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private PasswordUpdateService service;

    private static final LocalDate BIRTHDAY = LocalDate.of(1990, 5, 15);

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        service = new PasswordUpdateService(userRepository, passwordEncoder);
    }

    @Test
    @DisplayName("비밀번호 변경 성공")
    void updatePassword_success() {
        // given
        UserId userId = UserId.of("test1234");
        User user = createUser(userId, "encoded_current");
        Password currentPassword = Password.of("Current1!", BIRTHDAY);
        Password newPassword = Password.of("NewPass1!", BIRTHDAY);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Current1!", "encoded_current")).thenReturn(true);
        when(passwordEncoder.matches("NewPass1!", "encoded_current")).thenReturn(false);
        when(passwordEncoder.encrypt("NewPass1!")).thenReturn("encoded_new");

        // when and then
        assertThatNoException()
                .isThrownBy(() -> service.updatePassword(userId, currentPassword, newPassword));

        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("현재 비밀번호 불일치시 예외")
    void updatePassword_fail_wrong_current() {
        // given
        UserId userId = UserId.of("test1234");
        User user = createUser(userId, "encoded_current");
        Password wrongPassword = Password.of("WrongPw1!", BIRTHDAY);
        Password newPassword = Password.of("NewPass1!", BIRTHDAY);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPw1!", "encoded_current")).thenReturn(false);

        // when and then
        assertThatThrownBy(() -> service.updatePassword(userId, wrongPassword, newPassword))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("현재 비밀번호가 일치하지 않습니다");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("새 비밀번호가 현재와 동일하면 예외")
    void updatePassword_fail_same_password() {
        // given
        UserId userId = UserId.of("test1234");
        User user = createUser(userId, "encoded_current");
        Password currentPassword = Password.of("Current1!", BIRTHDAY);
        Password samePassword = Password.of("Current1!", BIRTHDAY);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Current1!", "encoded_current")).thenReturn(true);

        // when and then
        assertThatThrownBy(() -> service.updatePassword(userId, currentPassword, samePassword))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("현재 비밀번호는 사용할 수 없습니다");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("존재하지 않는 사용자면 예외")
    void updatePassword_fail_user_not_found() {
        // given
        UserId userId = UserId.of("notexist");
        Password currentPassword = Password.of("Current1!", BIRTHDAY);
        Password newPassword = Password.of("NewPass1!", BIRTHDAY);

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when and then
        assertThatThrownBy(() -> service.updatePassword(userId, currentPassword, newPassword))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("사용자를 찾을 수 없습니다");
    }

    private User createUser(UserId userId, String encodedPassword) {
        return User.reconstitute(
                1L,
                userId,
                UserName.of("홍길동"),
                encodedPassword,
                Birthday.of(BIRTHDAY),
                Email.of("test@example.com"),
                WrongPasswordCount.init(),
                LocalDateTime.now()
        );
    }
}
