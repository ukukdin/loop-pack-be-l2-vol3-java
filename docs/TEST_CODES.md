# 테스트 코드 가이드

## 1. Domain Model 테스트

### PasswordTest.java

```java
package com.loopers.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordTest {

    private static final LocalDate BIRTHDAY = LocalDate.of(1990, 5, 15);

    @Test
    @DisplayName("유효한 비밀번호 생성 성공")
    void create_success() {
        // given
        String rawPassword = "Valid123!";

        // when
        Password password = Password.of(rawPassword, BIRTHDAY);

        // then
        assertThat(password.getValue()).isEqualTo("Valid123!");
    }

    @Test
    @DisplayName("비밀번호 null이면 예외")
    void create_fail_null() {
        // given
        String rawPassword = null;

        // when & then
        assertThatThrownBy(() -> Password.of(rawPassword, BIRTHDAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("비밀번호는 필수 입력값입니다.");
    }

    @Test
    @DisplayName("비밀번호 공백이면 예외")
    void create_fail_blank() {
        // given
        String rawPassword = "   ";

        // when & then
        assertThatThrownBy(() -> Password.of(rawPassword, BIRTHDAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("비밀번호는 필수 입력값입니다.");
    }

    @Test
    @DisplayName("비밀번호 8자 미만이면 예외")
    void create_fail_too_short() {
        // given
        String rawPassword = "Short1!";

        // when & then
        assertThatThrownBy(() -> Password.of(rawPassword, BIRTHDAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("8~16자리");
    }

    @Test
    @DisplayName("비밀번호 16자 초과면 예외")
    void create_fail_too_long() {
        // given
        String rawPassword = "ThisIsVeryLongPassword123!";

        // when & then
        assertThatThrownBy(() -> Password.of(rawPassword, BIRTHDAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("8~16자리");
    }

    @Test
    @DisplayName("생년월일(yyyyMMdd) 포함시 예외")
    void create_fail_contains_birthday_yyyyMMdd() {
        // given
        String rawPassword = "Ab19900515!";

        // when & then
        assertThatThrownBy(() -> Password.of(rawPassword, BIRTHDAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("생년월일");
    }

    @Test
    @DisplayName("생년월일(yyMMdd) 포함시 예외")
    void create_fail_contains_birthday_yyMMdd() {
        // given
        String rawPassword = "Abcd900515!";

        // when & then
        assertThatThrownBy(() -> Password.of(rawPassword, BIRTHDAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("생년월일");
    }

    @Test
    @DisplayName("생년월일(MMdd) 포함시 예외")
    void create_fail_contains_birthday_MMdd() {
        // given
        String rawPassword = "Abcd0515ab!";

        // when & then
        assertThatThrownBy(() -> Password.of(rawPassword, BIRTHDAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("생년월일");
    }

    @Test
    @DisplayName("생년월일 없이 비밀번호 생성 가능")
    void create_success_without_birthday() {
        // given
        String rawPassword = "Valid123!";
        LocalDate birthday = null;

        // when
        Password password = Password.of(rawPassword, birthday);

        // then
        assertThat(password.getValue()).isEqualTo("Valid123!");
    }
}
```

### UserIdTest.java

```java
package com.loopers.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserIdTest {

    @Test
    @DisplayName("유효한 로그인 ID 생성 성공")
    void create_success() {
        // given
        String value = "test1234";

        // when
        UserId userId = UserId.of(value);

        // then
        assertThat(userId.getValue()).isEqualTo("test1234");
    }

    @Test
    @DisplayName("로그인 ID null이면 예외")
    void create_fail_null() {
        // given
        String value = null;

        // when & then
        assertThatThrownBy(() -> UserId.of(value))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("로그인 ID는 필수 입력값입니다.");
    }

    @Test
    @DisplayName("로그인 ID 공백이면 예외")
    void create_fail_blank() {
        // given
        String value = "   ";

        // when & then
        assertThatThrownBy(() -> UserId.of(value))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("로그인 ID는 필수 입력값입니다.");
    }

    @Test
    @DisplayName("로그인 ID 4자 미만이면 예외")
    void create_fail_too_short() {
        // given
        String value = "abc";

        // when & then
        assertThatThrownBy(() -> UserId.of(value))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("4~10자");
    }

    @Test
    @DisplayName("로그인 ID 10자 초과면 예외")
    void create_fail_too_long() {
        // given
        String value = "abcdefghijk";

        // when & then
        assertThatThrownBy(() -> UserId.of(value))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("4~10자");
    }

    @Test
    @DisplayName("로그인 ID 특수문자 포함시 예외")
    void create_fail_special_char() {
        // given
        String value = "test@123";

        // when & then
        assertThatThrownBy(() -> UserId.of(value))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("영문");
    }

    @Test
    @DisplayName("로그인 ID 공백 trim 처리")
    void create_success_with_trim() {
        // given
        String value = "  test1234  ";

        // when
        UserId userId = UserId.of(value);

        // then
        assertThat(userId.getValue()).isEqualTo("test1234");
    }
}
```

