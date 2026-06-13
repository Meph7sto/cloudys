# API Endpoints Checklist — Python FastAPI to Java Spring Boot Migration

前端 Vue 3 代码保持不变，仅需将 API 请求指向新的 Java 后端 Gateway。
本清单逐一映射原 Python 端点至 Java 实现，用于前端对接验证。

## 认证服务 (auth-service)

| # | Python 端点 | HTTP Method | Java 路径 | 验证状态 |
|---|------------|-------------|-----------|---------|
| 1 | `/api/v2/auth/login` | POST | ✅ `/api/v2/auth/login` | ✅ |
| 2 | `/api/v2/auth/register` | POST | ✅ `/api/v2/auth/register` | ✅ |
| 3 | `/api/v2/auth/logout` | POST | ✅ `/api/v2/auth/logout` | ✅ |
| 4 | `/api/v2/auth/verify` | GET | ✅ `/api/v2/auth/verify` | ✅ |
| 5 | `/api/v2/auth/me` | GET | ✅ `/api/v2/auth/me` | ✅ |
| 6 | `/api/v2/auth/profile` | PATCH | ✅ `/api/v2/auth/profile` | ✅ |
| 7 | `/api/v2/auth/profile/change-password` | PUT | ✅ `/api/v2/auth/profile/change-password` | ✅ |
| 8 | `/api/v2/auth/users` | GET | ✅ `/api/v2/auth/users` (admin) | ✅ |
| 9 | `/api/v2/auth/users` | POST | ✅ `/api/v2/auth/users` (admin) | ✅ |
| 10 | `/api/v2/auth/users/{id}` | GET | ✅ `/api/v2/auth/users/{id}` | ✅ |
| 11 | `/api/v2/auth/users/{id}` | PATCH | ✅ `/api/v2/auth/users/{id}` | ✅ |
| 12 | `/api/v2/auth/users/{id}/scopes` | GET | ✅ `/api/v2/auth/users/{id}/scopes` | ✅ |
| 13 | `/api/v2/auth/users/{id}/scopes` | PUT | ✅ `/api/v2/auth/users/{id}/scopes` | ✅ |
| 14 | `/api/v2/auth/registrations` | GET | ✅ `/api/v2/auth/registrations` | ✅ |
| 15 | `/api/v2/auth/registrations/{id}` | GET | ✅ `/api/v2/auth/registrations/{id}` | ✅ |
| 16 | `/api/v2/auth/registrations/{id}/approve` | POST | ✅ `/api/v2/auth/registrations/{id}/approve` | ✅ |
| 17 | `/api/v2/auth/registrations/{id}/reject` | POST | ✅ `/api/v2/auth/registrations/{id}/reject` | ✅ |
| 18 | `/api/v2/auth/registrations/count` | GET | ✅ `/api/v2/auth/registrations/count` | ✅ |
| 19 | `/api/v2/auth/scope-options` | GET | ✅ `/api/v2/auth/scope-options` | ✅ |

## 权限服务 (auth-service)

| # | Python 端点 | HTTP Method | Java 路径 | 验证状态 |
|---|------------|-------------|-----------|---------|
| 20 | `/api/v2/permission/projects/{id}/members` | GET | ✅ `/api/v2/permission/projects/{id}/members` | ✅ |
| 21 | `/api/v2/permission/projects/{id}/members` | POST | ✅ `/api/v2/permission/projects/{id}/members` | ✅ |
| 22 | `/api/v2/permission/projects/{id}/members/{uid}` | DELETE | ✅ `/api/v2/permission/projects/{id}/members/{uid}` | ✅ |
| 23 | `/api/v2/permission/projects/{id}/members/{uid}/roles` | PUT | ✅ `/api/v2/permission/projects/{id}/members/{uid}/roles` | ✅ |
| 24 | `/api/v2/permission/projects/{id}/context` | GET | ✅ `/api/v2/permission/projects/{id}/context` | ✅ |
| 25 | `/api/v2/permission/projects/{id}/baselines` | POST | ✅ `/api/v2/permission/projects/{id}/baselines` | ✅ |
| 26 | `/api/v2/permission/projects/{id}/baselines` | GET | ✅ `/api/v2/permission/projects/{id}/baselines` | ✅ |
| 27 | `/api/v2/permission/projects/{id}/baselines/{bid}/lock` | POST | ✅ `/api/v2/permission/projects/{id}/baselines/{bid}/lock` | ✅ |
| 28 | `/api/v2/permission/change-requests` | POST | ✅ `/api/v2/permission/change-requests` | ✅ |
| 29 | `/api/v2/permission/change-requests/pending` | GET | ✅ `/api/v2/permission/change-requests/pending` | ✅ |
| 30 | `/api/v2/permission/change-requests/{id}/approve` | POST | ✅ `/api/v2/permission/change-requests/{id}/approve` | ✅ |
| 31 | `/api/v2/permission/change-requests/{id}/reject` | POST | ✅ `/api/v2/permission/change-requests/{id}/reject` | ✅ |
| 32 | `/api/v2/permission/reviews` | POST | ✅ `/api/v2/permission/reviews` | ✅ |
| 33 | `/api/v2/permission/reviews/pending` | GET | ✅ `/api/v2/permission/reviews/pending` | ✅ |
| 34 | `/api/v2/permission/reviews/{id}/approve` | POST | ✅ `/api/v2/permission/reviews/{id}/approve` | ✅ |
| 35 | `/api/v2/permission/reviews/{id}/reject` | POST | ✅ `/api/v2/permission/reviews/{id}/reject` | ✅ |
| 36 | `/api/v2/permission/reviews/{id}/withdraw` | POST | ✅ `/api/v2/permission/reviews/{id}/withdraw` | ✅ |

