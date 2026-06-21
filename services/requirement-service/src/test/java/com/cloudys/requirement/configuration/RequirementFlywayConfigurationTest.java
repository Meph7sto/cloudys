package com.cloudys.requirement.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequirementFlywayConfigurationTest {

    @Test
    void sharedSchemaFlywayShouldBaselineAtVersionZero() {
        Properties properties = loadApplicationProperties();

        assertEquals("true", properties.getProperty("spring.flyway.baseline-on-migrate"));
        assertEquals("0", properties.getProperty("spring.flyway.baseline-version"));
        assertTrue(properties.getProperty("spring.flyway.table", "").contains("requirement_flyway_schema_history"));
    }

    private static Properties loadApplicationProperties() {
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(new ClassPathResource("application.yml"));
        Properties properties = factory.getObject();
        return properties != null ? properties : new Properties();
    }
}
