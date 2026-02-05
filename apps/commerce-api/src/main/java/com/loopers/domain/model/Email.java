package com.loopers.domain.model;

import lombok.Data;

import java.util.regex.Pattern;

@Data
public class Email {

    private static final Pattern PATTERN = Pattern.compile(
            "^[a-zA-Z0-9]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );

    private final String value;

    /**
     * Creates an Email instance with the given validated email string.
     *
     * @param value the validated, trimmed email string to store
     */
    private Email(String value) {
        this.value = value;
    }

    /**
     * Create an Email value object by validating and trimming the provided input.
     *
     * @param value the input email string; leading and trailing whitespace will be removed before validation
     * @return the validated Email containing the trimmed address
     * @throws IllegalArgumentException if {@code value} is null or blank, or if the trimmed value does not match the expected email format
     */
    public static Email of(String value) {
        if(value == null || value.isBlank()) {
            throw new IllegalArgumentException("이메일은 필수 입력값입니다.");
        }
        String trimmed = value.trim();
        if(!PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("올바른 이메일 형식이 아닙니다");
        }
        return new Email(trimmed);
    }
}