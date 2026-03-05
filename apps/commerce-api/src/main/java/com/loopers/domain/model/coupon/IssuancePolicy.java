package com.loopers.domain.model.coupon;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class IssuancePolicy {
    private final Integer maxIssuanceValue;
    private final Integer issuedCount;
    private final Integer maxIssuancePerUser;
    private final LocalDateTime issueStartAt;
    private final LocalDateTime issueEndAt;

    private IssuancePolicy(Integer maxIssuanceValue, Integer issuedCount,
                           Integer maxIssuancePerUser,
                           LocalDateTime issueStartAt, LocalDateTime issueEndAt) {
        this.maxIssuanceValue = maxIssuanceValue;
        this.issuedCount = issuedCount;
        this.maxIssuancePerUser = maxIssuancePerUser;
        this.issueStartAt = issueStartAt;
        this.issueEndAt = issueEndAt;
    }

    public static IssuancePolicy create(Integer maxIssuanceValue, Integer maxIssuancePerUser,
                                        LocalDateTime issueStartAt, LocalDateTime issueEndAt) {
        validate(maxIssuanceValue, maxIssuancePerUser);
        return new IssuancePolicy(maxIssuanceValue, 0, maxIssuancePerUser, issueStartAt, issueEndAt);
    }

    public static IssuancePolicy reconstitute(Integer maxIssuanceValue, Integer issuedCount,
                                              Integer maxIssuancePerUser,
                                              LocalDateTime issueStartAt, LocalDateTime issueEndAt) {
        return new IssuancePolicy(maxIssuanceValue, issuedCount, maxIssuancePerUser, issueStartAt, issueEndAt);
    }

    public boolean isIssuable() {
        return maxIssuanceValue == null || issuedCount < maxIssuanceValue;
    }

    public IssuancePolicy incrementIssuedCount() {
        if (!isIssuable()) {
            throw new CoreException(ErrorType.COUPON_EXCEEDED);
        }
        return new IssuancePolicy(this.maxIssuanceValue, this.issuedCount + 1,
                this.maxIssuancePerUser, this.issueStartAt, this.issueEndAt);
    }

    private static void validate(Integer maxIssuanceValue, Integer maxIssuancePerUser) {
        if (maxIssuanceValue != null && maxIssuanceValue <= 0) {
            throw new CoreException(ErrorType.COUPON_INVALID_ISSUANCE_COUNT);
        }
        if (maxIssuancePerUser != null && maxIssuanceValue != null
                && maxIssuancePerUser > maxIssuanceValue) {
            throw new CoreException(ErrorType.COUPON_INVALID_ISSUANCE_PER_USER);
        }
    }
}
