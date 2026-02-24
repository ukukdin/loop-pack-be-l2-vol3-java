package com.loopers.interfaces.api.user.dto;

import java.time.LocalDate;

public record UserRegisterRequest(
        String loginId,
        String password,
        String name,
        LocalDate birthday,
        String email
) {}
