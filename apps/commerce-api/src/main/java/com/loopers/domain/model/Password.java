package com.loopers.domain.model;

import lombok.Data;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Pattern;

@Data
public class Password {
    private static final Pattern ALLOWED_CHARS = Pattern.compile("^[a-zA-Z0-9!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?`~]{8,16}$");

    private final String value;

    /**
 * Creates a Password instance wrapping the validated password string.
 *
 * @param value the validated password value to store
 */
private Password(String value) {this.value = value;}

    /**
     * Creates a Password value object from the provided raw password after validating it.
     *
     * @param rawPassword the plaintext password to validate and encapsulate
     * @param birthday the user's birthday to forbid within the password; may be null
     * @return a Password instance containing the validated password
     * @throws IllegalArgumentException if rawPassword is null or blank, does not match the allowed character/length rules, or contains the provided birthday
     */
    public static Password of(String rawPassword, LocalDate birthday) {
        validate(rawPassword, birthday);
        return new Password(rawPassword);
    }
    /**
     * Validate the provided password according to required format and birthday exclusion rules.
     *
     * @param rawPassword the password to validate; must not be null or blank and must match the allowed character pattern (8–16 letters, digits, and permitted special characters)
     * @param birthday    the user's birthday used to check for forbidden substrings in the password; may be null to skip this check
     * @throws IllegalArgumentException if {@code rawPassword} is null or blank, does not match the allowed pattern, or contains a formatted representation of {@code birthday}
     */
    private static void validate(String rawPassword, LocalDate birthday) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("비밀번호는 필수 입력값입니다.");
        }

        if (!ALLOWED_CHARS.matcher(rawPassword).matches()) {
            throw new IllegalArgumentException("비밀번호는 8~16자리 영문 대소문자, 숫자, 특수문자만 가능합니다.");
        }
        if (birthday != null && containsBirthday(rawPassword, birthday)) {
            throw new IllegalArgumentException("생년월일은 비밀번호 내에 포함될 수 없습니다.");
        }
    }

    /**
     * Checks whether the password contains the birthday in any common numeric or dashed format.
     *
     * @param rawPassword the password string to inspect
     * @param birthday the birth date to look for within the password
     * @return `true` if `rawPassword` contains the birthday formatted as `yyyyMMdd`, `yyMMdd`, `MMdd`, `yyyy-MM-dd`, or `yy-MM-dd`, `false` otherwise
     */
    static boolean containsBirthday(String rawPassword, LocalDate birthday) {
        //yyyyMMdd, yyMMdd, MMdd 같은 포멧은 다 제외 하는걸로
        List<String> patterns = List.of(
                birthday.format(DateTimeFormatter.ofPattern("yyyyMMdd")),
                birthday.format(DateTimeFormatter.ofPattern("yyMMdd")),
                birthday.format(DateTimeFormatter.ofPattern("MMdd")),
                birthday.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                birthday.format(DateTimeFormatter.ofPattern("yy-MM-dd"))
        );

        return patterns.stream().anyMatch(rawPassword::contains);

    }
}