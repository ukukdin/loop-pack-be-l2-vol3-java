package com.loopers.interfaces.api.coupon;

import com.loopers.application.coupon.CouponQueryUseCase;
import com.loopers.application.coupon.RequestCouponIssueUseCase;
import com.loopers.domain.model.user.UserId;
import com.loopers.interfaces.api.coupon.dto.UserCouponResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class CouponController {

    private final RequestCouponIssueUseCase requestCouponIssueUseCase;
    private final CouponQueryUseCase couponQueryUseCase;

    public CouponController(RequestCouponIssueUseCase requestCouponIssueUseCase,
                            CouponQueryUseCase couponQueryUseCase) {
        this.requestCouponIssueUseCase = requestCouponIssueUseCase;
        this.couponQueryUseCase = couponQueryUseCase;
    }

    @PostMapping("/coupons/{couponId}/issue")
    public ResponseEntity<Map<String, Long>> issueCoupon(HttpServletRequest request,
                                                         @PathVariable Long couponId) {
        UserId userId = (UserId) request.getAttribute("authenticatedUserId");
        Long requestId = requestCouponIssueUseCase.requestIssue(userId, couponId);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(Map.of("requestId", requestId));
    }

    @GetMapping("/coupons/{couponId}/issue-status")
    public ResponseEntity<Map<String, String>> getIssueStatus(HttpServletRequest request,
                                                               @PathVariable Long couponId) {
        UserId userId = (UserId) request.getAttribute("authenticatedUserId");
        String status = requestCouponIssueUseCase.getIssueStatus(userId, couponId);
        return ResponseEntity.ok(Map.of("status", status));
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