### UserNameTest.java

```java
package com.loopers.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserNameTest {

    @Test
    @DisplayName("유효한 이름 생성 성공 - 한글")
    void create_success_korean() {
        // given
        String value = "홍길동";

        // when
        UserName userName = UserName.of(value);

        // then
        assertThat(userName.getValue()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("유효한 이름 생성 성공 - 영문")
    void create_success_english() {
        // given
        String value = "John";

        // when
        UserName userName = UserName.of(value);

        // then
        assertThat(userName.getValue()).isEqualTo("John");
    }

    @Test
    @DisplayName("이름 null이면 예외")
    void create_fail_null() {
        // given
        String value = null;

        // when & then
        assertThatThrownBy(() -> UserName.of(value))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이름은 필수 값입니다.");
    }

    @Test
    @DisplayName("이름 2자 미만이면 예외")
    void create_fail_too_short() {
        // given
        String value = "홍";

        // when & then
        assertThatThrownBy(() -> UserName.of(value))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("2~20자");
    }

    @Test
    @DisplayName("이름 20자 초과면 예외")
    void create_fail_too_long() {
        // given
        String value = "가나다라마바사아자차카타파하가나다라마바사";

        // when & then
        assertThatThrownBy(() -> UserName.of(value))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("2~20자");
    }

    @Test
    @DisplayName("이름 특수문자 포함시 예외")
    void create_fail_special_char() {
        // given
        String value = "홍길동!";

        // when & then
        assertThatThrownBy(() -> UserName.of(value))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("한글 또는 영문");
    }
}
```

### EmailTest.java

```java
package com.loopers.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailTest {

    @Test
    @DisplayName("유효한 이메일 생성 성공")
    void create_success() {
        // given
        String value = "test@example.com";

        // when
        Email email = Email.of(value);

        // then
        assertThat(email.getValue()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("이메일 null이면 예외")
    void create_fail_null() {
        // given
        String value = null;

        // when & then
        assertThatThrownBy(() -> Email.of(value))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이메일은 필수 입력값입니다.");
    }

    @Test
    @DisplayName("이메일 공백이면 예외")
    void create_fail_blank() {
        // given
        String value = "   ";

        // when & then
        assertThatThrownBy(() -> Email.of(value))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이메일은 필수 입력값입니다.");
    }

    @Test
    @DisplayName("이메일 형식 오류 - @ 없음")
    void create_fail_no_at() {
        // given
        String value = "testexample.com";

        // when & then
        assertThatThrownBy(() -> Email.of(value))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이메일 형식");
    }

    @Test
    @DisplayName("이메일 형식 오류 - 도메인 없음")
    void create_fail_no_domain() {
        // given
        String value = "test@";

        // when & then
        assertThatThrownBy(() -> Email.of(value))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이메일 형식");
    }

    @Test
    @DisplayName("이메일 공백 trim 처리")
    void create_success_with_trim() {
        // given
        String value = "  test@example.com  ";

        // when
        Email email = Email.of(value);

        // then
        assertThat(email.getValue()).isEqualTo("test@example.com");
    }
}
```

### BirthdayTest.java

