package com.loopers.interfaces.api.coupon;

import com.loopers.application.coupon.CouponAdminUseCase;
import com.loopers.domain.model.common.PageResult;
import com.loopers.interfaces.api.common.PageResponse;
import com.loopers.interfaces.api.coupon.dto.CouponCreateRequest;
import com.loopers.interfaces.api.coupon.dto.CouponDetailResponse;
import com.loopers.interfaces.api.coupon.dto.CouponSummaryResponse;
import com.loopers.interfaces.api.coupon.dto.CouponUpdateRequest;
import com.loopers.interfaces.api.coupon.dto.IssuedCouponResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api-admin/v1/coupons")
public class CouponAdminController {

    private final CouponAdminUseCase couponAdminUseCase;

    public CouponAdminController(CouponAdminUseCase couponAdminUseCase) {
        this.couponAdminUseCase = couponAdminUseCase;
    }

    @GetMapping
    public ResponseEntity<PageResponse<CouponSummaryResponse>> getCoupons(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResult<CouponAdminUseCase.CouponSummary> result = couponAdminUseCase.getCoupons(page, size);
        return ResponseEntity.ok(PageResponse.from(result, CouponSummaryResponse::from));
    }

    @GetMapping("/{couponId}")
    public ResponseEntity<CouponDetailResponse> getCoupon(@PathVariable Long couponId) {
        CouponAdminUseCase.CouponDetail detail = couponAdminUseCase.getCoupon(couponId);
        return ResponseEntity.ok(CouponDetailResponse.from(detail));
    }

    @PostMapping
    public ResponseEntity<Void> createCoupon(@Valid @RequestBody CouponCreateRequest request) {
        couponAdminUseCase.createCoupon(request.toCommand());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{couponId}")
    public ResponseEntity<Void> updateCoupon(@PathVariable Long couponId,
                                             @Valid @RequestBody CouponUpdateRequest request) {
        couponAdminUseCase.updateCoupon(couponId, request.toCommand());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{couponId}")
    public ResponseEntity<Void> deleteCoupon(@PathVariable Long couponId) {
        couponAdminUseCase.deleteCoupon(couponId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{couponId}/issues")
    public ResponseEntity<PageResponse<IssuedCouponResponse>> getIssuedCoupons(
            @PathVariable Long couponId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResult<CouponAdminUseCase.IssuedCouponInfo> result =
                couponAdminUseCase.getIssuedCoupons(couponId, page, size);
        return ResponseEntity.ok(PageResponse.from(result, IssuedCouponResponse::from));
    }
}
