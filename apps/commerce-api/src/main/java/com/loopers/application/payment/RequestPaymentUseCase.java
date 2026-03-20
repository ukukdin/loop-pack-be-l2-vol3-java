package com.loopers.application.payment;

import com.loopers.domain.model.user.UserId;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

public interface RequestPaymentUseCase {

    PaymentResult requestPayment(UserId userId, PaymentCommand command);

    record PaymentCommand(
            Long orderId,
            String cardType,
            String cardNo,
            Long amount
    ) {
        public PaymentCommand {
            if (orderId == null) {
                throw new CoreException(ErrorType.VALIDATION_ERROR, "주문 ID는 필수입니다.");
            }
            if (cardType == null || cardType.isBlank()) {
                throw new CoreException(ErrorType.VALIDATION_ERROR, "카드 종류는 필수입니다.");
            }
            if (cardNo == null || cardNo.isBlank()) {
                throw new CoreException(ErrorType.VALIDATION_ERROR, "카드 번호는 필수입니다.");
            }
            if (amount == null || amount <= 0) {
                throw new CoreException(ErrorType.VALIDATION_ERROR, "결제 금액은 0보다 커야 합니다.");
            }
        }
    }

    record PaymentResult(
            String transactionKey,
            String status,
            String reason
    ) {}
}