```java
package com.loopers.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BirthdayTest {

    @Test
    @DisplayName("유효한 생년월일 생성 성공")
    void create_success() {
        // given
        LocalDate date = LocalDate.of(1990, 5, 15);

        // when
        Birthday birthday = Birthday.of(date);

        // then
        assertThat(birthday.getValue()).isEqualTo(date);
    }

    @Test
    @DisplayName("생년월일 null이면 예외")
    void create_fail_null() {
        // given
        LocalDate date = null;

        // when & then
        assertThatThrownBy(() -> Birthday.of(date))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("생년월일은 필수 입력값입니다.");
    }

    @Test
    @DisplayName("생년월일 미래 날짜면 예외")
    void create_fail_future() {
        // given
        LocalDate future = LocalDate.now().plusDays(1);

        // when & then
        assertThatThrownBy(() -> Birthday.of(future))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("미래 날짜");
    }

    @Test
    @DisplayName("생년월일 1900년 이전이면 예외")
    void create_fail_before_1900() {
        // given
        LocalDate old = LocalDate.of(1899, 12, 31);

        // when & then
        assertThatThrownBy(() -> Birthday.of(old))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1900년");
    }

    @Test
    @DisplayName("생년월일 오늘 날짜 가능")
    void create_success_today() {
        // given
        LocalDate today = LocalDate.now();

        // when
        Birthday birthday = Birthday.of(today);

        // then
        assertThat(birthday.getValue()).isEqualTo(today);
    }
}
```

### WrongPasswordCountTest.java

```java
package com.loopers.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WrongPasswordCountTest {

    @Test
    @DisplayName("초기값 0으로 생성")
    void init_success() {
        // given & when
        WrongPasswordCount count = WrongPasswordCount.init();

        // then
        assertThat(count.getValue()).isEqualTo(0);
    }

    @Test
    @DisplayName("유효한 값으로 생성")
    void of_success() {
        // given
        int value = 3;

        // when
        WrongPasswordCount count = WrongPasswordCount.of(value);

        // then
        assertThat(count.getValue()).isEqualTo(3);
    }

    @Test
    @DisplayName("음수값이면 예외")
    void of_fail_negative() {
        // given
        int value = -1;

        // when & then
        assertThatThrownBy(() -> WrongPasswordCount.of(value))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("음수");
    }

    @Test
    @DisplayName("카운트 증가")
    void increment_success() {
        // given
        WrongPasswordCount count = WrongPasswordCount.init();

        // when
        WrongPasswordCount incremented = count.increment();

        // then
        assertThat(incremented.getValue()).isEqualTo(1);
    }

    @Test
    @DisplayName("카운트 리셋")
    void reset_success() {
        // given
        WrongPasswordCount count = WrongPasswordCount.of(3);

        // when
        WrongPasswordCount reset = count.reset();

        // then
        assertThat(reset.getValue()).isEqualTo(0);
    }

    @Test
    @DisplayName("5회 이상 실패시 잠금")
    void isLocked_true() {
        // given
        WrongPasswordCount count = WrongPasswordCount.of(5);

        // when
        boolean locked = count.isLocked();

        // then
        assertThat(locked).isTrue();
    }

    @Test
    @DisplayName("5회 미만 실패시 잠금 안됨")
    void isLocked_false() {
        // given
        WrongPasswordCount count = WrongPasswordCount.of(4);

        // when
        boolean locked = count.isLocked();

        // then
        assertThat(locked).isFalse();
    }
}
```

---

## 2. Infrastructure 테스트

### Sha256PasswordEncoderTest.java

