package com.cloudys.inference;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class InferenceServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(InferenceServiceApplication.class, args);
    }
}
