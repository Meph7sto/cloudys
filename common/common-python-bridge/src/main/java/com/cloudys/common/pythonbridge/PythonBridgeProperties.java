package com.cloudys.common.pythonbridge;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Python Bridge HTTP 连接配置。
 * Phase 3: HTTP Bridge 方案 — Java 通过 WebClient 调用 Python FastAPI sidecar。
 */
@ConfigurationProperties(prefix = "python.bridge")
public class PythonBridgeProperties {

    /** 是否启用 HTTP bridge */
    private boolean enabled = true;

    /** Python sidecar 基础 URL（默认 http://localhost:8000） */
    private String baseUrl = "http://localhost:8000";

    /** 是否由 Java 侧托管本地 Python sidecar 进程 */
    private boolean manageProcess = false;

    /** Python 可执行文件 */
    private String pythonExecutable = "python";

    /** Sidecar 工作目录 */
    private String workingDirectory = "python";

    /** Sidecar 模块入口，例如 sidecar_app:app */
    private String appModule = "sidecar_app:app";

    /** Sidecar 监听主机 */
    private String host = "127.0.0.1";

    /** Sidecar 监听端口 */
    private int port = 8000;

    /** Sidecar 启动后最大等待就绪时间 (ms) */
    private int startupTimeoutMs = 90000;

    /** 是否在服务启动时执行预热 */
    private boolean warmupEnabled = true;

    /** 预热请求文本 */
    private String warmupPrompt = "ping";

    /** 额外环境变量，格式 KEY=VALUE */
    private java.util.List<String> environment = java.util.List.of();

    /** 连接超时 (ms) */
    private int connectTimeoutMs = 5000;

    /** 读取超时 (ms)，LLM 推理可能较慢 */
    private int readTimeoutMs = 60000;

    /** 响应体最大内存缓冲大小 */
    private String maxInMemorySize = "16MB";

    /** 连接池最大连接数 */
    private int maxConnections = 50;

    /** 连接池空闲超时 (ms) */
    private int maxIdleTimeoutMs = 30000;

    // --- getters / setters ---

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isManageProcess() {
        return manageProcess;
    }

    public void setManageProcess(boolean manageProcess) {
        this.manageProcess = manageProcess;
    }

    public String getPythonExecutable() {
        return pythonExecutable;
    }

    public void setPythonExecutable(String pythonExecutable) {
        this.pythonExecutable = pythonExecutable;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    public String getAppModule() {
        return appModule;
    }

    public void setAppModule(String appModule) {
        this.appModule = appModule;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getStartupTimeoutMs() {
        return startupTimeoutMs;
    }

    public void setStartupTimeoutMs(int startupTimeoutMs) {
        this.startupTimeoutMs = startupTimeoutMs;
    }

    public boolean isWarmupEnabled() {
        return warmupEnabled;
    }

    public void setWarmupEnabled(boolean warmupEnabled) {
        this.warmupEnabled = warmupEnabled;
    }

    public String getWarmupPrompt() {
        return warmupPrompt;
    }

    public void setWarmupPrompt(String warmupPrompt) {
        this.warmupPrompt = warmupPrompt;
    }

    public java.util.List<String> getEnvironment() {
        return environment;
    }

    public void setEnvironment(java.util.List<String> environment) {
        this.environment = environment;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

    public String getMaxInMemorySize() {
        return maxInMemorySize;
    }

    public void setMaxInMemorySize(String maxInMemorySize) {
        this.maxInMemorySize = maxInMemorySize;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }

    public int getMaxIdleTimeoutMs() {
        return maxIdleTimeoutMs;
    }

    public void setMaxIdleTimeoutMs(int maxIdleTimeoutMs) {
        this.maxIdleTimeoutMs = maxIdleTimeoutMs;
    }
}
