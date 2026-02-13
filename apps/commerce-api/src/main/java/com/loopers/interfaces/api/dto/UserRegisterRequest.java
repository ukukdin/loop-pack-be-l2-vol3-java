package com.loopers.interfaces.api.dto;

import java.time.LocalDate;

public record UserRegisterRequest(
        String loginId,
        String password,
        String name,
        LocalDate birthday,
        String email
) {}
