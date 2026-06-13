# Cloudys 生产环境部署检查清单

## 部署前检查

### 代码与测试
- [ ] 所有单元测试通过: `mvn test`
- [ ] 所有集成测试通过: `mvn test -pl services/full-link-tests`
- [ ] API 兼容性测试通过: `mvn test -pl services/api-compatibility-tests`
- [ ] Gateway 路由测试通过: `mvn test -pl services/gateway-service`
- [ ] 代码已通过 code review

### 镜像
- [ ] 所有 8 个镜像已构建: eureka, gateway, auth, project, requirement, analysis, inference, python-sidecar
- [ ] 镜像已发布到远程仓库
- [ ] 镜像标签正确 (使用语义化版本，如 v1.0.0)
- [ ] Dockerfile 使用 `eclipse-temurin:17-jre-alpine` 作为基础镜像

### 配置
- [ ] JWT 密钥已替换为生产密钥 (非默认值)
- [ ] PostgreSQL 密码已替换为强密码
- [ ] DeepSeek API Key 已配置 (如需 LLM 功能)
- [ ] CORS 来源已限制为实际前端域名
- [ ] 所有 Secret 已正确配置

### Kubernetes 资源
- [ ] Namespace `semantic-atlas` 已创建
- [ ] PV/PVC 已配置且存储充足 (建议 ≥10Gi)
- [ ] PostgreSQL StatefulSet 已配置
- [ ] Eureka 注册中心已部署
- [ ] 所有微服务 Deployment 已配置
- [ ] Gateway 已配置 (含 NodePort 或 Ingress)
- [ ] 健康检查探针已配置 (liveness + readiness)

---

## 安全清单

- [ ] **JWT 密钥**: 长度 ≥ 32 字符, 高强度随机字符串
- [ ] **数据库密码**: 非默认值, 长度 ≥ 12 字符
- [ ] **CORS**: 已限制为生产前端域名 (非 `*`)
- [ ] **无硬编码密钥**: 所有密钥来自 Secret 或环境变量
- [ ] **Gateway JWT 校验**: 已启用, 公开路径已确认
- [ ] **HTTPS**: 生产环境使用 TLS (Ingress 或外部 LB)
- [ ] **审计日志**: 关键操作 (登录/权限变更) 有日志记录
- [ ] **速率限制**: Gateway 已配置限流 (按需)

---

## 性能清单

- [ ] **资源限制**: 所有容器已设置 CPU/内存 requests 和 limits
- [ ] **副本数**: 关键服务 (auth/project/requirement/analysis) ≥ 2 副本
- [ ] **数据库连接池**: HikariCP 已配置最大连接数
- [ ] **JVM 参数**: 已设置适当的堆内存大小
- [ ] **负载均衡**: 已通过多副本验证请求分发
- [ ] **熔断降级**: Feign 客户端已配置 fallback

---

## 监控清单

- [ ] **健康检查**: 所有服务的 `/actuator/health` 可访问
- [ ] **存活探针**: `/actuator/health/liveness` 配置正确
- [ ] **就绪探针**: `/actuator/health/readiness` 配置正确
- [ ] **Eureka 控制台**: 所有服务已注册
- [ ] **Kubernetes Dashboard**: 已部署并可访问

---

## 备份策略

- [ ] **数据库备份**: PostgreSQL 定期备份 (推荐每日)
- [ ] **配置备份**: Kubernetes 资源 YAML 已版本控制
- [ ] **密钥备份**: Secret 值安全存储在密钥管理系统中

### PostgreSQL 备份示例

```bash
# 创建备份
kubectl exec -n semantic-atlas postgresql-0 -- \
  pg_dump -U cloudys cloudys > backup_$(date +%Y%m%d).sql

# 恢复备份
kubectl exec -i -n semantic-atlas postgresql-0 -- \
  psql -U cloudys cloudys < backup_20260612.sql
```

---

## 回滚计划

1. **记录当前部署版本:**
   ```bash
   kubectl get deployments -n semantic-atlas -o wide
   ```

2. **保存当前配置快照:**
   ```bash
   kubectl get all -n semantic-atlas -o yaml > pre-deploy-snapshot.yaml
   ```

3. **回滚步骤:**
   ```bash
   # 回滚单个服务
   kubectl rollout undo deployment/auth-service -n semantic-atlas

   # 或重新 apply 旧版本 YAML
   kubectl apply -f deploy/k8s/ --recursive
   ```

---

## 发布后验证

- [ ] Eureka 控制台显示所有服务 UP
- [ ] Gateway 健康检查通过: `curl http://<host>:8008/actuator/health/liveness`
- [ ] 登录接口正常: `POST /api/v2/auth/login`
- [ ] 业务接口正常: `GET /api/v2/manage/projects`
- [ ] Kubernetes Pod 全部 Running: `kubectl get pods -n semantic-atlas`
- [ ] 无异常日志: 检查各服务日志
- [ ] 前端应用可正常访问和操作
