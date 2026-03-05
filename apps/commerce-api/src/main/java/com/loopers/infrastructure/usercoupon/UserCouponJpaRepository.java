package com.loopers.infrastructure.usercoupon;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserCouponJpaRepository extends JpaRepository<UserCouponJpaEntity, Long> {

    List<UserCouponJpaEntity> findAllByUserId(String userId);

    Page<UserCouponJpaEntity> findAllByCouponId(Long couponId, Pageable pageable);

    int countByUserIdAndCouponId(String userId, Long couponId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT uc FROM UserCouponJpaEntity uc WHERE uc.id = :id")
    Optional<UserCouponJpaEntity> findByIdForUpdate(@Param("id") Long id);
}
