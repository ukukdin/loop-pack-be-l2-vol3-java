package com.loopers.application.coupon;

import com.loopers.domain.model.common.PageResult;
import com.loopers.domain.model.coupon.*;
import com.loopers.domain.model.userCoupon.UserCoupon;
import com.loopers.domain.repository.CouponRepository;
import com.loopers.domain.repository.UserCouponRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.UUID;

@Service
@Transactional
public class CouponAdminService implements CouponAdminUseCase {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;

    public CouponAdminService(CouponRepository couponRepository,
                              UserCouponRepository userCouponRepository) {
        this.couponRepository = couponRepository;
        this.userCouponRepository = userCouponRepository;
    }

    @Override
    public void createCoupon(CouponCreateCommand command) {
        DiscountType discountType = DiscountType.valueOf(command.type());

        BigDecimal discountValue;
        if (discountType == DiscountType.PERCENTAGE) {
            discountValue = BigDecimal.valueOf(command.value().doubleValue() / 100.0);
        } else {
            discountValue = command.value();
        }

        DiscountPolicy discountPolicy = DiscountPolicy.create(
                discountType, discountValue, null,
                command.minOrderAmount()
        );

        IssuancePolicy issuancePolicy = IssuancePolicy.create(null, null, null, null);

        CouponTarget applicationTarget = CouponTarget.create(
                TargetType.ALL, Collections.emptyList(),
                command.minOrderAmount() != null ? command.minOrderAmount() : BigDecimal.ZERO
        );

        String code = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Coupon coupon = Coupon.create(
                code, command.name(), command.description(),
                discountPolicy, issuancePolicy, command.expiredAt(),
                applicationTarget, false
        );

        couponRepository.save(coupon);
    }

    @Override
    public void updateCoupon(Long couponId, CouponUpdateCommand command) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new CoreException(ErrorType.COUPON_NOT_FOUND));

        DiscountType discountType = DiscountType.valueOf(command.type());
        BigDecimal discountValue;
        if (discountType == DiscountType.PERCENTAGE) {
            discountValue = BigDecimal.valueOf(command.value().doubleValue() / 100.0);
        } else {
            discountValue = command.value();
        }

        DiscountPolicy discountPolicy = DiscountPolicy.create(
                discountType, discountValue, null,
                command.minOrderAmount()
        );

        Coupon updated = coupon.update(command.name(), command.description(),
                discountPolicy, command.expiredAt());

        couponRepository.save(updated);
    }

    @Override
    public void deleteCoupon(Long couponId) {
        couponRepository.findById(couponId)
                .orElseThrow(() -> new CoreException(ErrorType.COUPON_NOT_FOUND));
        couponRepository.deleteById(couponId);
    }

    @Override
    @Transactional(readOnly = true)
    public CouponDetail getCoupon(Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new CoreException(ErrorType.COUPON_NOT_FOUND));

        return toCouponDetail(coupon);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<CouponSummary> getCoupons(int page, int size) {
        PageResult<Coupon> coupons = couponRepository.findAll(page, size);
        return coupons.map(this::toCouponSummary);
    }

    @Override
    @Transactional(readOnly = true)
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
