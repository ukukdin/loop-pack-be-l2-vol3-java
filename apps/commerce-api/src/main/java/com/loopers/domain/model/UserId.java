package com.loopers.domain.model;

import lombok.Data;

import java.util.regex.Pattern;

@Data
public class UserId {

    private static final Pattern PATTERN = Pattern.compile("^[a-zA-Z0-9]{4,10}$");

    private final String value;

    /**
 * Initializes a UserId with the specified identifier value.
 *
 * @param value the identifier string to store in this UserId
 */
private UserId(String value) {this.value = value;}

    /**
     * Create a validated UserId from the given string.
     *
     * @param value the input string; leading and trailing whitespace will be trimmed before validation
     * @return a UserId containing the trimmed value
     * @throws IllegalArgumentException if {@code value} is null or blank, or if the trimmed value does not consist of 4 to 10 ASCII letters or digits
     */
    public static UserId of(String value) {
        if(value == null || value.isBlank()) {
            throw new IllegalArgumentException("로그인 ID는 필수 입력값입니다.");
        }
        String trimmed = value.trim();
        if(!PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException(
                    "로그인 ID는 4~10자의 영문 소문자, 숫자만 가능합니다.");
        }
        return new UserId(trimmed);
    }
}