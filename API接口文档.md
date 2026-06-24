# Cloudys（云原生版）— 需求工程语义分析平台 API 接口文档

> 生成日期：2026-06-24 | 共计 ~172 个网关可访问路径

## 系统架构概览

```
前端 (Vue 3 :25699) → 网关 (:8008) → 微服务集群 → PostgreSQL
                              ↓
                      Python AI Sidecar → DeepSeek API
```

共 **7 个微服务** + **1 个 Python AI 推理 sidecar** + **1 个 Vue 3 前端**。

---

## 一、网关服务 (Gateway Service, `:8008`)

统一入口，JWT 鉴权过滤、Bucket4j 限流、路由转发。以下为路由映射表：

| 路由 ID | 路径匹配 | 目标服务 |
|------|------|------|
| `auth-service-auth` | `/api/v2/auth/**` | auth-service |
| `auth-service-permission` | `/api/v2/permission/**` | auth-service |
| `project-service-manage` | `/api/v2/manage/**` | project-service |
| `project-service-product` | `/api/v2/product/**` | project-service |
| `requirement-service-requirements` | `/api/v2/requirements/**`（排除下方特定路径） | requirement-service |
| `analysis-service-requirements-compat` | `/api/v2/requirements/extract_l123` 等 | requirement-analysis-service |
| `analysis-service-classification` | `/api/v2/classification/**` | requirement-analysis-service |
| `analysis-service-dedup` | `/api/v2/dedup/**` | requirement-analysis-service |
| `analysis-service-actor` | `/api/v2/actor/**` | requirement-analysis-service |
| `analysis-service-analysis` | `/api/v2/analysis/**` | requirement-analysis-service |
| `analysis-service-requirement-graph` | `/api/v2/requirement-graph/**` | requirement-analysis-service |
| `analysis-service-requirement-change` | `/api/v2/requirement-change/**` | requirement-analysis-service |
| `analysis-service-default` | `/api/v2/**`（兜底，最低优先级） | requirement-analysis-service |
| `inference-service-chat` | `/api/v2/inference/chat/**` | inference-service |
| `inference-service-classification` | `/api/v2/inference/classification/**` | inference-service |
| `inference-service-conflict` | `/api/v2/inference/conflict/**` | inference-service |
| `inference-service-traceability` | `/api/v2/inference/traceability/**` | inference-service |
| `inference-service-kb` | `/api/v2/inference/kb/**` | inference-service |
| `inference-service-l4` | `/api/v2/inference/l4/**` | inference-service |
| `inference-service-acquisition` | `/api/v2/inference/acquisition/**` | inference-service |
| `inference-service-compat` | `/inference/**` | inference-service（v1 兼容） |

---

## 二、认证服务 (Auth Service, `:8001`)

### 2.1 登录认证 — `/api/v2/auth`

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/v2/auth/login` | 用户登录（用户名+密码+可选角色），返回 JWT |
| `POST` | `/api/v2/auth/register` | 用户注册 |
| `POST` | `/api/v2/auth/logout` | 注销 |
| `GET` | `/api/v2/auth/verify` | 验证 Token 有效性，返回基础用户信息 |
| `GET` | `/api/v2/auth/me` | 获取当前用户完整信息 |
| `GET` | `/api/v2/auth/profile` | 获取个人资料（`/me` 别名） |
| `PATCH` | `/api/v2/auth/profile` | 更新个人资料（显示名称、头像） |
| `POST` | `/api/v2/auth/change-password` | 修改密码 |
| `POST` | `/api/v2/auth/profile/change-password` | 修改密码（别名） |

### 2.2 用户管理

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/v2/auth/user-directory` | 用户目录列表 |
| `GET` | `/api/v2/auth/users` | 用户列表（管理员，可按角色筛选） |
| `POST` | `/api/v2/auth/users` | 创建用户（管理员） |
| `PATCH` | `/api/v2/auth/users/{userId}` | 更新用户（管理员） |
| `GET` | `/api/v2/auth/scope-options` | 获取可选权限范围选项 |
| `GET` | `/api/v2/auth/user-scopes/{userId}` | 获取用户权限范围 |
| `GET` | `/api/v2/auth/users/{userId}/scopes` | 获取用户权限范围（兼容别名） |
| `PUT` | `/api/v2/auth/user-scopes/{userId}` | 更新/替换用户权限范围 |
| `PUT` | `/api/v2/auth/users/{userId}/scopes` | 更新用户权限范围（兼容别名） |

