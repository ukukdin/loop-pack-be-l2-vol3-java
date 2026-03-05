package com.loopers.domain.repository;

import com.loopers.domain.model.common.PageResult;
import com.loopers.domain.model.coupon.Coupon;

import java.util.List;
import java.util.Optional;

public interface CouponRepository {

    Coupon save(Coupon coupon);

    Optional<Coupon> findById(Long id);

    Optional<Coupon> findByIdWithLock(Long id);

    List<Coupon> findAllByIds(List<Long> ids);

    PageResult<Coupon> findAll(int page, int size);

    void deleteById(Long id);
}
