package com.cloudys.inference.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * WebFlux 安全配置。
 * /health 公开访问，/api/v2/inference/** 与 /inference/** 需要 JWT 认证。
 */
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class InferenceSecurityConfig {

    private final ReactiveJwtAuthenticationFilter jwtFilter;

    public InferenceSecurityConfig(ReactiveJwtAuthenticationFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .authorizeExchange(auth -> auth
                        .pathMatchers("/health").permitAll()
                        .pathMatchers("/api/v2/inference/**").authenticated()
                        .pathMatchers("/inference/**").authenticated()
                        .anyExchange().permitAll()
                )
                .addFilterAt(jwtFilter, SecurityWebFiltersOrder.AUTHENTICATION);

        return http.build();
    }
}
