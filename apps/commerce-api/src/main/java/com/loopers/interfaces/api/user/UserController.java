package com.loopers.interfaces.api.user;

import com.loopers.application.user.PasswordUpdateUseCase;
import com.loopers.application.user.RegisterUseCase;
import com.loopers.application.user.UserQueryUseCase;
import com.loopers.domain.model.user.UserId;
import com.loopers.interfaces.api.user.dto.PasswordUpdateRequest;
import com.loopers.interfaces.api.user.dto.UserInfoResponse;
import com.loopers.interfaces.api.user.dto.UserRegisterRequest;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final RegisterUseCase registerUseCase;
    private final UserQueryUseCase userQueryUseCase;
    private final PasswordUpdateUseCase passwordUpdateUseCase;

    public UserController(
            RegisterUseCase registerUseCase,
            UserQueryUseCase userQueryUseCase,
            PasswordUpdateUseCase passwordUpdateUseCase
    ) {
        this.registerUseCase = registerUseCase;
        this.userQueryUseCase = userQueryUseCase;
        this.passwordUpdateUseCase = passwordUpdateUseCase;
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody UserRegisterRequest request) {
        registerUseCase.register(
                request.loginId(),
                request.name(),
                request.password(),
                request.birthday(),
                request.email()
        );
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserInfoResponse> getMyInfo(HttpServletRequest request) {
        UserId userId = (UserId) request.getAttribute("authenticatedUserId");

        var userInfo = userQueryUseCase.getUserInfo(userId);
        return ResponseEntity.ok(UserInfoResponse.from(userInfo));
    }

    @PutMapping("/me/password")
    public ResponseEntity<Void> updatePassword(
            HttpServletRequest request,
            @RequestBody PasswordUpdateRequest passwordUpdateRequest
    ) {
        UserId userId = (UserId) request.getAttribute("authenticatedUserId");

        passwordUpdateUseCase.updatePassword(
                userId,
                passwordUpdateRequest.currentPassword(),
                passwordUpdateRequest.newPassword()
        );
        return ResponseEntity.ok().build();
    }
}
