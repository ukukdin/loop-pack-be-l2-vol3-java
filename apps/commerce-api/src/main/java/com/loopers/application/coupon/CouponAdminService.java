package com.loopers.application.coupon;

import com.loopers.domain.model.coupon.*;
import com.loopers.domain.repository.CouponRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.UUID;

@Service
public class CouponAdminService implements CouponAdminUseCase {

    private final CouponRepository couponRepository;

    public CouponAdminService(CouponRepository couponRepository) {
        this.couponRepository = couponRepository;
    }

    @Transactional
    @Override
    public void createCoupon(CouponCreateCommand command) {
        DiscountType discountType = DiscountType.valueOf(command.type());

        DiscountPolicy discountPolicy = DiscountPolicy.createFromInput(
                discountType, command.value(), null,
                command.minOrderAmount()
        );

        IssuancePolicy issuancePolicy = IssuancePolicy.create(null, null, null, null);

        CouponTarget applicationTarget = CouponTarget.create(
                TargetType.ALL, Collections.emptyList()
        );

        String code = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Coupon coupon = Coupon.create(
                code, command.name(), command.description(),
                discountPolicy, issuancePolicy, command.expiredAt(),
                applicationTarget, false
        );

        couponRepository.save(coupon);
    }

    @Transactional
    @Override
    public void updateCoupon(Long couponId, CouponUpdateCommand command) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new CoreException(ErrorType.COUPON_NOT_FOUND));

        DiscountType discountType = DiscountType.valueOf(command.type());

        DiscountPolicy discountPolicy = DiscountPolicy.createFromInput(
                discountType, command.value(), null,
                command.minOrderAmount()
        );

        Coupon updated = coupon.update(command.name(), command.description(),
                discountPolicy, command.expiredAt());

        couponRepository.save(updated);
    }

    @Transactional
    @Override
    public void deleteCoupon(Long couponId) {
        couponRepository.findById(couponId)
                .orElseThrow(() -> new CoreException(ErrorType.COUPON_NOT_FOUND));
        couponRepository.deleteById(couponId);
    }
}
