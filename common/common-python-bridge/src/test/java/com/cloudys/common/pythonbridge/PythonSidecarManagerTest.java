package com.cloudys.common.pythonbridge;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.Test;

class PythonSidecarManagerTest {

    @SuppressWarnings("unchecked")
    @Test
    void buildCommand_defaultsToUvRunLockedUvicorn() throws Exception {
        PythonBridgeProperties properties = new PythonBridgeProperties();

        Method method = PythonSidecarManager.class.getDeclaredMethod("buildCommand", PythonBridgeProperties.class);
        method.setAccessible(true);

        List<String> command = (List<String>) method.invoke(null, properties);

        assertThat(command).containsExactly(
                "uv",
                "run",
                "--locked",
                "uvicorn",
                "sidecar_app:app",
                "--host",
                "127.0.0.1",
                "--port",
                "8000"
        );
    }

    @SuppressWarnings("unchecked")
    @Test
    void buildCommand_pythonOverrideFallsBackToModuleMode() throws Exception {
        PythonBridgeProperties properties = new PythonBridgeProperties();
        properties.setPythonExecutable("python");

        Method method = PythonSidecarManager.class.getDeclaredMethod("buildCommand", PythonBridgeProperties.class);
        method.setAccessible(true);

        List<String> command = (List<String>) method.invoke(null, properties);

        assertThat(command).containsExactly(
                "python",
                "-m",
                "uvicorn",
                "sidecar_app:app",
                "--host",
                "127.0.0.1",
                "--port",
                "8000"
        );
    }
}
