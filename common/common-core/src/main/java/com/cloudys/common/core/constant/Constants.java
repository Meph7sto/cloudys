package com.cloudys.common.core.constant;

/**
 * 项目级常量定义，对应 Python config.py + schema CHECK 约束中的枚举值。
 */
public final class Constants {

    private Constants() {}

    // ========================
    // 角色
    // ========================
    public static final String ROLE_SUPER_ADMIN = "super_admin";
    public static final String ROLE_ADMIN = "admin";
    public static final String ROLE_MEMBER = "member";
    public static final String ROLE_VIEWER = "viewer";

    // ========================
    // 需求类型
    // ========================
    public static final String REQ_TYPE_TOP_LEVEL = "top_level";
    public static final String REQ_TYPE_LOW_LEVEL = "low_level";
    public static final String REQ_TYPE_TASK = "task";

    // ========================
    // 需求状态
    // ========================
    public static final String REQ_STATUS_DRAFT = "draft";
    public static final String REQ_STATUS_UNDER_REVIEW = "under_review";
    public static final String REQ_STATUS_CONFIRMED = "confirmed";
    public static final String REQ_STATUS_IN_PROGRESS = "in_progress";
    public static final String REQ_STATUS_COMPLETED = "completed";
    public static final String REQ_STATUS_ARCHIVED = "archived";

    // ========================
    // 项目/产品状态
    // ========================
    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_ARCHIVED = "archived";

    // ========================
    // Agent 相关
    // ========================
    public static final String AGENT_TYPE_REQUIREMENT = "requirement_agent";
    public static final String GRAPH_TYPE_TOPOLOGY = "execution_topology_template";
    public static final String GRAPH_TYPE_RUNTIME = "agent_runtime_execution";

    // ========================
    // HTTP 头部
    // ========================
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";

    // ========================
    // 消息角色
    // ========================
    public static final String MSG_ROLE_USER = "user";
    public static final String MSG_ROLE_ASSISTANT = "assistant";
    public static final String MSG_ROLE_SYSTEM = "system";
}