### 2.3 注册审批

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/v2/auth/registrations` | 注册申请列表（管理员） |
| `GET` | `/api/v2/auth/registrations/{userId}` | 注册申请详情 |
| `POST` | `/api/v2/auth/registrations/{userId}/approve` | 批准注册 |
| `POST` | `/api/v2/auth/registrations/{userId}/reject` | 拒绝注册 |
| `GET` | `/api/v2/auth/registrations/pending/count` | 待审批注册数量 |

### 2.4 权限管理 — `/api/v2/permission`

#### 项目成员

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/v2/permission/projects/{projectId}/members` | 添加项目成员 |
| `GET` | `/api/v2/permission/projects/{projectId}/members` | 项目成员列表 |
| `DELETE` | `/api/v2/permission/projects/{projectId}/members/{userId}` | 移除项目成员 |
| `PUT` | `/api/v2/permission/projects/{projectId}/members/{userId}/roles` | 设置成员角色 |
| `GET` | `/api/v2/permission/projects/{projectId}/context` | 获取用户在某项目中的权限上下文 |

#### 基线管理

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/v2/permission/projects/{projectId}/baselines` | 创建基线 |
| `GET` | `/api/v2/permission/projects/{projectId}/baselines` | 项目基线列表 |
| `POST` | `/api/v2/permission/baselines/{id}/lock` | 锁定基线 |
| `POST` | `/api/v2/permission/baselines/{id}/unlock` | 解锁基线 |

#### 变更请求

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/v2/permission/requirements/{requirementId}/change-requests` | 创建变更请求 |
| `GET` | `/api/v2/permission/requirements/{requirementId}/change-requests` | 需求的变更请求列表 |
| `POST` | `/api/v2/permission/change-requests/{id}/approve` | 批准变更请求 |
| `POST` | `/api/v2/permission/change-requests/{id}/reject` | 拒绝变更请求 |
| `GET` | `/api/v2/permission/change-requests/pending` | 所有待处理变更请求 |
| `GET` | `/api/v2/permission/user/pending-reviews` | 当前用户的待审列表 |

#### 评审

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/v2/permission/requirements/{requirementId}/reviews` | 创建评审 |
| `GET` | `/api/v2/permission/requirements/{requirementId}/reviews` | 需求的评审列表 |
| `POST` | `/api/v2/permission/reviews/{id}/approve` | 批准评审 |
| `POST` | `/api/v2/permission/reviews/{id}/reject` | 拒绝评审 |
| `POST` | `/api/v2/permission/reviews/{id}/withdraw` | 撤回评审 |

---

## 三、项目服务 (Project Service, `:8002`)

### 3.1 项目管理 — `/api/v2/manage`

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/v2/manage/projects` | 创建项目 |
| `GET` | `/api/v2/manage/projects` | 项目列表 |
| `GET` | `/api/v2/manage/projects/{projectId}` | 项目详情 |
| `PATCH` | `/api/v2/manage/projects/{projectId}` | 更新项目 |
| `DELETE` | `/api/v2/manage/projects/{projectId}` | 归档/删除项目 |

