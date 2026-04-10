package com.loopers.application.coupon;

import com.loopers.domain.model.coupon.Coupon;
import com.loopers.domain.model.user.UserId;
import com.loopers.domain.model.userCoupon.UserCoupon;
import com.loopers.domain.repository.CouponRepository;
import com.loopers.domain.repository.UserCouponRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CouponService implements IssueCouponUseCase {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;

    public CouponService(CouponRepository couponRepository,
                         UserCouponRepository userCouponRepository) {
        this.couponRepository = couponRepository;
        this.userCouponRepository = userCouponRepository;
    }

    @Transactional
    @Override
    public void issue(UserId userId, Long couponId) {
        Coupon coupon = couponRepository.findByIdWithLock(couponId)
                .orElseThrow(() -> new CoreException(ErrorType.COUPON_NOT_FOUND));

        if (!coupon.isAvailable()) {
            throw new CoreException(ErrorType.COUPON_NOT_AVAILABLE);
        }

        if (!coupon.getIssuancePolicy().isIssuable()) {
            throw new CoreException(ErrorType.COUPON_EXCEEDED);
        }

        if (coupon.getIssuancePolicy().getMaxIssuancePerUser() != null) {
            int userIssuedCount = userCouponRepository.countByUserIdAndCouponId(userId, couponId);
            if (userIssuedCount >= coupon.getIssuancePolicy().getMaxIssuancePerUser()) {
                throw new CoreException(ErrorType.COUPON_EXCEEDED, "1인당 발급 가능 수량을 초과했습니다.");
            }
        }

        Coupon issuedCoupon = coupon.issue();
        couponRepository.save(issuedCoupon);

        UserCoupon userCoupon = UserCoupon.issue(couponId, userId);
        userCouponRepository.save(userCoupon);
    }
}
