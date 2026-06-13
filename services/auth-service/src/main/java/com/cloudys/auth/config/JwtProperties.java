package com.cloudys.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String secretKey = "cloudys-jwt-secret-change-in-production";
    private int expiryHours = 24;

    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }

    public int getExpiryHours() { return expiryHours; }
    public void setExpiryHours(int expiryHours) { this.expiryHours = expiryHours; }
}
