package com.loopers.infrastructure.usercoupon;

import com.loopers.domain.model.common.PageResult;
import com.loopers.domain.model.user.UserId;
import com.loopers.domain.model.userCoupon.UserCoupon;
import com.loopers.domain.model.userCoupon.UserCouponStatus;
import com.loopers.domain.repository.UserCouponRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class UserCouponRepositoryImpl implements UserCouponRepository {

    private final UserCouponJpaRepository userCouponJpaRepository;

    public UserCouponRepositoryImpl(UserCouponJpaRepository userCouponJpaRepository) {
        this.userCouponJpaRepository = userCouponJpaRepository;
    }

    @Override
    public UserCoupon save(UserCoupon userCoupon) {
        UserCouponJpaEntity entity = toEntity(userCoupon);
        UserCouponJpaEntity saved = userCouponJpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<UserCoupon> findById(Long id) {
        return userCouponJpaRepository.findById(id)
                .map(this::toDomain);
    }

    @Override
    public Optional<UserCoupon> findByIdWithLock(Long id) {
        return userCouponJpaRepository.findByIdForUpdate(id)
                .map(this::toDomain);
    }

    @Override
    public List<UserCoupon> findByUserId(UserId userId) {
        return userCouponJpaRepository.findAllByUserId(userId.getValue()).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public PageResult<UserCoupon> findByCouponId(Long couponId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "issuedAt"));
        Page<UserCouponJpaEntity> jpaPage = userCouponJpaRepository.findAllByCouponId(couponId, pageRequest);

        List<UserCoupon> content = jpaPage.getContent().stream().map(this::toDomain).toList();
        return new PageResult<>(content, jpaPage.getNumber(), jpaPage.getSize(),
                jpaPage.getTotalElements(), jpaPage.getTotalPages());
    }

    @Override
    public int countByUserIdAndCouponId(UserId userId, Long couponId) {
        return userCouponJpaRepository.countByUserIdAndCouponId(userId.getValue(), couponId);
    }

    private UserCouponJpaEntity toEntity(UserCoupon userCoupon) {
        return new UserCouponJpaEntity(
                userCoupon.getId(),
                userCoupon.getCouponId(),
                userCoupon.getUserId().getValue(),
                userCoupon.getStatus().name(),
                userCoupon.getIssuedAt(),
                userCoupon.getUsedAt()
        );
    }

    private UserCoupon toDomain(UserCouponJpaEntity entity) {
        return UserCoupon.reconstitute(
                entity.getId(),
                entity.getCouponId(),
                UserId.of(entity.getUserId()),
                UserCouponStatus.valueOf(entity.getStatus()),
                entity.getIssuedAt(),
                entity.getUsedAt()
        );
    }
}
