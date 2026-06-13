package com.cloudys.gateway.filter;

import com.cloudys.common.core.constant.Constants;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.mockito.ArgumentCaptor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpResponse;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link JwtGatewayFilter}.
 * Tests token validation, public path bypass, header injection, and error responses.
 */
@DisplayName("JwtGatewayFilter")
class JwtGatewayFilterTest {

    private static final String TEST_SECRET = "test-gateway-jwt-secret-minimum-32-chars";
    private static final String TEST_USER_ID = "user-001";
    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_ROLE = "admin";

    private JwtGatewayFilter filter;
    private GatewayFilterChain mockChain;
    private ArgumentCaptor<ServerWebExchange> exchangeCaptor;

    @BeforeEach
    void setUp() {
        filter = new JwtGatewayFilter(TEST_SECRET);
        mockChain = mock(GatewayFilterChain.class);
        exchangeCaptor = ArgumentCaptor.forClass(ServerWebExchange.class);
        when(mockChain.filter(exchangeCaptor.capture())).thenReturn(Mono.empty());
    }

    /** Create a valid JWT token with the given claims. */
    private String createValidToken() {
        return createToken(TEST_USER_ID, TEST_USERNAME, TEST_ROLE, 3600);
    }

    private String createToken(String userId, String username, String role, long expirySeconds) {
        SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();
        return Jwts.builder()
                .claim("user_id", userId)
                .claim("username", username)
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirySeconds)))
                .signWith(key)
                .compact();
    }

    private ServerWebExchange createExchange(String path) {
        return createExchange(path, null);
    }

    private ServerWebExchange createExchange(String path, String authHeader) {
        MockServerHttpRequest.BaseBuilder<?> builder = MockServerHttpRequest.get(path);
        if (authHeader != null) {
            builder.header(Constants.HEADER_AUTHORIZATION, authHeader);
        }
        return MockServerWebExchange.from(builder.build());
    }

    @SuppressWarnings("unchecked")
    @Nested
    @DisplayName("Token Validation")
    class TokenValidation {

        @Test
        @DisplayName("should pass valid token and inject headers")
        void shouldPassValidToken() {
            String token = createValidToken();
            ServerWebExchange exchange = createExchange(
                    "/api/v2/manage/projects",
                    Constants.TOKEN_PREFIX + token);

            filter.filter(exchange, mockChain).block();

            ServerWebExchange capturedExchange = exchangeCaptor.getValue();
            ServerHttpRequest mutated = capturedExchange.getRequest();
            assertThat(mutated.getHeaders().getFirst("X-User-Id")).isEqualTo(TEST_USER_ID);
            assertThat(mutated.getHeaders().getFirst("X-Username")).isEqualTo(TEST_USERNAME);
            assertThat(mutated.getHeaders().getFirst("X-Role")).isEqualTo(TEST_ROLE);
        }

        @Test
        @DisplayName("should reject expired token with 401")
        void shouldRejectExpiredToken() {
            String token = createToken(TEST_USER_ID, TEST_USERNAME, TEST_ROLE, -1);
            ServerWebExchange exchange = createExchange(
                    "/api/v2/auth/me",
                    Constants.TOKEN_PREFIX + token);

            filter.filter(exchange, mockChain).block();

            MockServerHttpResponse response = (MockServerHttpResponse) exchange.getResponse();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            verify(mockChain, never()).filter(any());
        }

        @Test
        @DisplayName("should reject token with invalid signature")
        void shouldRejectInvalidSignature() {
            String token = createValidToken();
            String tampered = token.substring(0, token.length() - 5) + "XXXXX";
            ServerWebExchange exchange = createExchange(
                    "/api/v2/projects",
                    Constants.TOKEN_PREFIX + tampered);

            filter.filter(exchange, mockChain).block();

            MockServerHttpResponse response = (MockServerHttpResponse) exchange.getResponse();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            verify(mockChain, never()).filter(any());
        }

        @Test
        @DisplayName("should reject missing Authorization header")
        void shouldRejectMissingAuthHeader() {
            ServerWebExchange exchange = createExchange("/api/v2/auth/me");

            filter.filter(exchange, mockChain).block();

            MockServerHttpResponse response = (MockServerHttpResponse) exchange.getResponse();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            verify(mockChain, never()).filter(any());
        }

        @Test
        @DisplayName("should reject malformed Authorization header")
        void shouldRejectMalformedAuthHeader() {
            ServerWebExchange exchange = createExchange(
                    "/api/v2/projects",
                    "NotBearer xyz123");

            filter.filter(exchange, mockChain).block();

            MockServerHttpResponse response = (MockServerHttpResponse) exchange.getResponse();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            verify(mockChain, never()).filter(any());
        }

        @Test
        @DisplayName("should pass through when token has null claims")
        void shouldPassThroughWithNullClaims() {
            SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
            String token = Jwts.builder()
                    .issuedAt(new Date())
                    .expiration(Date.from(Instant.now().plusSeconds(3600)))
                    .signWith(key)
                    .compact();

            ServerWebExchange exchange = createExchange(
                    "/api/v2/manage/list",
                    Constants.TOKEN_PREFIX + token);

            filter.filter(exchange, mockChain).block();

            ServerWebExchange capturedExchange = exchangeCaptor.getValue();
            ServerHttpRequest mutated = capturedExchange.getRequest();
            assertThat(mutated.getHeaders().getFirst("X-User-Id")).isEqualTo("");
            assertThat(mutated.getHeaders().getFirst("X-Username")).isEqualTo("");
            assertThat(mutated.getHeaders().getFirst("X-Role")).isEqualTo("");
        }
    }

    @Nested
    @DisplayName("Public Path Bypass")
    class PublicPathBypass {

        @Test
        @DisplayName("should bypass filter for /health")
        void shouldBypassHealth() {
            ServerWebExchange exchange = createExchange("/health");
            filter.filter(exchange, mockChain).block();
            verify(mockChain).filter(any());
        }

        @Test
        @DisplayName("should bypass filter for /api/v2/auth/login")
        void shouldBypassLogin() {
            ServerWebExchange exchange = createExchange("/api/v2/auth/login");
            filter.filter(exchange, mockChain).block();
            verify(mockChain).filter(any());
        }

        @Test
        @DisplayName("should bypass filter for /api/v2/auth/register")
        void shouldBypassRegister() {
            ServerWebExchange exchange = createExchange("/api/v2/auth/register");
            filter.filter(exchange, mockChain).block();
            verify(mockChain).filter(any());
        }

        @Test
        @DisplayName("should bypass filter for /api/v2/auth/logout")
        void shouldBypassLogout() {
            ServerWebExchange exchange = createExchange("/api/v2/auth/logout");
            filter.filter(exchange, mockChain).block();
            verify(mockChain).filter(any());
        }

        @Test
        @DisplayName("should bypass filter for non-API paths")
        void shouldBypassNonApiPaths() {
            ServerWebExchange exchange = createExchange("/some/other/path");
            filter.filter(exchange, mockChain).block();
            verify(mockChain).filter(any());
        }
    }

    @Nested
    @DisplayName("401 Response Format")
    class UnauthorizedResponse {

        @Test
        @DisplayName("should return 401 with JSON content type")
        void shouldReturnCorrectJsonContentType() {
            ServerWebExchange exchange = createExchange("/api/v2/projects");

            filter.filter(exchange, mockChain).block();

            MockServerHttpResponse response = (MockServerHttpResponse) exchange.getResponse();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getHeaders().getContentType()).isNotNull();
        }
    }

    @Test
    @DisplayName("should have order -100 to run early before routing")
    void shouldHaveCorrectOrder() {
        assertThat(filter.getOrder()).isEqualTo(-100);
    }
}