### 3.2 里程碑

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/v2/manage/projects/{projectId}/milestones` | 创建里程碑 |
| `GET` | `/api/v2/manage/projects/{projectId}/milestones` | 里程碑列表 |
| `GET` | `/api/v2/manage/milestones/{milestoneId}` | 里程碑详情 |
| `POST` | `/api/v2/manage/milestones/{milestoneId}/baseline` | 将里程碑标记为基线 |

### 3.3 分支

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/v2/manage/projects/{projectId}/branches` | 创建分支 |
| `GET` | `/api/v2/manage/projects/{projectId}/branches` | 分支列表 |

### 3.4 兼容层（需求/缺陷/审计/可追溯性 via manage 路径）

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/v2/manage/projects/{projectId}/requirements` | 项目需求列表（兼容） |
| `POST` | `/api/v2/manage/projects/{projectId}/requirements` | 创建需求（兼容） |
| `PATCH` | `/api/v2/manage/requirements/{reqId}` | 更新需求（兼容） |
| `POST` | `/api/v2/manage/requirements/bulk-status` | 批量更新需求状态 |
| `POST` | `/api/v2/manage/requirements/{reqId}/move` | 移动需求 |
| `DELETE` | `/api/v2/manage/requirements/{reqId}` | 删除需求 |
| `POST` | `/api/v2/manage/projects/{projectId}/requirements/import` | 导入需求 |
| `GET` | `/api/v2/manage/projects/{projectId}/defects` | 项目缺陷列表 |
| `GET` | `/api/v2/manage/requirements/{reqId}/defects` | 需求关联缺陷列表 |
| `POST` | `/api/v2/manage/projects/{projectId}/defects` | 创建缺陷 |
| `PATCH` | `/api/v2/manage/defects/{defectId}` | 更新缺陷 |
| `DELETE` | `/api/v2/manage/defects/{defectId}` | 删除缺陷 |
| `GET` | `/api/v2/manage/projects/{projectId}/audits` | 审计日志 |
| `GET` | `/api/v2/manage/projects/{projectId}/traceability/overview` | 可追溯性概览 |
| `GET` | `/api/v2/manage/projects/{projectId}/traceability/coverage` | 可追溯性覆盖率 |

### 3.5 产品管理 — `/api/v2/product`

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/v2/product/products` | 创建产品 |
| `GET` | `/api/v2/product/products` | 产品列表 |
| `GET` | `/api/v2/product/products/{productId}` | 产品详情 |
| `PATCH` | `/api/v2/product/products/{productId}` | 更新产品 |
| `DELETE` | `/api/v2/product/products/{productId}` | 归档/删除产品 |
| `GET` | `/api/v2/product/products/{productId}/projects` | 产品下项目列表 |
| `GET` | `/api/v2/product/products/{productId}/overview` | 产品概览 |
| `GET` | `/api/v2/product/products/{productId}/requirements` | 按产品查需求 |
| `GET` | `/api/v2/product/products/{productId}/milestones` | 按产品查里程碑 |
| `GET` | `/api/v2/product/products/{productId}/baselines` | 按产品查基线 |
| `POST` | `/api/v2/product/products/{productId}/projects` | 在产品下创建项目 |
| `POST` | `/api/v2/product/projects/{projectId}/bind` | 将项目绑定至产品 |

---

## 四、需求服务 (Requirement Service, `:8003`)

全部基于 `/api/v2/requirements`。

### 4.1 需求 CRUD

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/v2/requirements/projects/{projectId}/requirements` | 创建需求 |
| `GET` | `/api/v2/requirements/projects/{projectId}/requirements` | 需求列表（支持树形模式） |
| `GET` | `/api/v2/requirements/{reqId}` | 需求详情 |
| `PATCH` | `/api/v2/requirements/{reqId}` | 更新需求 |
| `DELETE` | `/api/v2/requirements/{reqId}` | 删除需求 |

### 4.2 需求操作

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/v2/requirements/bulk-status` | 批量更新需求状态 |
| `POST` | `/api/v2/requirements/{reqId}/move` | 移动需求至其他项目 |
| `POST` | `/api/v2/requirements/{reqId}/baseline` | 设置需求基线 |
| `POST` | `/api/v2/requirements/projects/{projectId}/requirements/import` | 从分析会话导入需求 |

