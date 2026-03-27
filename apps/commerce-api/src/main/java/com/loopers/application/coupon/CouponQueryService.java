package com.loopers.application.coupon;

import com.loopers.domain.model.coupon.Coupon;
import com.loopers.domain.model.user.UserId;
import com.loopers.domain.model.userCoupon.UserCoupon;
import com.loopers.domain.repository.CouponRepository;
import com.loopers.domain.repository.UserCouponRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CouponQueryService implements CouponQueryUseCase {

    private final UserCouponRepository userCouponRepository;
    private final CouponRepository couponRepository;

    public CouponQueryService(UserCouponRepository userCouponRepository,
                              CouponRepository couponRepository) {
        this.userCouponRepository = userCouponRepository;
        this.couponRepository = couponRepository;
    }

    @Transactional(readOnly = true)
    @Override
    public List<UserCouponInfo> getMyCoupons(UserId userId) {
        List<UserCoupon> userCoupons = userCouponRepository.findByUserId(userId);

        List<Long> couponIds = userCoupons.stream()
                .map(UserCoupon::getCouponId)
                .distinct()
                .toList();

        Map<Long, Coupon> couponMap = couponRepository.findAllByIds(couponIds).stream()
                .collect(Collectors.toMap(Coupon::getId, Function.identity()));

        return userCoupons.stream()
                .map(uc -> {
                    Coupon coupon = couponMap.get(uc.getCouponId());
                    return new UserCouponInfo(
                            uc.getId(),
                            uc.getCouponId(),
                            coupon != null ? coupon.getName() : "삭제된 쿠폰",
                            coupon != null ? coupon.getDiscountPolicy().getDiscountType().name() : null,
                            coupon != null ? coupon.getDiscountPolicy().getDiscountValue() : null,
                            uc.getStatus().name(),
                            uc.getIssuedAt(),
                            uc.getUsedAt(),
                            coupon != null ? coupon.getExpiredAt() : null
                    );
                })
                .toList();
    }
}
