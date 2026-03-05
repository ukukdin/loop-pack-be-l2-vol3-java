package com.loopers.domain.model.coupon;

import com.loopers.support.error.CouponException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

class IssuancePolicyTest {

    @Test
    void 발급_정책_생성_성공() {
        // given
        LocalDateTime start = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 12, 31, 23, 59);

        // when
        IssuancePolicy policy = IssuancePolicy.create(100, 1, start, end);

        // then
        assertThat(policy.getMaxIssuanceValue()).isEqualTo(100);
        assertThat(policy.getIssuedCount()).isEqualTo(0);
        assertThat(policy.getMaxIssuancePerUser()).isEqualTo(1);
    }

    @Test
    void 무제한_발급_정책_생성_성공() {
        // given & when
        IssuancePolicy policy = IssuancePolicy.create(null, 1, null, null);

        // then
        assertThat(policy.getMaxIssuanceValue()).isNull();
        assertThat(policy.isIssuable()).isTrue();
    }

    @Test
    void 최대_발급수량이_0이하이면_생성_실패() {
        assertThatThrownBy(() -> IssuancePolicy.create(0, 1, null, null))
                .isInstanceOf(CouponException.class);
    }

    @Test
    void 인당_발급수량이_총_발급수량_초과시_생성_실패() {
        assertThatThrownBy(() -> IssuancePolicy.create(10, 20, null, null))
                .isInstanceOf(CouponException.class);
    }

    @Test
    void 발급_가능_여부_확인() {
        // given
        IssuancePolicy issuable = IssuancePolicy.reconstitute(100, 99, 1, null, null);
        IssuancePolicy notIssuable = IssuancePolicy.reconstitute(100, 100, 1, null, null);

        // then
        assertThat(issuable.isIssuable()).isTrue();
        assertThat(notIssuable.isIssuable()).isFalse();
    }
}