### 4.3 测试用例

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/v2/requirements/projects/{projectId}/test-cases` | 创建测试用例 |
| `GET` | `/api/v2/requirements/projects/{projectId}/test-cases` | 测试用例列表 |
| `POST` | `/api/v2/requirements/{reqId}/bind-testcase` | 绑定测试用例至需求 |

### 4.4 缺陷管理

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/v2/requirements/projects/{projectId}/defects` | 项目缺陷列表 |
| `GET` | `/api/v2/requirements/{reqId}/defects` | 需求关联缺陷列表 |
| `POST` | `/api/v2/requirements/projects/{projectId}/defects` | 创建缺陷 |
| `PATCH` | `/api/v2/requirements/defects/{defectId}` | 更新缺陷 |
| `DELETE` | `/api/v2/requirements/defects/{defectId}` | 删除缺陷 |

---

## 五、需求分析服务 (Requirement Analysis Service, `:8004`)

**核心业务服务**，编排 AI 分析流水线。

### 5.1 分析运行 — `/api/v2/analysis`

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/v2/analysis/run` | 启动完整需求分析流水线 |
| `GET` | `/api/v2/analysis/progress/{analysisRunId}` | **SSE 流式**推送分析进度 |
| `POST` | `/api/v2/analysis/runs` | 保存分析运行记录 |
| `GET` | `/api/v2/analysis/runs` | 分析运行记录列表 |
| `GET` | `/api/v2/analysis/runs/latest` | 最新一次分析运行 |
| `GET` | `/api/v2/analysis/runs/{analysisRunId}` | 指定分析运行详情 |

### 5.2 上下文构建 — `/api/v2/analysis/context`

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/v2/analysis/context/spans` | 创建上下文跨度 |
| `GET` | `/api/v2/analysis/context/spans/{sessionId}` | 按会话列出跨度 |
| `POST` | `/api/v2/analysis/context/runs` | 创建上下文运行 |
| `GET` | `/api/v2/analysis/context/runs/{sessionId}` | 按会话列出上下文运行 |
| `POST` | `/api/v2/analysis/context/links` | 创建跨度链接 |
| `GET` | `/api/v2/analysis/context/links/{contextRunId}` | 按运行列出跨度链接 |
| `POST` | `/api/v2/analysis/context/bundles` | 创建上下文包 |
| `GET` | `/api/v2/analysis/context/bundles/{contextRunId}` | 按运行列出上下文包 |
| `POST` | `/api/v2/analysis/context/bundles/{bundleId}/items` | 向上下文包添加条目 |

### 5.3 需求提取（兼容路由）— `/api/v2/requirements`

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/v2/requirements/extract_l123` | 提取 L1/L2/L3 需求 |
| `POST` | `/api/v2/requirements/extract_l123/stream` | **SSE 流式**提取 L1/L2/L3 |
| `GET` | `/api/v2/requirements/session/{sessionId}` | 按会话列出需求 |
| `GET` | `/api/v2/requirements/stats` | 需求统计 |
| `GET` | `/api/v2/requirements/l4/{sessionId}` | 获取会话 L4 需求 |
| `GET` | `/api/v2/requirements/l4/{sessionId}/exists` | 检查 L4 是否已生成 |
| `DELETE` | `/api/v2/requirements/l4/{sessionId}` | 清除 L4 数据 |
| `POST` | `/api/v2/requirements/l4/generate` | 生成 L4 需求 |

### 5.4 分类 — `/api/v2/classification`

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/v2/classification/classify` | 对需求文本进行分类 |
| `POST` | `/api/v2/classification/predict-texts` | 通用文本分类（兼容） |
| `GET` | `/api/v2/classification/sessions/{sessionId}/results` | 获取分类结果 |
| `GET` | `/api/v2/classification/latest` | 最新分类结果 |

