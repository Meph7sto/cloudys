# Cloudys 逐项验收清单

本文档只列与当前仓库直接相关、并且现场验收时需要准备证据的事项。

## A. 代码与工程结构

- [x] Maven 聚合项目，父项目 `packaging` 为 `pom`
- [x] 父 POM 统一管理模块、依赖、插件与测试配置
- [x] 至少包含 1 个用户管理微服务: `auth-service`
- [x] 至少包含 2 个业务微服务: `project-service`、`requirement-service`、`requirement-analysis-service`
- [x] 各微服务有独立启动类、配置文件、POM
- [x] 已提供前端项目与后端联调入口

代码证据:
- [pom.xml](/W:/cloudyuansheng/cloudys/pom.xml:17)
- [pom.xml](/W:/cloudyuansheng/cloudys/pom.xml:21)

## B. 登录鉴权

- [x] `auth-service` 提供注册、登录、个人信息接口
- [x] 登录成功返回 JWT
- [x] 受保护接口要求认证
- [x] Gateway 对 `/api/**` 统一校验 JWT
- [x] 无 token / 非法 token 返回 401

代码证据:
- [AuthController.java](/W:/cloudyuansheng/cloudys/services/auth-service/src/main/java/com/cloudys/auth/controller/AuthController.java:37)
- [AuthService.java](/W:/cloudyuansheng/cloudys/services/auth-service/src/main/java/com/cloudys/auth/service/AuthService.java:58)
- [AuthSecurityConfig.java](/W:/cloudyuansheng/cloudys/services/auth-service/src/main/java/com/cloudys/auth/config/AuthSecurityConfig.java:39)
- [JwtGatewayFilter.java](/W:/cloudyuansheng/cloudys/services/gateway-service/src/main/java/com/cloudys/gateway/filter/JwtGatewayFilter.java:52)

现场证据:
- [ ] 演示登录获取 token
- [ ] 演示不带 token 访问业务接口返回 401
- [ ] 演示带 token 访问业务接口成功

## C. 服务注册、调用、负载均衡、熔断

- [x] 已实现 Eureka 注册中心
- [x] 微服务启用服务发现
- [x] 已实现 OpenFeign 调用
- [x] 已启用 Feign circuit breaker
- [x] 已实现多个 fallback
- [x] K8s 部署文件中关键服务有多副本配置，可用于演示负载均衡

代码证据:
- [EurekaServiceApplication.java](/W:/cloudyuansheng/cloudys/services/eureka-service/src/main/java/com/cloudys/eureka/EurekaServiceApplication.java:8)
- [RequirementAnalysisServiceApplication.java](/W:/cloudyuansheng/cloudys/services/requirement-analysis-service/src/main/java/com/cloudys/requirementanalysis/RequirementAnalysisServiceApplication.java:11)
- [RequirementServiceClient.java](/W:/cloudyuansheng/cloudys/services/project-service/src/main/java/com/cloudys/project/client/RequirementServiceClient.java:14)
- [RequirementServiceClientFallback.java](/W:/cloudyuansheng/cloudys/services/project-service/src/main/java/com/cloudys/project/client/RequirementServiceClientFallback.java:7)
- [04-auth-service.yml](/W:/cloudyuansheng/cloudys/deploy/k8s/04-auth-service.yml:28)

现场证据:
- [ ] Eureka 页面展示服务实例
- [ ] 演示一个服务调用另一个服务
- [ ] 演示多副本请求分发
- [ ] 演示停掉下游服务后的 fallback

## D. 数据库与持久化

- [x] 使用 PostgreSQL
- [x] 各服务使用 JPA/Flyway
- [x] 已提供 PostgreSQL Secret / ConfigMap / PVC / StatefulSet / Service
- [x] 已改为 NFS PV 方案，更贴近验收文档要求

代码证据:
- [01-postgres-pv-pvc.yml](/W:/cloudyuansheng/cloudys/deploy/k8s/01-postgres-pv-pvc.yml:1)
- [01-postgres-statefulset.yml](/W:/cloudyuansheng/cloudys/deploy/k8s/01-postgres-statefulset.yml:23)
- [application-k8s.yml](/W:/cloudyuansheng/cloudys/services/auth-service/src/main/resources/application-k8s.yml:6)

现场证据:
- [ ] 展示 PVC Bound
- [ ] 展示 PostgreSQL Pod Running
- [ ] 演示服务能查询数据库数据
- [ ] 演示数据库数据重启后仍保留

## E. Kubernetes 编排

- [x] 已统一使用 `cloudys` 命名空间
- [x] 已提供 Namespace、Deployment、Service、Ingress、Secret、ConfigMap、PV/PVC、StatefulSet
- [x] 已配置 readiness/liveness probes
- [x] 已配置资源 requests/limits

代码证据:
- [00-namespace.yml](/W:/cloudyuansheng/cloudys/deploy/k8s/00-namespace.yml:5)
- [03-gateway-service.yml](/W:/cloudyuansheng/cloudys/deploy/k8s/03-gateway-service.yml:13)
- [03-gateway-service.yml](/W:/cloudyuansheng/cloudys/deploy/k8s/03-gateway-service.yml:47)

现场证据:
- [ ] `kubectl get nodes`
- [ ] `kubectl get pods,svc,deploy -n cloudys`
- [ ] 网关 NodePort/Ingress 实际可访问

## F. 镜像与 CI/CD

- [x] 已提供各服务 Dockerfile
- [x] 已提供批量构建推送脚本
- [x] 已提供 Jenkinsfile，包含 Build/Test/BuildImages/PushImages/Deploy/Verify

代码证据:
- [Jenkinsfile](/W:/cloudyuansheng/cloudys/deploy/jenkins/Jenkinsfile:18)
- [build-and-push.sh](/W:/cloudyuansheng/cloudys/deploy/scripts/build-and-push.sh:1)

现场证据:
- [ ] 镜像仓库中存在已推送镜像
- [ ] Jenkins 任务可执行
- [ ] Jenkins 流水线成功
- [ ] 部署更新后 Pod 完成滚动更新

## G. Dashboard 与可观测

- [x] 已补充 Dashboard 管理员账号资源
- [x] 已配置 actuator 健康检查
- [x] 已提供基础监控目录

代码证据:
- [11-dashboard-admin.yml](/W:/cloudyuansheng/cloudys/deploy/k8s/11-dashboard-admin.yml:1)
- [03-gateway-service.yml](/W:/cloudyuansheng/cloudys/deploy/k8s/03-gateway-service.yml:53)

现场证据:
- [ ] Dashboard 已安装
- [ ] `dashboard-admin` token 可生成
- [ ] 可登录 Dashboard 查看节点/Pod/Service/Deployment

## H. 已完成本地验证

- [x] `mvn -pl services/gateway-service test`
- [x] `mvn -pl services/auth-service test`
- [x] `mvn -pl services/project-service test`

## I. 当前剩余必须准备的现场材料

- [ ] Kubernetes 节点 Ready 截图/命令输出
- [ ] Pod/Service/Deployment Running 截图/命令输出
- [ ] Eureka 页面截图
- [ ] 网关鉴权成功/失败截图
- [ ] Jenkins 流水线成功截图
- [ ] 镜像仓库截图
- [ ] Dashboard 登录与资源截图
