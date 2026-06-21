# Cloudys 故障排除指南

## 目录
1. [Pod 启动问题](#pod-启动问题)
2. [Eureka 注册问题](#eureka-注册问题)
3. [Gateway 路由问题](#gateway-路由问题)
4. [数据库连接问题](#数据库连接问题)
5. [JWT 认证问题](#jwt-认证问题)
6. [Feign 客户端调用问题](#feign-客户端调用问题)
7. [镜像构建推送问题](#镜像构建推送问题)

---

## Pod 启动问题

### 症状: Pod 一直处于 Pending 状态

```bash
kubectl describe pod <pod-name> -n cloudys
```

**常见原因:**
- 资源不足: 检查 `kubectl top nodes`
- PVC 未绑定: 检查 `kubectl get pvc -n cloudys`
- NFS 不可达: 检查节点是否能访问 `NFS_SERVER`，以及导出目录权限是否正确
- 镜像拉取失败: 检查 `imagePullSecrets` 和镜像仓库网络

### 症状: Pod 频繁重启 (CrashLoopBackOff)

```bash
kubectl logs <pod-name> -n cloudys --previous
```

**常见原因:**
- 数据库连接失败: 检查 `postgres-secret` 和 PostgreSQL Pod 状态
- JWT 密钥格式错误: 密钥长度需 ≥ 32 字符 (HS256 要求)
- Eureka 连接超时: 确认 Eureka Service DNS 可解析

### 症状: Probe 失败

```bash
# 检查就绪探针
kubectl exec -it <pod-name> -n cloudys -- wget -qO- http://localhost:<port>/actuator/health/readiness

# 检查存活探针
kubectl exec -it <pod-name> -n cloudys -- wget -qO- http://localhost:<port>/actuator/health/liveness
```

---

## Eureka 注册问题

### 症状: 服务未在 Eureka 控制台显示

1. **检查 Eureka Pod 是否正常运行:**
   ```bash
   kubectl get pods -n cloudys -l app=eureka-service
   ```

2. **检查服务配置:**
   确认 `application-k8s.yml` 中 Eureka URL 正确:
   ```yaml
   eureka:
     client:
       service-url:
         defaultZone: http://eureka-service.cloudys.svc.cluster.local:8888/eureka
   ```

3. **检查服务日志:**
   ```bash
   kubectl logs -n cloudys <service-pod> | grep -i eureka
   ```

4. **验证 DNS 解析:**
   ```bash
   kubectl run -it --rm debug --image=busybox --restart=Never -- nslookup eureka-service.cloudys.svc.cluster.local
   ```

### 症状: 自我保护模式

Eureka 控制台显示红色警告 "RENEWALS ARE LESSER THAN THRESHOLD":
- 开发环境正常 (禁用自我保护): `EUREKA_SELF_PRESERVATION=false`
- 生产环境: 这是 Eureka 的正常保护机制，不代表服务故障

---

## Gateway 路由问题

### 症状: 请求返回 503 Service Unavailable

```bash
# 检查 Gateway 日志
kubectl logs -n cloudys -l app=gateway-service | tail -50
```

**原因分析:**
1. 目标服务未注册到 Eureka: 检查 Eureka 控制台
2. 服务名不匹配: Gateway 配置中的 `lb://service-name` 必须与目标服务 `spring.application.name` 一致
3. LoadBalancer 无可用实例: 确认目标服务至少有 1 个健康实例

### 症状: 请求返回 401 Unauthorized

**原因分析:**
1. 未携带 Authorization header: 确认客户端在请求中包含 `Authorization: Bearer <token>`
2. Token 已过期: JWT 默认 24 小时有效
3. JWT 密钥不一致: Gateway 和 auth-service 的 `jwt.secret-key` 必须相同
4. 公开路径: `/health`, `/api/v2/auth/login|register|logout` 不需要 token

### 症状: 跨域请求被拦截

Gateway 默认允许所有来源 (`*`)。如需限制:
```yaml
# application.yml
spring.cloud.gateway.globalcors.cors-configurations.[/**].allowedOrigins: "https://your-frontend.com"
```

---

## 数据库连接问题

### 症状: `Connection refused` 或 `timeout`

1. **检查 PostgreSQL Pod:**
   ```bash
   kubectl get pods -n cloudys -l app=postgresql
   ```

2. **检查 Secret 是否正确:**
   ```bash
   kubectl get secret postgres-secret -n cloudys -o yaml
   ```
   `POSTGRES_USER`、`POSTGRES_PASSWORD`、`POSTGRES_DB` 必须与连接字符串一致。

3. **检查 NFS PV/PVC:**
   ```bash
   kubectl get pv postgres-pv
   kubectl get pvc postgres-pvc -n cloudys
   ```
   若 PVC 长时间 Pending，重点检查:
   - `NFS_SERVER` 是否可达
   - `NFS_PATH` 是否已导出
   - 节点是否安装了 NFS client

3. **测试数据库连接:**
   ```bash
   kubectl run -it --rm debug --image=postgres:16-alpine --restart=Never -- \
     psql -h postgresql.cloudys.svc.cluster.local -U cloudys -d cloudys
   ```

### 症状: Flyway 迁移失败

```bash
kubectl logs -n cloudys <service-pod> | grep -i flyway
```

- 确认 `spring.flyway.enabled: true`
- 检查迁移文件是否在 `src/main/resources/db/migration/` 目录
- 手动修复: 连接数据库，检查 `flyway_schema_history` 表

### 症状: Dashboard 无法登录

```bash
kubectl get pods -n kubernetes-dashboard
kubectl apply -f deploy/k8s/11-dashboard-admin.yml
kubectl -n kubernetes-dashboard create token dashboard-admin
```

若 token 已生成但仍无法访问:
- 优先使用 `kubectl proxy`
- 检查浏览器访问路径是否正确
- 检查 Dashboard 服务是否已存在

---

## JWT 认证问题

### 症状: Token 验证失败

**检查清单:**
1. JWT 密钥长度 ≥ 32 字符 (HS256 要求 256 位)
2. 所有服务使用相同的 `jwt.secret-key`
3. Token 格式: `Authorization: Bearer <base64_encoded_token>`
4. Token 载荷包含: `user_id`, `username`, `role`, `iat`, `exp`

### 症状: 前端登录成功但后续请求 401

**可能原因:**
- 前端未正确存储/传递 token
- Token 前缀错误: 必须是 `Bearer <token>` (注意空格)
- 不同环境密钥不一致: 检查 application-dev.yml vs application-k8s.yml

---

## Feign 客户端调用问题

### 症状: `FeignException$ServiceUnavailable`

1. **检查服务注册状态:**
   ```bash
   curl http://<eureka-pod>:8888/eureka/apps
   ```

2. **验证 Feign 客户端配置:**
   ```java
   @FeignClient(name = "requirement-service", path = "/api/v2/requirements")
   ```
   确保 `name` 与目标服务 `spring.application.name` 匹配。

3. **检查是否需要 JWT 透传:**
   Feign 客户端需配置 `FeignSecurityConfig` 传递当前请求的 JWT 到下游服务。

### 症状: Fallback 被执行但目标服务正常

- 检查 CircuitBreaker 配置 (`resilience4j.circuitbreaker`)
- 检查超时配置 (`spring.cloud.openfeign.client.config.*.connectTimeout`)
- 确认目标服务响应时间未超过阈值

---

## 镜像构建推送问题

### 症状: `nerdctl build` 失败

1. **确保 Maven 已构建 JAR:**
   ```bash
   mvn clean package -DskipTests
   ls services/auth-service/target/*.jar
   ```

2. **检查 Dockerfile 路径:**
   ```bash
   nerdctl build -f deploy/docker/Dockerfile.auth .
   ```

### 症状: `nerdctl push` 认证失败

```bash
nerdctl login registry.cn-hangzhou.aliyuncs.com --username=<your-username>
```

或在 Jenkins 中配置凭据。

### 症状: Python sidecar 构建失败

Python sidecar 构建使用仓库根作为上下文，并从 `services/python-sidecar/` 读取源码与 `uv.lock`:
```bash
nerdctl build -f deploy/docker/Dockerfile.python-sidecar .
```
