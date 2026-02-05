package com.loopers.domain.model;

public class WrongPasswordCount {

    private final int value;

    /**
     * Creates a WrongPasswordCount with the specified count.
     *
     * <p>This private constructor initializes the immutable underlying value. Callers must ensure
     * the supplied value is greater than or equal to 0; validation is performed by the public
     * factory methods.
     *
     * @param value the number of wrong password attempts; must be >= 0
     */
    private WrongPasswordCount(int value) {
        this.value = value;
    }

    /**
     * Creates a WrongPasswordCount representing zero wrong password attempts.
     *
     * @return a WrongPasswordCount with value 0
     */
    public static WrongPasswordCount init() {
        return new WrongPasswordCount(0);
    }

    /**
     * Creates a WrongPasswordCount representing the given number of wrong password attempts.
     *
     * @param value the number of wrong password attempts; must be greater than or equal to 0
     * @return a WrongPasswordCount with the specified value
     * @throws IllegalArgumentException if {@code value} is less than 0
     */
    public static WrongPasswordCount of(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("비밀번호 오류 횟수는 음수일 수 없습니다.");
        }
        return new WrongPasswordCount(value);
    }

    /**
     * Gets the current wrong-password count.
     *
     * @return the current wrong-password count
     */
    public int getValue() {
        return value;
    }

    /**
     * Create a new WrongPasswordCount with its count increased by one.
     *
     * @return a WrongPasswordCount whose value is this instance's value plus one.
     */
    public WrongPasswordCount increment() {
        return new WrongPasswordCount(this.value + 1);
    }

    /**
     * Reset the wrong-password count to zero and return a new immutable instance.
     *
     * @return a new {@code WrongPasswordCount} whose value is 0
     */
    public WrongPasswordCount reset() {
        return new WrongPasswordCount(0);
    }

    /**
     * Indicates whether the account is locked due to too many wrong password attempts.
     *
     * @return true if the underlying count is greater than or equal to 5, false otherwise.
     */
    public boolean isLocked() {
        return this.value >= 5;
    }
}