package com.loopers.domain.model.coupon;

import com.loopers.support.error.CouponException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

class CouponTargetTest {

    @Test
    void 전체_대상_쿠폰_생성_성공() {
        // given & when
        CouponTarget target = CouponTarget.create(
                TargetType.ALL, null, BigDecimal.valueOf(10000));

        // then
        assertThat(target.getTargetType()).isEqualTo(TargetType.ALL);
    }

    @Test
    void 특정_대상_쿠폰_생성_성공() {
        // given & when
        CouponTarget target = CouponTarget.create(
                TargetType.SPECIFIC, List.of(1L, 2L, 3L), BigDecimal.valueOf(10000));

        // then
        assertThat(target.getTargetType()).isEqualTo(TargetType.SPECIFIC);
        assertThat(target.getTargetIds()).isEqualTo(List.of(1L, 2L, 3L));
    }

    @Test
    void 특정_대상_쿠폰에_대상ID_없으면_생성_실패() {
        assertThatThrownBy(() -> CouponTarget.create(
                TargetType.SPECIFIC, null, BigDecimal.valueOf(10000)))
                .isInstanceOf(CouponException.class);
    }

    @Test
    void 특정_대상_쿠폰에_대상ID_빈리스트면_생성_실패() {
        assertThatThrownBy(() -> CouponTarget.create(
                TargetType.SPECIFIC, List.of(), BigDecimal.valueOf(10000)))
                .isInstanceOf(CouponException.class);
    }

    @Test
    void 전체_대상_쿠폰_최소주문금액_이상이면_적용_가능() {
        // given
        CouponTarget target = CouponTarget.create(
                TargetType.ALL, null, BigDecimal.valueOf(10000));

        // then
        assertThat(target.isApplicable(1L, BigDecimal.valueOf(15000))).isTrue();
        assertThat(target.isApplicable(1L, BigDecimal.valueOf(5000))).isFalse();
    }

    @Test
    void 특정_대상_쿠폰_대상ID_포함시_적용_가능() {
        // given
        CouponTarget target = CouponTarget.create(
                TargetType.SPECIFIC, List.of(1L, 2L), BigDecimal.valueOf(10000));

        // then
        assertThat(target.isApplicable(1L, BigDecimal.valueOf(15000))).isTrue();
        assertThat(target.isApplicable(3L, BigDecimal.valueOf(15000))).isFalse();
    }
}
