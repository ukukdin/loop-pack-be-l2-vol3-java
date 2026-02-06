package com.loopers.interfaces.api;

import com.loopers.application.PasswordUpdateUseCase;
import com.loopers.application.RegisterUseCase;
import com.loopers.application.UserQueryUseCase;
import com.loopers.domain.model.*;
import com.loopers.domain.service.PasswordEncoder;
import com.loopers.interfaces.api.dto.PasswordUpdateRequest;
import com.loopers.interfaces.api.dto.UserInfoResponse;
import com.loopers.interfaces.api.dto.UserRegisterRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final RegisterUseCase registerUseCase;
    private final UserQueryUseCase userQueryUseCase;
    private final PasswordUpdateUseCase passwordUpdateUseCase;
    private final PasswordEncoder passwordEncoder;

    public UserController(
            RegisterUseCase registerUseCase,
            UserQueryUseCase userQueryUseCase,
            PasswordUpdateUseCase passwordUpdateUseCase,
            PasswordEncoder passwordEncoder
    ) {
        this.registerUseCase = registerUseCase;
        this.userQueryUseCase = userQueryUseCase;
        this.passwordUpdateUseCase = passwordUpdateUseCase;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody UserRegisterRequest request) {
        UserId userId = UserId.of(request.loginId());
        UserName userName = UserName.of(request.name());
        Birthday birthday = Birthday.of(request.birthday());
        Email email = Email.of(request.email());
        Password password = Password.of(request.password(), request.birthday());
        String encodedPassword = passwordEncoder.encrypt(password.getValue());

        registerUseCase.register(userId, userName, encodedPassword, birthday, email);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserInfoResponse> getMyInfo(
            @NotBlank @RequestHeader("X-Loopers-LoginId") String loginId,
            @NotBlank @RequestHeader("X-Loopers-LoginPw") String loginPw
    ) {
        UserId userId = UserId.of(loginId);
        var userInfo = userQueryUseCase.getUserInfo(userId);

        return ResponseEntity.ok(UserInfoResponse.from(userInfo));
    }

    @PutMapping("/me/password")
    public ResponseEntity<Void> updatePassword(
            @NotBlank @RequestHeader("X-Loopers-LoginId") String loginId,
            @NotBlank @RequestHeader("X-Loopers-LoginPw") String loginPw,
            @Valid @RequestBody PasswordUpdateRequest request
    ) {
        UserId userId = UserId.of(loginId);

        // 현재 비밀번호와 새 비밀번호 생성 (생년월일 검증을 위해 사용자 정보 필요)
        var userInfo = userQueryUseCase.getUserInfo(userId);

        Password currentPassword = Password.of(request.currentPassword(), userInfo.birthday());
        Password newPassword = Password.of(request.newPassword(), userInfo.birthday());

        passwordUpdateUseCase.updatePassword(userId, currentPassword, newPassword);
        return ResponseEntity.ok().build();
    }
}
