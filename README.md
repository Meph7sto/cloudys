# cloudys

需求工程语义分析平台 — Spring Boot 3 + Spring Cloud 微服务版。

## 技术栈

| 组件 | 选型 | 说明 |
|------|------|------|
| 框架 | Spring Boot 3.4.x + Spring Cloud 2024.x | Java 17+ |
| 网关 | Spring Cloud Gateway | 统一入口，端口 8008 |
| 服务注册/发现 | Eureka | 服务注册与发现 |
| 负载均衡 | Spring Cloud LoadBalancer | 客户端负载均衡 |
| 熔断降级 | Resilience4j | 替代 Hystrix |
| 远程调用 | Spring Cloud OpenFeign | 微服务间 HTTP 调用 |
| 数据库 | Spring Data JPA + Flyway | ORM + 数据库迁移 |
| 安全 | Spring Security + JWT | HS256，24h 有效期 |
| 响应式 | Spring WebFlux | 支持 SSE 流式响应 (gateway + agent) |
| 构建工具 | Maven 多模块 | 统一依赖管理 |

## 快速开始

### 环境要求

- Java 17+
- Maven 3.9+
- Docker & Docker Compose
- (可选) Python 3.12 + conda (inference-service)

### 1. 启动开发环境

```bash
./start-all.ps1
```

默认宿主机端口:

- Gateway: `25698`
- Frontend: `25699`
- PostgreSQL: `25700`
- Eureka: `25701`
- Python sidecar: `25702`
- Inference: `25703`
- Auth: `25704`
- Project: `25705`
- Requirement: `25706`
- Requirement Analysis: `25707`
- Adminer: `25708`

如需调整，可编辑 `.env` 中的 `HOST_*_PORT` 变量。

### 2. 编译项目

```bash
mvn clean compile -DskipTests
```

### 3. 手动启动微服务

按依赖顺序启动：

```bash
# 1. 注册中心
mvn -pl services/eureka-service spring-boot:run

# 2. 认证服务 (其他服务依赖)
mvn -pl services/auth-service spring-boot:run

# 3. 网关 (入口)
mvn -pl services/gateway-service spring-boot:run

# 4. 其他服务 (可并行)
mvn -pl services/project-service spring-boot:run
mvn -pl services/requirement-service spring-boot:run
mvn -pl services/requirement-analysis-service spring-boot:run
mvn -pl services/inference-service spring-boot:run
```

`inference-service` 默认会从 `services/inference-service/python/sidecar_app.py` 启动 Python sidecar。
该桥接入口复用仓库中的 `Semantic-Atlas/backend_inference` 代码；如果该目录不在默认位置，可通过 `SEMANTIC_ATLAS_BACKEND_INFERENCE` 或 `PYTHON_SIDECAR_WORKDIR` 覆盖。

### 4. 运行测试

```bash
mvn test
```

## 微服务列表

| 服务 | 端口 | spring.application.name | 数据库 |
|------|------|------------------------|--------|
| eureka-service | 8888 | eureka-service | 无 |
| gateway-service | 8008 | gateway-service | 无 |
| auth-service | 8001 | auth-service | PostgreSQL |
| project-service | 8002 | project-service | PostgreSQL |
| requirement-service | 8003 | requirement-service | PostgreSQL |
| requirement-analysis-service | 8004 | requirement-analysis-service | PostgreSQL |
| inference-service | 8006 | inference-service | 无 (Python sidecar) |

## 项目结构

```
cloudys/
├── pom.xml                          # 父 POM
├── docker-compose.yml               # 基础服务定义（仅容器内网络）
├── docker-compose.dev.yml           # 本地开发端口映射
├── docker-compose.vm.yml            # VM 部署端口映射
├── docker-compose.vm.debug.yml      # VM 调试端口映射
├── .env                             # 环境变量
├── common/
│   ├── common-core/                  # 公共工具、异常、常量
│   ├── common-dto/                   # 共享 DTO (record 类型)
│   ├── common-security/              # JWT 认证、Spring Security 配置
│   └── common-python-bridge/         # Python sidecar HTTP/进程管理桥
├── services/
│   ├── eureka-service/               # Eureka 注册中心
│   ├── gateway-service/              # API 网关
│   ├── auth-service/                 # 认证权限服务
│   ├── project-service/              # 项目管理服务
│   ├── requirement-service/          # 需求管理服务
│   ├── requirement-analysis-service/ # 需求分析服务
│   └── inference-service/            # 推理服务 (Java + Python)
│       └── python/                   # sidecar 启动桥接入口
└── frontend/                         # Vue 3 前端 (保持不变)
```

## API 路由

| 前缀 | 目标服务 |
|------|----------|
| `/api/v2/auth/**` | auth-service |
| `/api/v2/permission/**` | auth-service |
| `/api/v2/manage/**` | project-service |
| `/api/v2/product/**` | project-service |
| `/api/v2/requirements/**` | requirement-service |
| `/api/v2/classification/**` | requirement-analysis-service |
| `/api/v2/dedup/**` | requirement-analysis-service |
| `/api/v2/actor/**` | requirement-analysis-service |
| `/api/v2/analysis/**` | requirement-analysis-service |
| `/api/v2/requirement-graph/**` | requirement-analysis-service |
| `/api/v2/requirement-change/**` | requirement-analysis-service |
| `/inference/**` | inference-service |
| `/api/v2/**` (default) | requirement-analysis-service |

## 测试用户

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin | admin | super_admin |
| test | test | super_admin |

## 端口约定

容器内端口保持不变：

| 服务 | 容器内端口 | 说明 |
|------|-----------|------|
| Eureka | 8888 | 服务注册中心 |
| Gateway | 8008 | API 网关 |
| Auth Service | 8001 | 认证服务 |
| Project Service | 8002 | 项目服务 |
| Requirement Service | 8003 | 需求服务 |
| Requirement Analysis Service | 8004 | 分析服务 |
| Inference Service | 8006 | 推理服务 |
| Python Sidecar | 8000 | Python 推理桥 |
| PostgreSQL | 5432 | 数据库 |
| Adminer | 8080 | 数据库管理 |

宿主机开发端口默认从 `25698` 开始，见 `.env.example`。

## 文档

- [Deployment Guide](docs/DEPLOYMENT.md) — 完整部署指南 (Docker Compose + Kubernetes)
- [Troubleshooting](docs/TROUBLESHOOTING.md) — 故障排除指南
- [Production Checklist](docs/PRODUCTION-CHECKLIST.md) — 生产环境检查清单
- [API Endpoints Checklist](docs/API-ENDPOINTS-CHECKLIST.md) — 前端 API 对接验证清单

## 测试

```bash
# 单元测试 & 单服务集成测试
mvn test

# Gateway 测试
mvn test -pl services/gateway-service

# 全链路集成测试 (需 Docker)
mvn test -pl services/full-link-tests

# API 兼容性测试
mvn test -pl services/api-compatibility-tests

# 性能测试 (需 k6)
./deploy/perf/run-perf-test.sh http://localhost:25698
```

## 分阶段实施

- **Phase 0**: 基础设施搭建 ✅
- **Phase 1**: 认证权限服务 ✅
- **Phase 2**: 项目管理服务 ✅
- **Phase 3**: 推理服务 + Python Bridge ✅
- **Phase 4**: 需求管理服务 ✅
- **Phase 5**: 需求分析服务 ✅
- **Phase 6**: 集成测试 & 上线 ✅
- **Phase 7**: 云原生部署与课程验收 (待执行)