### 5.5 去重 — `/api/v2/dedup`

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/v2/dedup/detect` | 检测重复/相似需求 |
| `GET` | `/api/v2/dedup/sessions/{sessionId}/results` | 获取去重结果 |

### 5.6 需求图谱 — `/api/v2/requirement-graph`

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/v2/requirement-graph/relations` | 搜索需求关系 |
| `GET` | `/api/v2/requirement-graph/snapshots/{snapshotId}` | 获取图谱快照 |
| `POST` | `/api/v2/requirement-graph/invalidate` | 使关系缓存失效 |
| `GET` | `/api/v2/requirement-graph/stats` | 图谱统计信息 |

### 5.7 变更分析 — `/api/v2/requirement-change`

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/v2/requirement-change/analyze` | 分析需求变更影响范围 |
| `GET` | `/api/v2/requirement-change/history/{sessionId}` | 变更历史记录 |

### 5.8 执行者 — `/api/v2/actor`

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/v2/actor` | 创建执行者 |
| `GET` | `/api/v2/actor/requirement/{requirementId}` | 按需求列出执行者 |
| `PATCH` | `/api/v2/actor/{actorId}` | 更新执行者 |
| `DELETE` | `/api/v2/actor/{actorId}` | 删除执行者 |

### 5.9 会话 & Beta 兼容

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/v2/analysis/sessions/{sessionId}/requirements-export` | 导出会话提取的需求 |
| `GET` | `/api/v2/analysis/sample_transcript` | 获取示例转写文本 |
| `POST` | `/api/v2/analysis/ingest_transcript/stream` | **SSE 流式**录入转写文本 |
| `POST` | `/api/v2/analysis/build_context/stream` | **SSE 流式**构建上下文 |
| `GET` | `/api/v2/analysis/context_runs` | 按 session_id 查上下文运行 |
| `GET` | `/api/v2/analysis/spans/{sessionId}` | 按会话列出跨度（Beta 兼容） |
| `GET` | `/api/v2/analysis/span_links/{contextRunId}` | 按运行列出链接（Beta 兼容） |
| `POST` | `/api/v2/conflict/analyze` | 分析冲突（兼容路径） |
| `GET` | `/api/v2/conflict/latest` | 最新冲突结果（兼容路径） |
| `POST` | `/api/v2/trace-by-mapping` | 按映射追溯（兼容路径） |
| `GET` | `/api/v2/trace/latest` | 最新追溯结果（兼容路径） |

---

## 六、推理服务 (Inference Service, `:8006` → Python Sidecar `:8000`)

Java 层通过响应式 WebClient 将请求转发至 Python FastAPI sidecar。每个接口同时暴露 `/api/v2/inference/**` 和 `/inference/**` 两套路径。

### 6.1 通用对话

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/v2/inference/chat/stream` | **NDJSON 流式**通用对话 |
| `POST` | `/api/v2/inference/chat/completions` | 标准聊天补全 |
| `POST` | `/api/v2/inference/chat/completions/tools` | 带工具调用（Tool Use）的聊天补全 |
| `POST` | `/api/v2/inference/chat/completions/tools/stream` | **NDJSON 流式**工具调用聊天 |
| `POST` | `/api/v2/inference/chat/context-completion` | 上下文感知补全 |
| `POST` | `/api/v2/inference/chat/context-completion/stream` | **NDJSON 流式**上下文补全 |

### 6.2 冲突检测

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/v2/inference/conflict/check` | 单对需求冲突检测 |
| `POST` | `/api/v2/inference/conflict/stream-check` | **NDJSON 流式**冲突检测 |

### 6.3 可追溯性分析

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/v2/inference/traceability/relation` | 单对可追溯性关系分析 |
| `POST` | `/api/v2/inference/traceability/batch-relation` | 批量可追溯性分析 |

### 6.4 需求分类

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/v2/inference/classification/predict-texts` | 基于文本的需求分类 |
| `POST` | `/api/v2/inference/classification/predict-csv` | 基于 CSV 文件批量分类（multipart） |

### 6.5 需求提取与生成

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/v2/inference/acquisition/extract` | 从会议记录/文档提取 L1/L2/L3 需求 |
| `POST` | `/api/v2/inference/l4/generate` | 生成 L4 级细化需求 |
| `POST` | `/api/v2/inference/l4/validate` | 验证 L4 需求质量 |

### 6.6 知识库

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/v2/inference/kb/search` | 知识库向量相似度搜索 |
| `GET` | `/api/v2/inference/kb/status` | 知识库索引状态 |
| `POST` | `/api/v2/inference/kb/rebuild` | 重建知识库向量索引 |

---

## 七、Python Sidecar（FastAPI, `:8000`，内部服务）

独立运行，仅由 inference-service 通过 WebClient 直连，不经过网关。

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/health` | 健康检查（含模型/分类器加载状态） |
| `POST` | `/chat/stream` | **NDJSON 流式**对话 |
| `POST` | `/chat/completions` | 标准聊天补全 |
| `POST` | `/chat/completions/tools` | 带工具的聊天补全 |
| `POST` | `/chat/completions/tools/stream` | **NDJSON 流式**工具聊天 |
| `POST` | `/chat/context-completion` | 上下文补全 |
| `POST` | `/chat/context-completion/stream` | **NDJSON 流式**上下文补全 |
| `POST` | `/classification/predict-texts` | 文本分类 |
| `POST` | `/classification/predict-csv` | CSV 批量分类 |
| `POST` | `/conflict/check` | 冲突检测 |
| `POST` | `/conflict/stream_check` | **NDJSON 流式**冲突检测 |
| `POST` | `/traceability/relation` | 单对追溯分析 |
| `POST` | `/traceability/batch-relation` | 批量追溯分析 |
| `POST` | `/kb/search` | 知识库搜索 |
| `GET` | `/kb/status` | 知识库状态 |
| `POST` | `/kb/rebuild` | 重建知识库索引 |
| `POST` | `/l4/generate` | 生成 L4 需求 |
| `POST` | `/l4/validate` | 验证 L4 需求 |
| `POST` | `/acquisition/extract` | 提取 L1/L2/L3 需求 |

---

## 八、服务注册中心 (Eureka, `:8761`)

Netflix Eureka Server，所有微服务注册于此，OpenFeign 通过服务名进行 RPC 调用。

---

## 服务间调用拓扑

```
auth-service        ← OpenFeign ← requirement-service
                    ← OpenFeign ← requirement-analysis-service

project-service     ← OpenFeign ← requirement-service

requirement-service ← OpenFeign ← requirement-analysis-service

inference-service   ← OpenFeign ← requirement-analysis-service
    ↓ (PythonBridgeClient, 响应式 WebClient)
python-sidecar (:8000)
    ↓ (HTTP)
DeepSeek API (外部 LLM)
```

---

## 技术栈速览

| 组件 | 选型 |
|------|------|
| 框架 | Spring Boot 3.4 + Spring Cloud 2024 |
| API 网关 | Spring Cloud Gateway |
| 服务发现 | Netflix Eureka |
| 负载均衡 | Spring Cloud LoadBalancer |
| 熔断降级 | Resilience4j |
| 远程调用 | Spring Cloud OpenFeign |
| 数据库 | PostgreSQL + Spring Data JPA + Flyway |
| 安全 | Spring Security + JWT (HS256, 24h) |
| 响应式 | Spring WebFlux (SSE) |
| 前端 | Vue 3 + Vite + Tailwind CSS + Pinia |
| AI 推理 | Python FastAPI sidecar + DeepSeek API |
| 构建 | Maven 多模块 |
| 容器化 | Docker + Docker Compose |
| 编排 | Kubernetes |
| 监控 | Prometheus + Grafana |
| CI/CD | Jenkins |
