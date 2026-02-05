package com.loopers.interfaces.api.dto;

public record UserInfoResponse(
        String loginId,
        String name,
        String birthday,
        String email
) {}
