package com.loopers.domain.model.coupon;

import com.loopers.support.error.CoreException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

class CouponTargetTest {

    @Test
    void 전체_대상_쿠폰_생성_성공() {
        // given & when
        CouponTarget target = CouponTarget.create(TargetType.ALL, null);

        // then
        assertThat(target.getTargetType()).isEqualTo(TargetType.ALL);
    }

    @Test
    void 특정_대상_쿠폰_생성_성공() {
        // given & when
        CouponTarget target = CouponTarget.create(TargetType.SPECIFIC, List.of(1L, 2L, 3L));

        // then
        assertThat(target.getTargetType()).isEqualTo(TargetType.SPECIFIC);
        assertThat(target.getTargetIds()).isEqualTo(List.of(1L, 2L, 3L));
    }

    @Test
    void 특정_대상_쿠폰에_대상ID_없으면_생성_실패() {
        assertThatThrownBy(() -> CouponTarget.create(TargetType.SPECIFIC, null))
                .isInstanceOf(CoreException.class);
    }

    @Test
    void 특정_대상_쿠폰에_대상ID_빈리스트면_생성_실패() {
        assertThatThrownBy(() -> CouponTarget.create(TargetType.SPECIFIC, List.of()))
                .isInstanceOf(CoreException.class);
    }

    @Test
    void 전체_대상_쿠폰_적용_가능() {
        // given
        CouponTarget target = CouponTarget.create(TargetType.ALL, null);

        // then
        assertThat(target.isApplicable(1L)).isTrue();
        assertThat(target.isApplicable(999L)).isTrue();
    }

    @Test
    void 특정_대상_쿠폰_대상ID_포함시_적용_가능() {
        // given
        CouponTarget target = CouponTarget.create(TargetType.SPECIFIC, List.of(1L, 2L));

        // then
        assertThat(target.isApplicable(1L)).isTrue();
        assertThat(target.isApplicable(3L)).isFalse();
    }
}
