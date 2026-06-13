package com.cloudys.gateway.filter;

import com.cloudys.common.core.constant.Constants;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * Gateway JWT 认证全局过滤器。
 * 在请求路由到下游微服务前验证 JWT token，
 * 并将用户信息注入 X-User-Id / X-Username / X-Role 请求头。
 *
 * 对应原 Nginx + Python auth deps 中 get_current_user 的职责。
 */
@Component
public class JwtGatewayFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtGatewayFilter.class);

    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/health",
            "/api/v2/auth/login",
            "/api/v2/auth/register",
            "/api/v2/auth/logout"
    );

    private static final String BODY_401 = "{\"detail\":\"未认证或 token 已过期\"}";

    private final SecretKey secretKey;

    public JwtGatewayFilter(@Value("${jwt.secret-key}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // Public endpoints — pass through without token check
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        // Only check /api/** paths; pass through everything else
        if (!path.startsWith("/api/")) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(Constants.HEADER_AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith(Constants.TOKEN_PREFIX)) {
            return unauthorized(exchange, "missing token");
        }

        String token = authHeader.substring(Constants.TOKEN_PREFIX.length());
        Claims claims = validateToken(token);

        if (claims == null) {
            return unauthorized(exchange, "invalid token");
        }

        // Extract user info and forward as headers
        String userId = claims.get("user_id", String.class);
        String username = claims.get("username", String.class);
        String role = claims.get("role", String.class);

        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header("X-User-Id", userId != null ? userId : "")
                .header("X-Username", username != null ? username : "")
                .header("X-Role", role != null ? role : "")
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    @Override
    public int getOrder() {
        // Run early, before routing
        return -100;
    }

    // ---- private helpers ----

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.contains(path);
    }

    private Claims validateToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.debug("JWT expired: {}", e.getMessage());
            return null;
        } catch (JwtException e) {
            log.debug("JWT invalid: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.debug("JWT parse error: {}", e.getMessage());
            return null;
        }
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String reason) {
        log.debug("Gateway auth rejected: {}", reason);
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        DataBuffer buffer = response.bufferFactory()
                .wrap(BODY_401.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
