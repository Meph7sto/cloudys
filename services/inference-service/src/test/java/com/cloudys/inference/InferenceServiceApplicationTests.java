package com.cloudys.inference;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class InferenceServiceApplicationTests {

    @Test
    void contextLoads() {
        // 验证 Spring 容器启动成功
    }
}
