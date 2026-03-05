package com.loopers.application.coupon;

import com.loopers.domain.model.common.PageResult;
import com.loopers.domain.model.coupon.Coupon;
import com.loopers.domain.model.userCoupon.UserCoupon;
import com.loopers.domain.repository.CouponRepository;
import com.loopers.domain.repository.UserCouponRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CouponAdminQueryService implements CouponAdminQueryUseCase {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;

    public CouponAdminQueryService(CouponRepository couponRepository,
                                   UserCouponRepository userCouponRepository) {
        this.couponRepository = couponRepository;
        this.userCouponRepository = userCouponRepository;
    }

    @Override
    public CouponDetail getCoupon(Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new CoreException(ErrorType.COUPON_NOT_FOUND));
        return toCouponDetail(coupon);
    }

    @Override
    public PageResult<CouponSummary> getCoupons(int page, int size) {
        PageResult<Coupon> coupons = couponRepository.findAll(page, size);
        return coupons.map(this::toCouponSummary);
    }

    @Override
    public PageResult<IssuedCouponInfo> getIssuedCoupons(Long couponId, int page, int size) {
        couponRepository.findById(couponId)
                .orElseThrow(() -> new CoreException(ErrorType.COUPON_NOT_FOUND));

        PageResult<UserCoupon> userCoupons = userCouponRepository.findByCouponId(couponId, page, size);
        return userCoupons.map(uc -> new IssuedCouponInfo(
                uc.getId(),
                uc.getUserId().getValue(),
                uc.getStatus().name(),
                uc.getIssuedAt(),
                uc.getUsedAt()
        ));
    }

    private CouponDetail toCouponDetail(Coupon coupon) {
        return new CouponDetail(
                coupon.getId(),
                coupon.getCode(),
                coupon.getName(),
                coupon.getDescription(),
                coupon.getDiscountPolicy().getDiscountType().name(),
                coupon.getDiscountPolicy().getDiscountValue(),
                coupon.getDiscountPolicy().getMinOrderAmount(),
                coupon.getStatus().name(),
                coupon.getExpiredAt(),
                coupon.getCreatedAt(),
                coupon.getUpdatedAt()
        );
    }

    private CouponSummary toCouponSummary(Coupon coupon) {
        return new CouponSummary(
                coupon.getId(),
                coupon.getName(),
                coupon.getDiscountPolicy().getDiscountType().name(),
                coupon.getDiscountPolicy().getDiscountValue(),
                coupon.getStatus().name(),
                coupon.getExpiredAt(),
                coupon.getCreatedAt()
        );
    }
}
