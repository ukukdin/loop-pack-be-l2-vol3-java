# 통합 테스트 & E2E 테스트 가이드

## 테스트 피라미드

```
        /\
       /  \      E2E 테스트 (적음)
      /----\     - 실제 HTTP 요청
     /      \    - 전체 시스템 검증
    /--------\
   /          \  통합 테스트 (중간)
  /            \ - 실제 DB 연동
 /--------------\- 여러 컴포넌트 협력
/                \
/------------------\ 단위 테스트 (많음)
                     - Mock 사용
                     - 빠른 실행
```

---

## 1. 통합 테스트 (Integration Test)

### 목적
- 실제 DB와 연동하여 Repository, Service 동작 검증
- 컴포넌트 간 협력이 제대로 되는지 확인

### 특징
| 항목 | 설명 |
|------|------|
| DB | Testcontainers (Docker 기반 실제 MySQL) |
| 속도 | 단위 테스트보다 느림 |
| 범위 | Service + Repository + DB |
| Mock | 외부 API만 Mock (DB는 실제 사용) |

### 설정

```java
// 테스트용 설정 클래스
@TestConfiguration
public class TestContainerConfig {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }
}
```

### 통합 테스트 예시

```java
package com.loopers.integration;

import com.loopers.application.service.UserRegisterService;
import com.loopers.domain.model.*;
import com.loopers.domain.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional  // 각 테스트 후 롤백
class UserRegisterServiceIntegrationTest {

    @Autowired
    private UserRegisterService userRegisterService;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("회원가입 후 DB에서 조회 성공")
    void register_and_find_success() {
        // given
        UserId userId = UserId.of("test1234");
        UserName userName = UserName.of("홍길동");
        Birthday birthday = Birthday.of(LocalDate.of(1990, 5, 15));
        Email email = Email.of("test@example.com");
        String encodedPassword = "encoded_password";

        // when
        userRegisterService.register(userId, userName, encodedPassword, birthday, email);

        // then
        var found = userRepository.findById(userId);
        assertThat(found).isPresent();
        assertThat(found.get().getUserId().getValue()).isEqualTo("test1234");
        assertThat(found.get().getUserName().getValue()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("중복 ID로 가입 시 예외 - 실제 DB 검증")
    void register_fail_duplicate_id() {
        // given
        UserId userId = UserId.of("test1234");
        UserName userName = UserName.of("홍길동");
        Birthday birthday = Birthday.of(LocalDate.of(1990, 5, 15));
        Email email = Email.of("test@example.com");
        String encodedPassword = "encoded_password";

        // 첫 번째 가입
        userRegisterService.register(userId, userName, encodedPassword, birthday, email);

        // when & then - 두 번째 가입 시도
        assertThatThrownBy(() ->
            userRegisterService.register(userId, userName, encodedPassword, birthday, email))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 사용중인 ID");
    }
}
```

### Repository 통합 테스트

```java
package com.loopers.integration;

import com.loopers.domain.model.*;
import com.loopers.domain.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserRepositoryIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("User 저장 및 조회")
    void save_and_findById() {
        // given
        User user = User.register(
                UserId.of("test1234"),
                UserName.of("홍길동"),
                "encoded_password",
                Birthday.of(LocalDate.of(1990, 5, 15)),
                Email.of("test@example.com"),
                WrongPasswordCount.init(),
                LocalDateTime.now()
        );

        // when
        User saved = userRepository.save(user);
        var found = userRepository.findById(UserId.of("test1234"));

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(found).isPresent();
        assertThat(found.get().getUserId().getValue()).isEqualTo("test1234");
    }

    @Test
    @DisplayName("존재하는 ID 확인")
    void existsById_true() {
        // given
        User user = User.register(
                UserId.of("exist123"),
                UserName.of("홍길동"),
                "encoded_password",
                Birthday.of(LocalDate.of(1990, 5, 15)),
                Email.of("test@example.com"),
                WrongPasswordCount.init(),
                LocalDateTime.now()
        );
        userRepository.save(user);

        // when
        boolean exists = userRepository.existsById(UserId.of("exist123"));

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 ID 확인")
    void existsById_false() {
        // when
        boolean exists = userRepository.existsById(UserId.of("notexist"));

        // then
        assertThat(exists).isFalse();
    }
}
```

