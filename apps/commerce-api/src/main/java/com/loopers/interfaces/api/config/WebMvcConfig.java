package com.loopers.interfaces.api.config;

import com.loopers.interfaces.api.interceptor.AdminAuthenticationInterceptor;
import com.loopers.interfaces.api.interceptor.AuthenticationInterceptor;
import com.loopers.interfaces.api.interceptor.EntryTokenInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthenticationInterceptor authenticationInterceptor;
    private final AdminAuthenticationInterceptor adminAuthenticationInterceptor;
    private final EntryTokenInterceptor entryTokenInterceptor;

    public WebMvcConfig(AuthenticationInterceptor authenticationInterceptor,
                        AdminAuthenticationInterceptor adminAuthenticationInterceptor,
                        EntryTokenInterceptor entryTokenInterceptor) {
        this.authenticationInterceptor = authenticationInterceptor;
        this.adminAuthenticationInterceptor = adminAuthenticationInterceptor;
        this.entryTokenInterceptor = entryTokenInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authenticationInterceptor)
                .order(1)
                .addPathPatterns("/api/v1/users/me", "/api/v1/users/me/**")
                .addPathPatterns("/api/v1/users/*/likes")
                .addPathPatterns("/api/v1/products/*/likes")
                .addPathPatterns("/api/v1/orders", "/api/v1/orders/**")
                .addPathPatterns("/api/v1/queue/**")
                .addPathPatterns("/api/v1/coupons/*/issue");

        // 주문 생성 시 입장 토큰 검증 (인증 인터셉터 이후 실행)
        registry.addInterceptor(entryTokenInterceptor)
                .order(2)
                .addPathPatterns("/api/v1/orders");

        registry.addInterceptor(adminAuthenticationInterceptor)
                .addPathPatterns("/api-admin/v1/**");
    }
}
