package com.loopers.domain.model;

import java.time.LocalDate;

public class Birthday {

    private final LocalDate value;

    /**
 * Create a Birthday with the specified date.
 *
 * @param value the validated birth date to store; must be non-null, not be in the future,
 *              and not be before 1900-01-01
 */
private Birthday(LocalDate value) {this.value = value; }

    /**
     * The stored birthday date.
     *
     * @return the stored {@link java.time.LocalDate} representing the birthday
     */
    public LocalDate getValue() {
        return value;
    }

    /**
     * Create a Birthday value object from the given date after validating it.
     *
     * @param value the birth date to validate and encapsulate
     * @return the Birthday representing the provided date
     * @throws IllegalArgumentException if {@code value} is {@code null}, is after today, or is before 1900-01-01
     */
    public static Birthday of(LocalDate value) {
        if(value == null) {
            throw new IllegalArgumentException("생년월일은 필수 입력값입니다.");
        }
        if(value.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("생년월일은 미래 날짜일 수 없습니다.");
        }
        if(value.isBefore(LocalDate.of(1900, 1,1))) {
            throw new IllegalArgumentException("생년월일은 1900년 이후여야 합니다.");
        }
        return new Birthday(value);
    }
}