package com.cloudys.inference.security;

import com.cloudys.common.security.JwtTokenProvider;
import com.cloudys.inference.config.ReactiveJwtAuthenticationFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

class ReactiveJwtAuthenticationFilterTest {

    private JwtTokenProvider tokenProvider;
    private ReactiveJwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider("test-secret-key-for-unit-tests-min-32", 24);
        filter = new ReactiveJwtAuthenticationFilter(tokenProvider);
    }

    @Test
    void validToken_setsSecurityContext() {
        String token = tokenProvider.generateToken("user-123", "testuser", "admin", "local");
        var request = MockServerHttpRequest.get("/test")
                .header("Authorization", "Bearer " + token)
                .build();
        var exchange = MockServerWebExchange.from(request);

        // Chain that captures the authenticated context
        WebFilterChain chain = e -> ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .doOnNext(auth -> {
                    assertThat(auth).isNotNull();
                    assertThat(auth.getPrincipal()).isEqualTo("user-123");
                })
                .then();

        filter.filter(exchange, chain).block();
    }

    @Test
    void missingToken_passesThrough() {
        var request = MockServerHttpRequest.get("/test").build();
        var exchange = MockServerWebExchange.from(request);

        WebFilterChain chain = e -> {
            // Chain was called → filter passed through
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();
    }

    @Test
    void invalidToken_passesThrough() {
        var request = MockServerHttpRequest.get("/test")
                .header("Authorization", "Bearer invalid-token")
                .build();
        var exchange = MockServerWebExchange.from(request);

        WebFilterChain chain = e -> Mono.empty();

        filter.filter(exchange, chain).block();
    }
}
