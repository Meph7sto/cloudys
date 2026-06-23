package com.cloudys.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "gateway.rate-limit")
public class GatewayRateLimitProperties {

    private boolean enabled = true;
    private RouteLimit defaultLimit = new RouteLimit(100, 200, 1);
    private Map<String, RouteLimit> routes = new LinkedHashMap<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public RouteLimit getDefault() {
        return defaultLimit;
    }

    public void setDefault(RouteLimit defaultLimit) {
        this.defaultLimit = defaultLimit;
    }

    public Map<String, RouteLimit> getRoutes() {
        return routes;
    }

    public void setRoutes(Map<String, RouteLimit> routes) {
        this.routes = routes;
    }

    public static class RouteLimit {

        private int replenishRate;
        private int burstCapacity;
        private int requestedTokens = 1;

        public RouteLimit() {
        }

        public RouteLimit(int replenishRate, int burstCapacity, int requestedTokens) {
            this.replenishRate = replenishRate;
            this.burstCapacity = burstCapacity;
            this.requestedTokens = requestedTokens;
        }

        public int getReplenishRate() {
            return replenishRate;
        }

        public void setReplenishRate(int replenishRate) {
            this.replenishRate = replenishRate;
        }

        public int getBurstCapacity() {
            return burstCapacity;
        }

        public void setBurstCapacity(int burstCapacity) {
            this.burstCapacity = burstCapacity;
        }

        public int getRequestedTokens() {
            return requestedTokens;
        }

        public void setRequestedTokens(int requestedTokens) {
            this.requestedTokens = requestedTokens;
        }
    }
}
