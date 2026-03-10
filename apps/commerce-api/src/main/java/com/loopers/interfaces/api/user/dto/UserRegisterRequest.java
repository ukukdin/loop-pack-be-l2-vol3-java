package com.loopers.interfaces.api.user.dto;

import com.loopers.application.user.RegisterUseCase.RegisterCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record UserRegisterRequest(
        @NotBlank(message = "로그인 ID는 필수입니다.")
        String loginId,
        @NotBlank(message = "비밀번호는 필수입니다.")
        String password,
        @NotBlank(message = "이름은 필수입니다.")
        String name,
        @NotNull(message = "생년월일은 필수입니다.")
        LocalDate birthday,
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        String email
) {
    public RegisterCommand toCommand() {
        return new RegisterCommand(loginId, name, password, birthday, email);
    }
}
