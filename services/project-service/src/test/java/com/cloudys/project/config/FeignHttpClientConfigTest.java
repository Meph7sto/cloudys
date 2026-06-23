package com.cloudys.project.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import feign.Client;
import feign.hc5.ApacheHttp5Client;

@SpringBootTest
@ActiveProfiles("test")
class FeignHttpClientConfigTest {

    @Autowired
    private Client feignClient;

    @Test
    void feignClientRemainsLoadBalancedWhenHttpClient5IsEnabled() {
        assertThat(feignClient).isNotNull();
        assertThat(feignClient).isNotInstanceOf(ApacheHttp5Client.class);
        assertThat(feignClient.getClass().getName()).contains("LoadBalancer");
    }
}
