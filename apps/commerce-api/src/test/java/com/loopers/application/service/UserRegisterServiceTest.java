package com.loopers.application.service;

import com.loopers.domain.model.*;
import com.loopers.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserRegisterServiceTest {

    private UserRepository userRepository;
    private UserRegisterService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        service = new UserRegisterService(userRepository);
    }

    @Test
    @DisplayName("회원가입 성공")
    void register_success() {
        // given
        UserId userId = UserId.of("test1234");
        UserName userName = UserName.of("홍길동");
        Birthday birthday = Birthday.of(LocalDate.of(1990, 5, 15));
        Email email = Email.of("test@example.com");
        String encodedPassword = "encoded_password";

        when(userRepository.existsById(userId)).thenReturn(false);

        // when & then
        assertThatNoException()
                .isThrownBy(() -> service.register(userId, userName, encodedPassword, birthday, email));

        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("중복된 ID로 가입시 예외")
    void register_fail_duplicated_id() {
        // given
        UserId duplicatedId = UserId.of("test1234");
        UserName userName = UserName.of("홍길동");
        Birthday birthday = Birthday.of(LocalDate.of(1990, 5, 15));
        Email email = Email.of("test@example.com");
        String encodedPassword = "encoded_password";

        when(userRepository.existsById(duplicatedId)).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> service.register(duplicatedId, userName, encodedPassword, birthday, email))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 사용중인 ID");

        verify(userRepository, never()).save(any(User.class));
    }
}
