package com.loopers.infrastructure.coupon;

import com.loopers.domain.model.common.PageResult;
import com.loopers.domain.model.coupon.*;
import com.loopers.domain.repository.CouponRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Repository
public class CouponRepositoryImpl implements CouponRepository {

    private final CouponJpaRepository couponJpaRepository;

    public CouponRepositoryImpl(CouponJpaRepository couponJpaRepository) {
        this.couponJpaRepository = couponJpaRepository;
    }

    @Override
    public Coupon save(Coupon coupon) {
        CouponJpaEntity entity = toEntity(coupon);
        CouponJpaEntity saved = couponJpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Coupon> findById(Long id) {
        return couponJpaRepository.findById(id)
                .map(this::toDomain);
    }

    @Override
    public Optional<Coupon> findByIdWithLock(Long id) {
        return couponJpaRepository.findByIdForUpdate(id)
                .map(this::toDomain);
    }

    @Override
    public List<Coupon> findAllByIds(List<Long> ids) {
        return couponJpaRepository.findAllById(ids).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public PageResult<Coupon> findAll(int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<CouponJpaEntity> jpaPage = couponJpaRepository.findAll(pageRequest);

        List<Coupon> content = jpaPage.getContent().stream().map(this::toDomain).toList();
        return new PageResult<>(content, jpaPage.getNumber(), jpaPage.getSize(),
                jpaPage.getTotalElements(), jpaPage.getTotalPages());
    }

    @Override
    public void deleteById(Long id) {
        couponJpaRepository.deleteById(id);
    }

    private CouponJpaEntity toEntity(Coupon coupon) {
        DiscountPolicy dp = coupon.getDiscountPolicy();
        IssuancePolicy ip = coupon.getIssuancePolicy();
        CouponTarget ct = coupon.getApplicationTarget();

        String targetIdsStr = ct.getTargetIds() != null && !ct.getTargetIds().isEmpty()
                ? ct.getTargetIds().stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse(null)
                : null;

        return new CouponJpaEntity(
                coupon.getId(),
                coupon.getCode(),
                coupon.getName(),
                coupon.getDescription(),
                dp.getDiscountType().name(),
                dp.getDiscountValue(),
                dp.getMaxDiscountValue(),
                dp.getMinOrderAmount(),
                ip.getMaxIssuanceValue(),
                ip.getIssuedCount(),
                ip.getMaxIssuancePerUser(),
                ip.getIssueStartAt(),
                ip.getIssueEndAt(),
                ct.getTargetType().name(),
                targetIdsStr,
                ct.getMinOrderAmount(),
                coupon.isDuplicate(),
                coupon.getStatus().name(),
                coupon.getExpiredAt(),
                coupon.getCreatedAt(),
                coupon.getUpdatedAt()
        );
    }

    private Coupon toDomain(CouponJpaEntity entity) {
        DiscountPolicy discountPolicy = DiscountPolicy.create(
                DiscountType.valueOf(entity.getDiscountType()),
                entity.getDiscountValue(),
                entity.getMaxDiscountValue(),
                entity.getMinOrderAmount()
        );

        IssuancePolicy issuancePolicy = IssuancePolicy.reconstitute(
                entity.getMaxIssuanceValue(),
                entity.getIssuedCount(),
                entity.getMaxIssuancePerUser(),
                entity.getIssueStartAt(),
                entity.getIssueEndAt()
        );

        List<Long> targetIds = entity.getTargetIds() != null && !entity.getTargetIds().isBlank()
                ? Arrays.stream(entity.getTargetIds().split(",")).map(Long::valueOf).toList()
                : Collections.emptyList();

        CouponTarget applicationTarget = CouponTarget.reconstitute(
                TargetType.valueOf(entity.getTargetType()),
                targetIds,
                entity.getTargetMinOrderAmount() != null ? entity.getTargetMinOrderAmount() : BigDecimal.ZERO
        );

        return Coupon.reconstitute(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getDescription(),
                discountPolicy,
                issuancePolicy,
                entity.getExpiredAt(),
                applicationTarget,
                entity.isDuplicate(),
                CouponStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
