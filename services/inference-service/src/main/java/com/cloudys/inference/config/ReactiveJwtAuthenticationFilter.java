package com.cloudys.inference.config;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import com.cloudys.common.core.constant.Constants;
import com.cloudys.common.security.JwtTokenProvider;

import io.jsonwebtoken.Claims;
import reactor.core.publisher.Mono;

/**
 * WebFlux 版 JWT 认证过滤器。
 * 从 Authorization: Bearer <token> 提取验证，设置 ReactiveSecurityContext。
 *
 * <p>注意：WebFlux 不能使用 OncePerRequestFilter (Servlet API)，
 * 必须实现 WebFilter 并使用 ReactiveSecurityContextHolder。
 */
@Component
public class ReactiveJwtAuthenticationFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(ReactiveJwtAuthenticationFilter.class);

    private final JwtTokenProvider tokenProvider;

    public ReactiveJwtAuthenticationFilter(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(Constants.HEADER_AUTHORIZATION);

        if (authHeader != null && authHeader.startsWith(Constants.TOKEN_PREFIX)) {
            String token = authHeader.substring(Constants.TOKEN_PREFIX.length());
            Claims claims = tokenProvider.verifyToken(token);

            if (claims != null) {
                String userId = claims.get("user_id", String.class);
                String username = claims.get("username", String.class);
                String role = claims.get("role", String.class);

                if (userId != null && !userId.isBlank()) {
                    List<SimpleGrantedAuthority> authorities = List.of(
                            new SimpleGrantedAuthority("ROLE_" + (role != null ? role : "member"))
                    );

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userId, null, authorities);
                    authentication.setDetails(Map.of(
                            "userId", userId,
                            "username", username != null ? username : "",
                            "role", role != null ? role : ""
                    ));

                    return chain.filter(exchange)
                            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
                }
            }
        }

        return chain.filter(exchange);
    }
}