---

## 2. E2E 테스트 (End-to-End Test)

### 목적
- 실제 HTTP 요청으로 전체 시스템 검증
- 클라이언트 관점에서 API 동작 확인

### 특징
| 항목 | 설명 |
|------|------|
| 범위 | Controller → Service → Repository → DB |
| 요청 | 실제 HTTP 요청 (RestAssured / TestRestTemplate) |
| 속도 | 가장 느림 |
| 검증 | 응답 상태코드, 응답 바디, 헤더 등 |

### E2E 테스트 예시 - TestRestTemplate

```java
package com.loopers.e2e;

import com.loopers.interfaces.api.dto.UserRegisterRequest;
import com.loopers.interfaces.api.dto.UserInfoResponse;
import com.loopers.interfaces.api.dto.PasswordUpdateRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserApiE2ETest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String baseUrl() {
        return "http://localhost:" + port + "/api/v1/users";
    }

    @Test
    @DisplayName("회원가입 API 성공")
    void register_success() {
        // given
        UserRegisterRequest request = new UserRegisterRequest(
                "newuser1",
                "Password1!",
                "홍길동",
                LocalDate.of(1990, 5, 15),
                "test@example.com"
        );

        // when
        ResponseEntity<Void> response = restTemplate.postForEntity(
                baseUrl() + "/register",
                request,
                Void.class
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("회원가입 API 실패 - 중복 ID")
    void register_fail_duplicate() {
        // given - 먼저 가입
        UserRegisterRequest request = new UserRegisterRequest(
                "dupuser1",
                "Password1!",
                "홍길동",
                LocalDate.of(1990, 5, 15),
                "test@example.com"
        );
        restTemplate.postForEntity(baseUrl() + "/register", request, Void.class);

        // when - 같은 ID로 다시 가입 시도
        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl() + "/register",
                request,
                String.class
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("내 정보 조회 API 성공")
    void getMyInfo_success() {
        // given - 먼저 가입
        String loginId = "infouser";
        String password = "Password1!";

        UserRegisterRequest registerRequest = new UserRegisterRequest(
                loginId,
                password,
                "홍길동",
                LocalDate.of(1990, 5, 15),
                "info@example.com"
        );
        restTemplate.postForEntity(baseUrl() + "/register", registerRequest, Void.class);

        // when
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Loopers-LoginId", loginId);
        headers.set("X-Loopers-LoginPw", password);

        ResponseEntity<UserInfoResponse> response = restTemplate.exchange(
                baseUrl() + "/me",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                UserInfoResponse.class
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().loginId()).isEqualTo(loginId);
        assertThat(response.getBody().name()).isEqualTo("홍길*");  // 마스킹
    }

    @Test
    @DisplayName("비밀번호 수정 API 성공")
    void updatePassword_success() {
        // given - 먼저 가입
        String loginId = "pwduser1";
        String oldPassword = "OldPass1!";
        String newPassword = "NewPass1!";

        UserRegisterRequest registerRequest = new UserRegisterRequest(
                loginId,
                oldPassword,
                "홍길동",
                LocalDate.of(1990, 5, 15),
                "pwd@example.com"
        );
        restTemplate.postForEntity(baseUrl() + "/register", registerRequest, Void.class);

        // when
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Loopers-LoginId", loginId);
        headers.set("X-Loopers-LoginPw", oldPassword);
        headers.setContentType(MediaType.APPLICATION_JSON);

        PasswordUpdateRequest updateRequest = new PasswordUpdateRequest(
                oldPassword,
                newPassword
        );

        ResponseEntity<Void> response = restTemplate.exchange(
                baseUrl() + "/me/password",
                HttpMethod.PUT,
                new HttpEntity<>(updateRequest, headers),
                Void.class
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("비밀번호 수정 API 실패 - 현재 비밀번호 불일치")
    void updatePassword_fail_wrong_current() {
        // given - 먼저 가입
        String loginId = "pwduser2";
        String correctPassword = "Correct1!";

        UserRegisterRequest registerRequest = new UserRegisterRequest(
                loginId,
                correctPassword,
                "홍길동",
                LocalDate.of(1990, 5, 15),
                "pwd2@example.com"
        );
        restTemplate.postForEntity(baseUrl() + "/register", registerRequest, Void.class);

        // when
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Loopers-LoginId", loginId);
        headers.set("X-Loopers-LoginPw", correctPassword);
        headers.setContentType(MediaType.APPLICATION_JSON);

        PasswordUpdateRequest updateRequest = new PasswordUpdateRequest(
                "WrongPwd1!",  // 틀린 현재 비밀번호
                "NewPass1!"
        );

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl() + "/me/password",
                HttpMethod.PUT,
                new HttpEntity<>(updateRequest, headers),
                String.class
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
```