```java
package com.loopers.infrastructure.security;

import com.loopers.domain.service.PasswordEncoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Sha256PasswordEncoderTest {

    private PasswordEncoder encoder;

    @BeforeEach
    void setUp() {
        encoder = new Sha256PasswordEncoder();
    }

    @Test
    @DisplayName("비밀번호 암호화 성공")
    void encrypt_success() {
        // given
        String rawPassword = "password123!";

        // when
        String encoded = encoder.encrypt(rawPassword);

        // then
        assertThat(encoded).isNotNull();
        assertThat(encoded).contains(":");
        assertThat(encoded).isNotEqualTo(rawPassword);
    }

    @Test
    @DisplayName("같은 비밀번호도 매번 다른 해시 생성 (salt)")
    void encrypt_different_hash_each_time() {
        // given
        String rawPassword = "password123!";

        // when
        String encoded1 = encoder.encrypt(rawPassword);
        String encoded2 = encoder.encrypt(rawPassword);

        // then
        assertThat(encoded1).isNotEqualTo(encoded2);
    }

    @Test
    @DisplayName("비밀번호 매칭 성공")
    void matches_success() {
        // given
        String rawPassword = "password123!";
        String encoded = encoder.encrypt(rawPassword);

        // when
        boolean result = encoder.matches(rawPassword, encoded);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("틀린 비밀번호 매칭 실패")
    void matches_fail_wrong_password() {
        // given
        String encoded = encoder.encrypt("password123!");
        String wrongPassword = "wrongPassword!";

        // when
        boolean result = encoder.matches(wrongPassword, encoded);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("잘못된 형식의 인코딩된 비밀번호")
    void matches_fail_invalid_format() {
        // given
        String rawPassword = "password";
        String invalidEncoded = "invalid_format";

        // when
        boolean result = encoder.matches(rawPassword, invalidEncoded);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("빈 salt 형식 처리")
    void matches_fail_empty_salt() {
        // given
        String rawPassword = "password";
        String invalidEncoded = ":hash";

        // when
        boolean result = encoder.matches(rawPassword, invalidEncoded);

        // then
        assertThat(result).isFalse();
    }
}
```

---

## 3. Application Service 테스트

### UserRegisterServiceTest.java

```java
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
```

### UserQueryServiceTest.java

```java
package com.loopers.application.service;

import com.loopers.domain.model.*;
import com.loopers.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserQueryServiceTest {

    private UserRepository userRepository;
    private UserQueryService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        service = new UserQueryService(userRepository);
    }

    @Test
    @DisplayName("내 정보 조회 성공")
    void getUserInfo_success() {
        // given
        UserId userId = UserId.of("test1234");
        User user = User.reconstitute(
                1L,
                userId,
                UserName.of("홍길동"),
                "encoded_password",
                Birthday.of(LocalDate.of(1990, 5, 15)),
                Email.of("test@example.com"),
                WrongPasswordCount.init(),
                LocalDateTime.now()
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // when
        var result = service.getUserInfo(userId);

        // then
        assertThat(result.loginId()).isEqualTo("test1234");
        assertThat(result.maskedName()).isEqualTo("홍길*");
        assertThat(result.birthday()).isEqualTo("19900515");
        assertThat(result.email()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("이름 마스킹 - 2자")
    void getUserInfo_maskedName_2chars() {
        // given
        UserId userId = UserId.of("test1234");
        User user = User.reconstitute(
                1L,
                userId,
                UserName.of("홍길"),
                "encoded_password",
                Birthday.of(LocalDate.of(1990, 5, 15)),
                Email.of("test@example.com"),
                WrongPasswordCount.init(),
                LocalDateTime.now()
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // when
        var result = service.getUserInfo(userId);

        // then
        assertThat(result.maskedName()).isEqualTo("홍*");
    }

    @Test
    @DisplayName("존재하지 않는 사용자 조회시 예외")
    void getUserInfo_fail_not_found() {
        // given
        UserId userId = UserId.of("notexist");

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.getUserInfo(userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("사용자를 찾을 수 없습니다");
    }
}
```

### PasswordUpdateServiceTest.java

```java
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

        // when & then
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

        // when & then
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

        // when & then
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

        // when & then
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
```

---

## 4. 테스트 파일 위치

```
src/test/java/com/loopers/
├── domain/
│   └── model/
│       ├── PasswordTest.java
│       ├── UserIdTest.java
│       ├── UserNameTest.java
│       ├── EmailTest.java
│       ├── BirthdayTest.java
│       └── WrongPasswordCountTest.java
├── application/
│   └── service/
│       ├── UserRegisterServiceTest.java
│       ├── UserQueryServiceTest.java
│       └── PasswordUpdateServiceTest.java
└── infrastructure/
    └── security/
        └── Sha256PasswordEncoderTest.java
```

---

## 5. 테스트 실행 명령어

```bash
# 전체 테스트
./gradlew :apps:commerce-api:test

# 도메인 테스트만
./gradlew :apps:commerce-api:test --tests "com.loopers.domain.*"

# 특정 테스트 클래스만
./gradlew :apps:commerce-api:test --tests "com.loopers.domain.model.PasswordTest"

# 특정 테스트 메서드만
./gradlew :apps:commerce-api:test --tests "com.loopers.domain.model.PasswordTest.create_success"
```
