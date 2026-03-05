//package com.loopers.domain.model.coupon;
//
//import java.math.BigDecimal;
//import java.time.LocalDateTime;
//import java.util.List;
//
//public class CouponTest {
//    // given
//    String code = "NIKE2025";
//    String name = "나이키 10% 할인 쿠폰";
//    String description = "나이키 전 상품 10% 할인";
//
//    DiscountPolicy discountPolicy = DiscountPolicy.create(
//            DiscountType.PERCENTAGE,
//            BigDecimal.valueOf(0.1),          // 10%
//            BigDecimal.valueOf(5000),          // 최대 5000원
//            BigDecimal.valueOf(30000)          // 최소 주문 30000원
//    );
//
////    IssuancePolicy issuancePolicy = IssuancePolicy.create(
////            100,   // 최대 발급 100장
////            1      // 1인당 1장
////    );
////
////    ApplicationTarget applicationTarget = ApplicationTarget.ofSpecific(
////            TargetDomainType.BRAND,
////            List.of(1L, 2L, 3L)              // 나이키, 아디다스, 뉴발란스
////    );
//
//    LocalDateTime expiredAt = LocalDateTime.of(2025, 12, 31, 23, 59, 59);
//    boolean isDuplicate = false;
//
//    // when
//    Coupon coupon = Coupon.create(
//            code,
//            name,
//            description,
//            discountPolicy,
//            "",
//            expiredAt,
//            "",
//            isDuplicate
//    );
//
//    // then
//    assertThat(coupon.getCode()).isEqualTo("NIKE2025");
//    assertThat(coupon.getName()).isEqualTo("나이키 10% 할인 쿠폰");
//    assertThat(coupon.getStatus()).isEqualTo(CouponStatus.AVAILABLE);
//    assertThat(coupon.getExpiredAt()).isEqualTo(expiredAt);
//    assertThat(coupon.isDuplicate()).isFalse();
//
//}
