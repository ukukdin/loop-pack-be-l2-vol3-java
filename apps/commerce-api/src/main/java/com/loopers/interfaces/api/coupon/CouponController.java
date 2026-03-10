package com.loopers.interfaces.api.coupon;

import com.loopers.application.coupon.CouponQueryUseCase;
import com.loopers.application.coupon.IssueCouponUseCase;
import com.loopers.domain.model.user.UserId;
import com.loopers.interfaces.api.coupon.dto.UserCouponResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class CouponController {

    private final IssueCouponUseCase issueCouponUseCase;
    private final CouponQueryUseCase couponQueryUseCase;

    public CouponController(IssueCouponUseCase issueCouponUseCase,
                            CouponQueryUseCase couponQueryUseCase) {
        this.issueCouponUseCase = issueCouponUseCase;
        this.couponQueryUseCase = couponQueryUseCase;
    }

    @PostMapping("/coupons/{couponId}/issue")
    public ResponseEntity<Void> issueCoupon(HttpServletRequest request,
                                            @PathVariable Long couponId) {
        UserId userId = (UserId) request.getAttribute("authenticatedUserId");
        issueCouponUseCase.issue(userId, couponId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/users/me/coupons")
    public ResponseEntity<List<UserCouponResponse>> getMyCoupons(HttpServletRequest request) {
        UserId userId = (UserId) request.getAttribute("authenticatedUserId");
        List<UserCouponResponse> coupons = couponQueryUseCase.getMyCoupons(userId).stream()
                .map(UserCouponResponse::from)
                .toList();
        return ResponseEntity.ok(coupons);
    }
}
