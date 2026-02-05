package com.loopers.application.service;

import com.loopers.application.UserQueryUseCase;
import com.loopers.domain.model.User;
import com.loopers.domain.model.UserId;
import com.loopers.domain.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Service
public class UserQueryService implements UserQueryUseCase {

    private final UserRepository userRepository;

    /**
     * Creates a UserQueryService that uses the provided UserRepository to retrieve user data.
     *
     * @param userRepository repository used to fetch User entities by ID
     */
    public UserQueryService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Retrieve user information for the given user ID.
     *
     * @param userId the identifier of the user to query
     * @return a UserInfoResponse containing the user's ID, masked name, birth date formatted as "yyyyMMdd", and email
     * @throws IllegalArgumentException if no user exists for the provided ID
     */
    @Override
    public UserInfoResponse getUserInfo(UserId userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        return new UserInfoResponse(
                user.getUserId().getValue(),
                maskName(user.getUserName().getValue()),
                user.getBirth().getValue().format(DateTimeFormatter.ofPattern("yyyyMMdd")),
                user.getEmail().getValue()
        );
    }

    /**
     * Mask a person's name by replacing its last character with an asterisk.
     *
     * @param name the name to mask; may be null or empty
     * @return the masked name: `null` or empty string returned unchanged, `*` if the input has length 1, otherwise the input with its last character replaced by `*`
     */
    private String maskName(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        if (name.length() == 1) {
            return "*";
        }
        return name.substring(0, name.length() - 1) + "*";
    }
}