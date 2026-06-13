package com.cloudys.common.pythonbridge;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Python Bridge 自动配置。
 * 当 python.bridge.enabled=true 时（默认），自动注册 PythonBridgeClient bean。
 */
@AutoConfiguration
@EnableConfigurationProperties(PythonBridgeProperties.class)
@ConditionalOnProperty(prefix = "python.bridge", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PythonBridgeAutoConfiguration {

    @Bean
    public PythonBridgeClient pythonBridgeClient(PythonBridgeProperties properties) {
        return new PythonBridgeClient(properties);
    }

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean
    public PythonSidecarManager pythonSidecarManager(PythonBridgeProperties properties) {
        return new PythonSidecarManager(properties);
    }
}
