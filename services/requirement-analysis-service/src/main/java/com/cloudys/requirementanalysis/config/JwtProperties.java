package com.cloudys.requirementanalysis.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.cloudys.common.security.JwtTokenProvider;

@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String secretKey;
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

    @Bean
    public JwtTokenProvider jwtTokenProvider() {
        return new JwtTokenProvider(secretKey, expiryHours);
    }
}
