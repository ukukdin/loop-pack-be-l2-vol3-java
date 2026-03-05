package com.loopers.domain.model.userCoupon;

import com.loopers.domain.model.user.UserId;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class UserCoupon {

    private final Long id;
    private final Long couponId;
    private final UserId userId;
    private final UserCouponStatus status;
    private final LocalDateTime issuedAt;
    private final LocalDateTime usedAt;

    private UserCoupon(Long id, Long couponId, UserId userId, UserCouponStatus status,
                       LocalDateTime issuedAt, LocalDateTime usedAt) {
        this.id = id;
        this.couponId = couponId;
        this.userId = userId;
        this.status = status;
        this.issuedAt = issuedAt;
        this.usedAt = usedAt;
    }

    public static UserCoupon issue(Long couponId, UserId userId) {
        return new UserCoupon(null, couponId, userId, UserCouponStatus.AVAILABLE,
                LocalDateTime.now(), null);
    }

    public static UserCoupon reconstitute(Long id, Long couponId, UserId userId,
                                          UserCouponStatus status, LocalDateTime issuedAt,
                                          LocalDateTime usedAt) {
        return new UserCoupon(id, couponId, userId, status, issuedAt, usedAt);
    }

    public UserCoupon use() {
        if (!isUsable()) {
            throw new IllegalStateException("사용할 수 없는 쿠폰입니다. 현재 상태: " + status);
        }
        return new UserCoupon(this.id, this.couponId, this.userId,
                UserCouponStatus.USED, this.issuedAt, LocalDateTime.now());
    }

    public UserCoupon restore() {
        if (status != UserCouponStatus.USED) {
            throw new IllegalStateException("사용된 쿠폰만 복원할 수 있습니다. 현재 상태: " + status);
        }
        return new UserCoupon(this.id, this.couponId, this.userId,
                UserCouponStatus.AVAILABLE, this.issuedAt, null);
    }

    public boolean isUsable() {
        return status == UserCouponStatus.AVAILABLE;
    }
}
