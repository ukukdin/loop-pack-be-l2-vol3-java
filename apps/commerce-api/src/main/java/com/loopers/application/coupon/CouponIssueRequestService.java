package com.loopers.application.coupon;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.domain.model.coupon.Coupon;
import com.loopers.domain.model.user.UserId;
import com.loopers.domain.repository.CouponRepository;
import com.loopers.infrastructure.couponissue.CouponIssueRequestJpaEntity;
import com.loopers.infrastructure.couponissue.CouponIssueRequestJpaRepository;
import com.loopers.confg.kafka.EventTypes;
import com.loopers.confg.kafka.KafkaTopics;
import com.loopers.infrastructure.outbox.OutboxJpaEntity;
import com.loopers.infrastructure.outbox.OutboxJpaRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class CouponIssueRequestService implements RequestCouponIssueUseCase {

    private final CouponRepository couponRepository;
    private final CouponIssueRequestJpaRepository issueRequestRepository;
    private final OutboxJpaRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public CouponIssueRequestService(CouponRepository couponRepository,
                                     CouponIssueRequestJpaRepository issueRequestRepository,
                                     OutboxJpaRepository outboxRepository,
                                     ObjectMapper objectMapper) {
        this.couponRepository = couponRepository;
        this.issueRequestRepository = issueRequestRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public Long requestIssue(UserId userId, Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new CoreException(ErrorType.COUPON_NOT_FOUND));

        if (!coupon.isAvailable()) {
            throw new CoreException(ErrorType.COUPON_NOT_AVAILABLE);
        }

        CouponIssueRequestJpaEntity request = issueRequestRepository.save(
                new CouponIssueRequestJpaEntity(couponId, userId.getValue()));

        Integer maxIssuance = coupon.getIssuancePolicy().getMaxIssuanceValue();

        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "requestId", request.getId(),
                    "couponId", couponId,
                    "userId", userId.getValue(),
                    "maxIssuance", maxIssuance != null ? maxIssuance : -1,
                    "requestedAt", LocalDateTime.now().toString()
            ));
            outboxRepository.save(new OutboxJpaEntity(
                    "COUPON", String.valueOf(couponId), EventTypes.COUPON_ISSUE_REQUESTED,
                    KafkaTopics.COUPON_ISSUE_REQUESTS, String.valueOf(couponId), payload));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("쿠폰 발급 요청 직렬화 실패", e);
        }

        return request.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public String getIssueStatus(UserId userId, Long couponId) {
        return issueRequestRepository
                .findTopByCouponIdAndUserIdOrderByCreatedAtDesc(couponId, userId.getValue())
                .map(CouponIssueRequestJpaEntity::getStatus)
                .orElse("NOT_FOUND");
    }
}
