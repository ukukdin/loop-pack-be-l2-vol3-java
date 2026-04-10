package com.loopers.interfaces.api.interceptor;

import com.loopers.application.queue.ValidateEntryTokenUseCase;
import com.loopers.domain.model.queue.EntryToken;
import com.loopers.domain.model.queue.QueueProperties;
import com.loopers.domain.model.user.UserId;
import com.loopers.domain.repository.EntryTokenRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class EntryTokenInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(EntryTokenInterceptor.class);
    private static final String ENTRY_TOKEN_HEADER = "X-Entry-Token";
    private static final String CONSUMED_TOKEN_ATTR = "consumedEntryToken";
    private static final String CONSUMED_USER_ATTR = "consumedEntryTokenUserId";

    private final ValidateEntryTokenUseCase validateEntryTokenUseCase;
    private final EntryTokenRepository entryTokenRepository;
    private final QueueProperties queueProperties;

    public EntryTokenInterceptor(ValidateEntryTokenUseCase validateEntryTokenUseCase,
                                 EntryTokenRepository entryTokenRepository,
                                 QueueProperties queueProperties) {
        this.validateEntryTokenUseCase = validateEntryTokenUseCase;
        this.entryTokenRepository = entryTokenRepository;
        this.queueProperties = queueProperties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // POST /api/v1/orders 에만 토큰 검증 적용
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = request.getHeader(ENTRY_TOKEN_HEADER);
        if (token == null || token.isBlank()) {
            log.debug("Entry token missing for request to {}", request.getRequestURI());
            throw new CoreException(ErrorType.QUEUE_TOKEN_REQUIRED);
        }

        UserId userId = (UserId) request.getAttribute("authenticatedUserId");
        if (userId == null) {
            log.warn("authenticatedUserId not set - interceptor order issue?");
            throw new CoreException(ErrorType.UNAUTHORIZED);
        }

        log.debug("Consuming entry token for userId={}", userId);
        // 원자적으로 토큰 검증 + 소비 (동일 토큰 동시 요청 시 정확히 하나만 통과)
        validateEntryTokenUseCase.consume(userId, token);

        // 주문 실패 시 토큰 복구를 위해 소비된 토큰 정보를 request attribute에 저장
        request.setAttribute(CONSUMED_TOKEN_ATTR, token);
        request.setAttribute(CONSUMED_USER_ATTR, userId);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        if (ex == null) {
            return;
        }

        // 주문 처리 중 예외 발생 시 소비된 토큰을 복구
        String token = (String) request.getAttribute(CONSUMED_TOKEN_ATTR);
        UserId userId = (UserId) request.getAttribute(CONSUMED_USER_ATTR);

        if (token == null || userId == null) {
            return;
        }

        try {
            EntryToken entryToken = EntryToken.of(token, userId);
            entryTokenRepository.save(entryToken, queueProperties.getTokenTtlSeconds());
            log.info("주문 실패로 입장 토큰 복구: userId={}", userId);
        } catch (Exception restoreEx) {
            log.warn("입장 토큰 복구 실패: userId={}, error={}", userId, restoreEx.getMessage());
        }
    }
}
