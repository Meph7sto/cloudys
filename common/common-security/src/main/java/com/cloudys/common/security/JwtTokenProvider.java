package com.cloudys.common.security;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * JWT Token 签发与验证，对应 Python AuthService 的核心逻辑。
 * 算法: HS256，有效期: 24 小时。
 */
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final int expiryHours;

    public JwtTokenProvider(String secret, int expiryHours) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.expiryHours = expiryHours;
    }

    /**
     * 生成 JWT Token。
     * Payload: user_id, username, role, external_type, iat, exp
     */
    public String generateToken(String userId, String username, String role, String externalType) {
        Instant now = Instant.now();
        Instant expiry = now.plus(expiryHours, ChronoUnit.HOURS);

        return Jwts.builder()
                .claims(Map.of(
                        "user_id", userId != null ? userId : "",
                        "username", username != null ? username : "",
                        "role", role != null ? role : "",
                        "external_type", externalType != null ? externalType : ""
                ))
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(secretKey)
                .compact();
    }

    /**
     * 验证 JWT Token，返回解码后的 Claims，失败返回 null。
     */
    public Claims verifyToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            return null;
        } catch (JwtException e) {
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取 Token 过期时间。
     */
    public Instant getExpiry() {
        return Instant.now().plus(expiryHours, ChronoUnit.HOURS);
    }
}