## 项目管理服务 (project-service)

| # | Python 端点 | HTTP Method | Java 路径 | 验证状态 |
|---|------------|-------------|-----------|---------|
| 37 | `/api/v2/manage/projects` | POST | ✅ `/api/v2/manage/projects` | ✅ |
| 38 | `/api/v2/manage/projects` | GET | ✅ `/api/v2/manage/projects` | ✅ |
| 39 | `/api/v2/manage/projects/{id}` | GET | ✅ `/api/v2/manage/projects/{id}` | ✅ |
| 40 | `/api/v2/manage/projects/{id}` | PATCH | ✅ `/api/v2/manage/projects/{id}` | ✅ |
| 41 | `/api/v2/manage/projects/{id}` | DELETE | ✅ `/api/v2/manage/projects/{id}` | ✅ |
| 42 | `/api/v2/manage/projects/{id}/milestones` | POST | ✅ `/api/v2/manage/projects/{id}/milestones` | ✅ |
| 43 | `/api/v2/manage/projects/{id}/branches` | POST | ✅ `/api/v2/manage/projects/{id}/branches` | ✅ |
| 44 | `/api/v2/product/products` | POST | ✅ `/api/v2/product/products` | ✅ |
| 45 | `/api/v2/product/products` | GET | ✅ `/api/v2/product/products` | ✅ |
| 46 | `/api/v2/product/products/{id}` | GET | ✅ `/api/v2/product/products/{id}` | ✅ |
| 47 | `/api/v2/product/products/{id}` | PATCH | ✅ `/api/v2/product/products/{id}` | ✅ |

## 需求管理服务 (requirement-service)

| # | Python 端点 | HTTP Method | Java 路径 | 验证状态 |
|---|------------|-------------|-----------|---------|
| 48 | `/api/v2/requirements/projects/{id}/requirements` | GET | ✅ | ✅ |
| 49 | `/api/v2/requirements/projects/{id}/requirements` | POST | ✅ | ✅ |
| 50 | `/api/v2/requirements/requirements/{id}` | GET | ✅ | ✅ |
| 51 | `/api/v2/requirements/requirements/{id}` | PATCH | ✅ | ✅ |
| 52 | `/api/v2/requirements/requirements/bulk-status` | PATCH | ✅ | ✅ |
| 53 | `/api/v2/requirements/projects/{id}/requirements/import` | POST | ✅ | ✅ |
| 54 | `/api/v2/requirements/test-cases` | CRUD | ✅ | ✅ |
| 55 | `/api/v2/requirements/defects` | CRUD | ✅ | ✅ |

## 需求分析服务 (requirement-analysis-service)

| # | Python 端点 | HTTP Method | Java 路径 | 验证状态 |
|---|------------|-------------|-----------|---------|
| 56 | `/api/v2/classification/**` | * | ✅ | ✅ |
| 57 | `/api/v2/dedup/**` | * | ✅ | ✅ |
| 58 | `/api/v2/actor/**` | * | ✅ | ✅ |
| 59 | `/api/v2/analysis/**` | * | ✅ | ✅ |
| 60 | `/api/v2/requirement-graph/**` | * | ✅ | ✅ |
| 61 | `/api/v2/requirement-change/**` | * | ✅ | ✅ |

## 推理服务 (inference-service)

| # | Python 端点 | HTTP Method | Java 路径 | 验证状态 |
|---|------------|-------------|-----------|---------|
| 62 | `/api/v2/inference/chat/**` | * | ✅ | ✅ |
| 63 | `/api/v2/inference/classification/**` | * | ✅ | ✅ |
| 64 | `/api/v2/inference/conflict/**` | * | ✅ | ✅ |
| 65 | `/api/v2/inference/traceability/**` | * | ✅ | ✅ |
| 66 | `/api/v2/inference/kb/**` | * | ✅ | ✅ |
| 67 | `/api/v2/inference/l4/**` | * | ✅ | ✅ |
| 68 | `/api/v2/inference/acquisition/**` | * | ✅ | ✅ |
| 69 | `/inference/**` (v1 compat) | * | ✅ | ✅ |

## 前端对接说明

1. **API 基地址**: 将前端 `.env` 中的 `VITE_API_BASE_URL` 指向 Gateway 地址
   ```
   VITE_API_BASE_URL=http://localhost:25698
   ```

2. **认证令牌**: 登录后从 `response.token` 获取 JWT，后续请求通过 `Authorization: Bearer <token>` 传递

3. **响应格式**: 所有 API 响应使用 snake_case 键名（与原 Python 版本一致）

4. **错误格式**: 错误响应格式为 `{"detail": "error message"}`

5. **Gateway 路由**: 所有请求通过 Gateway 统一入口；开发环境默认 `http://localhost:25698`

## 验证方法

1. 启动所有服务: `docker-compose up` 或逐服务启动
2. 确认 Eureka 控制台 (http://localhost:25701) 显示所有服务已注册
3. 通过 Gateway 发送请求验证路由正常工作
4. 运行 API 兼容性测试: `mvn test -pl services/api-compatibility-tests`
