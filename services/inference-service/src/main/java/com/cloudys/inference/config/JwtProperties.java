package com.cloudys.inference.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.cloudys.common.security.JwtTokenProvider;

/**
 * JWT 配置属性，与 project-service / auth-service 保持一致。
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.JwtConfig.class)
public class JwtProperties {

    @Bean
    public JwtTokenProvider jwtTokenProvider(JwtConfig config) {
        return new JwtTokenProvider(config.getSecretKey(), config.getExpiryHours());
    }

    @ConfigurationProperties(prefix = "jwt")
    public static class JwtConfig {
        private String secretKey = "cloudys-jwt-secret-change-in-production";
        private int expiryHours = 24;

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        public int getExpiryHours() {
            return expiryHours;
        }

        public void setExpiryHours(int expiryHours) {
            this.expiryHours = expiryHours;
        }
    }
}
