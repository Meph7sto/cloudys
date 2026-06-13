# Cloudys 部署指南

## 概述

Cloudys 是 Semantic-Atlas 需求工程语义分析平台的 Java Spring Boot 3 + Spring Cloud 微服务版本。
本文档涵盖从源码构建到 Kubernetes 集群部署的完整流程。

## 环境要求

### 开发环境
- JDK 17+
- Maven 3.9+
- Docker / nerdctl (containerd)
- PostgreSQL 16 (或 Docker Compose)

### Kubernetes 集群 (生产环境)
- 2-3 节点 Ubuntu 22.04 LTS
- containerd + kubeadm 1.28+
- Calico CNI
- Kubernetes Dashboard (可选)

## 服务清单

| 服务 | 端口 | 副本(生产) | 数据库 | 说明 |
|------|------|-----------|--------|------|
| eureka-service | 8888 | 1 | 无 | 服务注册与发现 |
| gateway-service | 8008 | 1 | 无 | API 网关 (NodePort 30008) |
| auth-service | 8001 | 2 | cloudys | 认证与权限 |
| project-service | 8002 | 2 | cloudys | 项目管理 |
| requirement-service | 8003 | 2 | cloudys | 需求管理 |
| requirement-analysis-service | 8004 | 2 | cloudys | 需求分析 |
| inference-service | 8006 | 1 | 无 | 推理服务 (含 Python sidecar) |
| postgresql | 5432 | 1 | cloudys | 数据库 |

## 快速开始 (Docker Compose 开发模式)

### 1. 准备环境变量

```bash
cp .env.example .env
# 编辑 .env 设置数据库密码、JWT 密钥、HOST_*_PORT
```

### 2. 构建项目

```bash
# 编译所有模块
mvn clean package -DskipTests
```

### 3. 启动服务

```bash
# Windows 开发环境（推荐）
./start-all.ps1

# 等价的 Docker Compose 命令
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d --build
```

### 4. 验证

```bash
# 检查 Eureka 控制台
curl http://localhost:25701

# 检查 Gateway 健康状态
curl http://localhost:25698/actuator/health/liveness

# 测试登录接口
curl -X POST http://localhost:25698/api/v2/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"Admin123!","display_name":"Admin","role":"super_admin"}'
```

### 虚拟机部署建议

虚拟机场景推荐使用：

```bash
docker compose -f docker-compose.yml -f docker-compose.vm.yml up -d --build
```

默认仅暴露：

- `Gateway`: `0.0.0.0:25698 -> 8008`
- `PostgreSQL`: `127.0.0.1:25700 -> 5432`

调试时可启用 `debug` profile 暴露 Eureka 和 Adminer：

```bash
docker compose -f docker-compose.yml -f docker-compose.vm.yml -f docker-compose.vm.debug.yml up -d
```

## Kubernetes 部署

### 前置准备

1. **搭建 Kubernetes 集群** (参考 6.1 节)
2. **安装 kubectl** 并配置 kubeconfig
3. **配置镜像仓库** (阿里云 ACR 或其他)

### 构建镜像

```bash
# 编译项目
mvn clean package -DskipTests

# 运行构建推送脚本
cd deploy/scripts
./build-and-push.sh
```

脚本默认推送到 `registry.cn-hangzhou.aliyuncs.com/cloudys`，如需修改请编辑脚本中的 `REGISTRY` 变量。

### 部署到 Kubernetes

```bash
# 使用部署脚本 (推荐)
cd deploy/scripts
./deploy-k8s.sh

# 或手动部署
kubectl apply -f deploy/k8s/00-namespace.yml
kubectl apply -f deploy/k8s/10-jwt-secret.yml
kubectl apply -f deploy/k8s/98-deepseek-secret.yml
kubectl apply -f deploy/k8s/01-postgres-*.yml
kubectl apply -f deploy/k8s/02-eureka-service.yml
kubectl apply -f deploy/k8s/04-auth-service.yml
kubectl apply -f deploy/k8s/05-project-service.yml
kubectl apply -f deploy/k8s/06-requirement-service.yml
kubectl apply -f deploy/k8s/07-requirement-analysis-service.yml
kubectl apply -f deploy/k8s/09-inference-service.yml
kubectl apply -f deploy/k8s/03-gateway-service.yml  # 最后部署 Gateway
kubectl apply -f deploy/k8s/08-ingress.yml
```

### 部署验证

```bash
# 检查所有 Pod 运行状态
kubectl get pods -n semantic-atlas

# 检查 Service
kubectl get svc -n semantic-atlas

# 检查 Eureka 注册 (port-forward)
kubectl port-forward -n semantic-atlas svc/eureka-service 8888:8888 &
curl http://localhost:8888/eureka/apps

# 通过 Gateway 访问
kubectl port-forward -n semantic-atlas svc/gateway-service 8008:8008 &
curl http://localhost:8008/health
```

### 镜像仓库配置

镜像使用 `${REGISTRY}${TAG}` 变量替换。部署前设置:

```bash
export REGISTRY=registry.cn-hangzhou.aliyuncs.com/cloudys
export TAG=v1.0.0
```

或通过 Jenkins Pipeline 自动设置 (见 Jenkinsfile)。

### Secret 管理

生产环境需配置以下 Secret:

| Secret | 用途 | 文件 |
|--------|------|------|
| postgres-secret | PostgreSQL 用户名/密码 | `01-postgres-secret.yml` |
| jwt-secret | JWT 签名密钥 | `10-jwt-secret.yml` |
| deepseek-secret | DeepSeek API Key (可选) | `98-deepseek-secret.yml` |

**部署前务必修改默认密码和密钥！**

### 扩容

```bash
# 将 auth-service 扩展到 3 副本
kubectl scale deployment auth-service -n semantic-atlas --replicas=3

# 将 project-service 扩展到 3 副本 (验证负载均衡)
kubectl scale deployment project-service -n semantic-atlas --replicas=3
```

### 滚动更新

```bash
# 更新镜像后触发滚动更新
kubectl set image deployment/auth-service auth-service=${REGISTRY}/auth-service:${TAG} -n semantic-atlas

# 或重启所有业务服务
kubectl rollout restart deployment -n semantic-atlas --selector=app!=postgresql
```

### 回滚

```bash
# 查看历史版本
kubectl rollout history deployment/auth-service -n semantic-atlas

# 回滚到上一个版本
kubectl rollout undo deployment/auth-service -n semantic-atlas
```

## CI/CD Pipeline (Jenkins)

Jenkinsfile 包含 7 个阶段:
1. **Checkout** — 拉取代码
2. **Build** — `mvn clean package -DskipTests`
3. **Test** — `mvn test` (采集 JUnit 报告)
4. **BuildImages** — nerdctl 构建 8 个镜像
5. **PushImages** — 推送到远程仓库
6. **Deploy** — kubectl apply (有序部署)
7. **Verify** — 验证节点/Pod/Service/Eureka

使用前需在 Jenkins 中配置:
- `REGISTRY_USERNAME` / `REGISTRY_PASSWORD` 凭据
- `kubeconfig` 凭据 (Kubernetes 集群访问)

## 监控

每个微服务暴露 Actuator 端点:
- `/actuator/health` — 整体健康状态
- `/actuator/health/liveness` — 存活探测
- `/actuator/health/readiness` — 就绪探测
- `/actuator/info` — 应用信息

可集成 Prometheus + Grafana 进行指标采集和可视化 (部署文件见 `deploy/monitoring/`)。
