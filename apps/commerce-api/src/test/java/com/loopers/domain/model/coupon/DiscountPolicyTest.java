package com.loopers.domain.model.coupon;


import com.loopers.support.error.CoreException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

class DiscountPolicyTest {

    @Test
    void 정액_할인_계산_성공(){
        //given
        DiscountPolicy discountPolicy = DiscountPolicy.create(
                DiscountType.FIXED,
                BigDecimal.valueOf(1000),
                null,
                BigDecimal.valueOf(100000)
        );

        //when
        BigDecimal result = discountPolicy.calculate(BigDecimal.valueOf(300000));

        //then
        assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(1000));
    }
    // 정률 할인
    @Test
    void 정률_할인_계산_성공() {
        // given
        DiscountPolicy policy = DiscountPolicy.create(
                DiscountType.PERCENTAGE,
                BigDecimal.valueOf(0.1),   // 10%
                null,                      // cap 없음
                BigDecimal.valueOf(10000)
        );

        // when
        BigDecimal result = policy.calculate(BigDecimal.valueOf(30000));

        // then
        assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(3000)); // 30000 * 10%
    }
    @Test
    void 정률_할인이_100퍼센트_초과시_생성_실패() {
        assertThatThrownBy(() -> DiscountPolicy.create(
                DiscountType.PERCENTAGE,
                BigDecimal.valueOf(1.1),  // 110%
                null,
                null
        )).isInstanceOf(CoreException.class);
    }

    @Test
    void createFromInput으로_퍼센트_변환_성공() {
        // given
        DiscountPolicy policy = DiscountPolicy.createFromInput(
                DiscountType.PERCENTAGE,
                BigDecimal.valueOf(10),   // 사용자 입력: 10%
                null,
                null
        );

        // then
        assertThat(policy.getDiscountValue()).isEqualByComparingTo(BigDecimal.valueOf(0.1));
    }

    @Test
    void createFromInput으로_정액은_변환없이_통과() {
        // given
        DiscountPolicy policy = DiscountPolicy.createFromInput(
                DiscountType.FIXED,
                BigDecimal.valueOf(1000),
                null,
                null
        );

        // then
        assertThat(policy.getDiscountValue()).isEqualByComparingTo(BigDecimal.valueOf(1000));
    }

}
