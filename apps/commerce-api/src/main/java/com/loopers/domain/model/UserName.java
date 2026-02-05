package com.loopers.domain.model;

import lombok.Data;

import java.util.regex.Pattern;

@Data
public class UserName {

    private static final Pattern PATTERN = Pattern.compile("^[a-zA-Z0-9가-힣]{2,20}$");

    private final String value;
    /**
 * Constructs a UserName that holds the given string.
 *
 * @param value the user name string to store; expected to be already validated and trimmed if validation is required
 */
public UserName(String value) {this.value = value;}

    /**
     * Create a validated UserName instance from the provided string.
     *
     * @param value the candidate name (may contain surrounding whitespace)
     * @return the created UserName with leading and trailing whitespace removed
     * @throws IllegalArgumentException if {@code value} is null or empty
     * @throws IllegalArgumentException if the trimmed value does not match the allowed pattern (2–20 characters: letters, digits, or Korean syllables)
     */
    public static UserName of(String value) {
        if(value == null || value.isEmpty()) {
            throw new IllegalArgumentException("이름은 필수 값입니다.");
        }
        String trimmed = value.trim();
        if(!PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("이름은 2~20자의 한글 또는 영문만 가능합니다.");
        }
        return new UserName(trimmed);
    }
}