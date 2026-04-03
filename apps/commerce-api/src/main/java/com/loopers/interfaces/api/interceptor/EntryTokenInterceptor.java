package com.loopers.interfaces.api.interceptor;

import com.loopers.application.queue.ValidateEntryTokenUseCase;
import com.loopers.domain.model.user.UserId;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class EntryTokenInterceptor implements HandlerInterceptor {

    private static final String ENTRY_TOKEN_HEADER = "X-Entry-Token";

    private final ValidateEntryTokenUseCase validateEntryTokenUseCase;

    public EntryTokenInterceptor(ValidateEntryTokenUseCase validateEntryTokenUseCase) {
        this.validateEntryTokenUseCase = validateEntryTokenUseCase;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // POST /api/v1/orders 에만 토큰 검증 적용
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = request.getHeader(ENTRY_TOKEN_HEADER);
        if (token == null || token.isBlank()) {
            throw new CoreException(ErrorType.QUEUE_TOKEN_REQUIRED);
        }

        UserId userId = (UserId) request.getAttribute("authenticatedUserId");
        if (userId == null) {
            throw new CoreException(ErrorType.UNAUTHORIZED);
        }

        // 원자적으로 토큰 검증 + 소비 (동일 토큰 동시 요청 시 정확히 하나만 통과)
        validateEntryTokenUseCase.consume(userId, token);
        return true;
    }
}