### E2E 테스트 예시 - RestAssured (더 가독성 좋음)

```java
package com.loopers.e2e;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class UserApiRestAssuredE2ETest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/api/v1/users";
    }

    @Test
    @DisplayName("회원가입 API 성공")
    void register_success() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "loginId": "restuser",
                    "password": "Password1!",
                    "name": "홍길동",
                    "birthday": "1990-05-15",
                    "email": "rest@example.com"
                }
                """)
        .when()
            .post("/register")
        .then()
            .statusCode(200);
    }

    @Test
    @DisplayName("내 정보 조회 API 성공")
    void getMyInfo_success() {
        // 먼저 가입
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "loginId": "restinfo",
                    "password": "Password1!",
                    "name": "홍길동",
                    "birthday": "1990-05-15",
                    "email": "restinfo@example.com"
                }
                """)
        .when()
            .post("/register")
        .then()
            .statusCode(200);

        // 조회
        given()
            .header("X-Loopers-LoginId", "restinfo")
            .header("X-Loopers-LoginPw", "Password1!")
        .when()
            .get("/me")
        .then()
            .statusCode(200)
            .body("loginId", equalTo("restinfo"))
            .body("name", equalTo("홍길*"));  // 마스킹 확인
    }
}
```

---

## 3. 테스트 파일 위치

```
src/test/java/com/loopers/
├── domain/model/           # 단위 테스트
├── application/service/    # 단위 테스트 (mock)
├── infrastructure/         # 단위 테스트
├── integration/            # 통합 테스트
│   ├── UserRegisterServiceIntegrationTest.java
│   └── UserRepositoryIntegrationTest.java
└── e2e/                    # E2E 테스트
    └── UserApiE2ETest.java
```

---

## 4. 테스트 실행 명령어

```bash
# 전체 테스트
./gradlew :apps:commerce-api:test

# 단위 테스트만
./gradlew :apps:commerce-api:test --tests "com.loopers.domain.*" --tests "com.loopers.application.*"

# 통합 테스트만
./gradlew :apps:commerce-api:test --tests "com.loopers.integration.*"

# E2E 테스트만
./gradlew :apps:commerce-api:test --tests "com.loopers.e2e.*"
```

---

## 5. 테스트 비교 요약

| 구분 | 단위 테스트 | 통합 테스트 | E2E 테스트 |
|------|------------|------------|-----------|
| **범위** | 단일 클래스 | 여러 컴포넌트 | 전체 시스템 |
| **DB** | Mock | 실제 (Testcontainers) | 실제 (Testcontainers) |
| **HTTP** | X | X | O |
| **속도** | 빠름 | 중간 | 느림 |
| **수량** | 많음 | 중간 | 적음 |
| **목적** | 로직 검증 | 협력 검증 | 사용자 시나리오 검증 |

---

## 6. 주의사항

1. **@Transactional**: 통합 테스트에서 사용하면 테스트 후 자동 롤백
2. **@TestInstance(PER_CLASS)**: E2E에서 테스트 간 데이터 공유 시 사용
3. **테스트 격리**: 각 테스트는 독립적으로 실행 가능해야 함
4. **테스트 데이터**: 고유한 ID 사용 (예: `testuser1`, `testuser2`)
5. **Docker 필수**: Testcontainers 사용 시 Docker 실행 필요
