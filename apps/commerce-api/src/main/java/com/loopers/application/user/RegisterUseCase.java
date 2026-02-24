package com.loopers.application.user;

import java.time.LocalDate;

public interface RegisterUseCase {

    void register(String loginId, String name, String rawPassword, LocalDate birthday, String email);
}
