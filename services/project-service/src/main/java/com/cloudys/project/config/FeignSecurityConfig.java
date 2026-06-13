package com.cloudys.project.config;

import static com.cloudys.common.core.constant.Constants.HEADER_AUTHORIZATION;
import static com.cloudys.common.core.constant.Constants.TOKEN_PREFIX;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import feign.RequestInterceptor;

@Configuration
public class FeignSecurityConfig {

    @Bean
    public RequestInterceptor bearerTokenForwardingInterceptor() {
        return template -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || authentication.getCredentials() == null) {
                return;
            }

            Object credentials = authentication.getCredentials();
            if (!(credentials instanceof String token) || token.isBlank()) {
                return;
            }

            template.header(HEADER_AUTHORIZATION, TOKEN_PREFIX + token);
        };
    }
}
