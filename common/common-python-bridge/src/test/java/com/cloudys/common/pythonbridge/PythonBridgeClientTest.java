package com.cloudys.common.pythonbridge;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class PythonBridgeClientTest {

    @Test
    void constructor_createsClientSuccessfully() {
        var props = new PythonBridgeProperties();
        props.setBaseUrl("http://localhost:8000");
        props.setConnectTimeoutMs(5000);
        props.setReadTimeoutMs(60000);
        props.setMaxConnections(10);

        assertDoesNotThrow(() -> new PythonBridgeClient(props));
    }

    @Test
    void properties_defaultValues_areCorrect() {
        var props = new PythonBridgeProperties();

        assertThat(props.getBaseUrl()).isEqualTo("http://localhost:8000");
        assertThat(props.isEnabled()).isTrue();
        assertThat(props.isManageProcess()).isFalse();
        assertThat(props.getPythonExecutable()).isEqualTo("uv");
        assertThat(props.getWorkingDirectory()).isEqualTo("services/python-sidecar");
        assertThat(props.getAppModule()).isEqualTo("sidecar_app:app");
        assertThat(props.getHost()).isEqualTo("127.0.0.1");
        assertThat(props.getPort()).isEqualTo(8000);
        assertThat(props.getStartupTimeoutMs()).isEqualTo(90000);
        assertThat(props.isWarmupEnabled()).isTrue();
        assertThat(props.getWarmupPrompt()).isEqualTo("ping");
        assertThat(props.getConnectTimeoutMs()).isEqualTo(5000);
        assertThat(props.getReadTimeoutMs()).isEqualTo(60000);
        assertThat(props.getMaxInMemorySize()).isEqualTo("16MB");
        assertThat(props.getMaxConnections()).isEqualTo(50);
        assertThat(props.getMaxIdleTimeoutMs()).isEqualTo(30000);
    }

    @Test
    void properties_setters_work() {
        var props = new PythonBridgeProperties();
        props.setBaseUrl("http://python:8000");
        props.setEnabled(false);
        props.setManageProcess(true);
        props.setPythonExecutable("python3");
        props.setWorkingDirectory("/opt/semantic-atlas");
        props.setAppModule("custom.app:app");
        props.setHost("0.0.0.0");
        props.setPort(18000);
        props.setStartupTimeoutMs(120000);
        props.setWarmupEnabled(false);
        props.setWarmupPrompt("warmup");
        props.setEnvironment(java.util.List.of("A=1", "B=2"));
        props.setConnectTimeoutMs(10000);
        props.setReadTimeoutMs(120000);
        props.setMaxInMemorySize("32MB");
        props.setMaxConnections(100);
        props.setMaxIdleTimeoutMs(60000);

        assertThat(props.getBaseUrl()).isEqualTo("http://python:8000");
        assertThat(props.isEnabled()).isFalse();
        assertThat(props.isManageProcess()).isTrue();
        assertThat(props.getPythonExecutable()).isEqualTo("python3");
        assertThat(props.getWorkingDirectory()).isEqualTo("/opt/semantic-atlas");
        assertThat(props.getAppModule()).isEqualTo("custom.app:app");
        assertThat(props.getHost()).isEqualTo("0.0.0.0");
        assertThat(props.getPort()).isEqualTo(18000);
        assertThat(props.getStartupTimeoutMs()).isEqualTo(120000);
        assertThat(props.isWarmupEnabled()).isFalse();
        assertThat(props.getWarmupPrompt()).isEqualTo("warmup");
        assertThat(props.getEnvironment()).containsExactly("A=1", "B=2");
        assertThat(props.getConnectTimeoutMs()).isEqualTo(10000);
        assertThat(props.getReadTimeoutMs()).isEqualTo(120000);
        assertThat(props.getMaxInMemorySize()).isEqualTo("32MB");
        assertThat(props.getMaxConnections()).isEqualTo(100);
        assertThat(props.getMaxIdleTimeoutMs()).isEqualTo(60000);
    }

    @Test
    void client_hasWebClient_andObjectMapper() {
        var props = new PythonBridgeProperties();
        var client = new PythonBridgeClient(props);

        assertThat(client.getWebClient()).isNotNull();
        assertThat(client.getObjectMapper()).isNotNull();
    }

    @Test
    void maxInMemorySize_parsing_handlesValidValues() {
        var props = new PythonBridgeProperties();
        props.setMaxInMemorySize("32MB");
        // Should not throw when constructing client
        assertDoesNotThrow(() -> new PythonBridgeClient(props));
    }
}
