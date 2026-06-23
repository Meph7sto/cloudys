# Cloudys 三节点虚拟机 Kubernetes 重新部署 Runbook

> 适用场景：课程验收 / 离线或半离线虚拟机环境 / containerd 本地镜像部署。本文档整理自一次真实的 `cloudys` 三节点 Kubernetes 重新部署过程，目标是让后续复部署、排错、截图验收有一套可执行流程。

## 1. 环境与目标

### 1.1 集群环境

本次部署使用三台 Ubuntu 22.04 虚拟机：

- `master`：`192.168.200.140`
- `worker1`：`192.168.200.141`
- `worker2`：`192.168.200.142`

集群组件：

- Kubernetes：`v1.28.x`
- 容器运行时：`containerd`
- CNI：Calico
- 镜像方式：不依赖远程仓库，使用本地 Docker 构建后导出 tar，再导入各节点 `containerd` 的 `k8s.io` namespace。

### 1.2 Cloudys 组件

`cloudys` 命名空间中应部署：

- PostgreSQL：`postgresql-0`
- Eureka：`eureka-service`
- Gateway：`gateway-service`
- 认证服务：`auth-service`
- 项目管理服务：`project-service`
- 需求管理服务：`requirement-service`
- 需求分析服务：`requirement-analysis-service`
- 推理服务：`inference-service`，包含 Java 容器和 `python-sidecar`
- 前端：`frontend-service`

补充验收组件：

- Kubernetes Dashboard
- Jenkins

## 2. 部署前检查

### 2.1 检查节点与资源

```bash
kubectl get nodes -o wide
kubectl get pods -A
kubectl get pods,svc,deploy,statefulset,pvc -n cloudys -o wide
```

重点检查：

- 三个节点是否 `Ready`。
- 是否存在 `DiskPressure` / `MemoryPressure`。
- `cloudys` 命名空间中是否有旧的 `Error` / `Evicted` / `Pending` Pod。

查看 master taint 与磁盘压力：

```bash
kubectl describe node master | grep -E '^Taints:|DiskPressure' -A4
```

查看磁盘：

```bash
df -h / /home /var/lib/containerd /var/lib/docker 2>/dev/null || df -h
df -ih
```

### 2.2 清理旧失败 Pod 与磁盘压力

如果存在大量 `Error` / `Evicted` Pod，先清理：

```bash
kubectl delete pod -n cloudys --field-selector=status.phase=Failed --ignore-not-found=true
```

清理日志、包缓存、无用镜像：

```bash
sudo journalctl --vacuum-time=1d || true
sudo apt-get clean || true
sudo crictl rmi --prune || true
sudo docker system prune -af || true
```

如果 master 上保留过旧镜像 tar，也要清理：

```bash
rm -f /home/master/cloudys-image-tars/*.tar
```

清理后再次确认：

```bash
df -h /
kubectl describe node master | grep -E '^Taints:|DiskPressure' -A4
```

> 经验：`DiskPressure` 解除可能有延迟，磁盘降下去后 kubelet 需要一小段时间才会去掉 taint。

## 3. 上传源码与 Maven 构建

### 3.1 上传源码

从本机将 `W:\cloudyuansheng\cloudys` 打包上传到 master。建议排除 `.git`、`target`、`node_modules`、`.venv`、运行日志等目录，避免归档过大。

示例：

```bash
tar \
  --exclude='.git' \
  --exclude='**/target' \
  --exclude='**/node_modules' \
  --exclude='.run' \
  --exclude='services/python-sidecar/.venv' \
  -C /mnt/w/cloudyuansheng \
  -czf /mnt/w/hermes/tmp/cloudys-src.tar.gz cloudys
```

上传到 master 后解压：

```bash
cd /home/master
mv cloudys "cloudys.backup.$(date +%Y%m%d-%H%M%S)" 2>/dev/null || true
tar -xzf /home/master/cloudys-src.tar.gz -C /home/master
cd /home/master/cloudys
```

### 3.2 Maven 构建

Cloudys 使用 Java 17，若系统默认 Java 是 8，需要显式设置 `JAVA_HOME`：

```bash
cd /home/master/cloudys
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"
java -version
mvn clean package -DskipTests
```

构建成功标志：

```text
[INFO] BUILD SUCCESS
```

本次实际打包成功的模块包括：

- `gateway-service`
- `eureka-service`
- `auth-service`
- `project-service`
- `requirement-service`
- `requirement-analysis-service`
- `inference-service`
- 测试模块：`full-link-tests`、`api-compatibility-tests`

## 4. 本地镜像构建、导出与分发

### 4.1 构建 Java 服务镜像

在 master 上用 Docker 构建镜像，再导入 containerd：

```bash
cd /home/master/cloudys

SERVICES='eureka-service gateway-service auth-service project-service requirement-service requirement-analysis-service inference-service'

for svc in $SERVICES; do
  case "$svc" in
    eureka-service) df=Dockerfile.eureka ;;
    gateway-service) df=Dockerfile.gateway ;;
    auth-service) df=Dockerfile.auth ;;
    project-service) df=Dockerfile.project ;;
    requirement-service) df=Dockerfile.requirement ;;
    requirement-analysis-service) df=Dockerfile.requirement-analysis ;;
    inference-service) df=Dockerfile.inference ;;
  esac

  sudo docker build --progress=plain \
    -t "cloudys/$svc:latest" \
    -f "deploy/docker/$df" \
    .
done
```

### 4.2 构建 Python sidecar 镜像

`inference-service` 的 Pod 中有两个容器，其中 `python-sidecar` 也必须构建并导入所有可能调度到的节点：

```bash
sudo docker build --progress=plain \
  -t cloudys/python-sidecar:latest \
  -f deploy/docker/Dockerfile.python-sidecar \
  .
```

常见问题：

- 如果没有构建 / 导入 `python-sidecar`，Pod 会显示 `ImagePullBackOff`，并尝试从 Docker Hub 拉取 `cloudys/python-sidecar:latest`。

### 4.3 构建前端镜像

前端使用 `frontend/dist` 静态文件 + Nginx。

关键经验：前端代码的 API 基址是：

```js
baseURL: '/api/v2'
inference baseURL: '/inference'
```

因此前端 Nginx 必须反向代理：

- `/api/` 到 `gateway-service:8008`
- `/inference/` 到 `gateway-service:8008`

不能只做静态文件服务，否则登录页请求 `POST /api/v2/auth/login` 会落到 Nginx 静态服务器上，出现：

```text
Request failed with status code 405
```

推荐前端 Dockerfile 片段：

```dockerfile
FROM nginx:1.27-alpine
COPY frontend/dist/ /usr/share/nginx/html/
RUN printf 'server {\n  listen 8080;\n  server_name _;\n  root /usr/share/nginx/html;\n  index index.html;\n\n  location /api/ {\n    resolver 10.96.0.10 valid=10s ipv6=off;\n    set $gateway_upstream gateway-service.cloudys.svc.cluster.local;\n    proxy_pass http://$gateway_upstream:8008;\n    proxy_http_version 1.1;\n    proxy_set_header Host $host;\n    proxy_set_header X-Real-IP $remote_addr;\n    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;\n    proxy_set_header X-Forwarded-Proto $scheme;\n  }\n\n  location /inference/ {\n    resolver 10.96.0.10 valid=10s ipv6=off;\n    set $gateway_upstream gateway-service.cloudys.svc.cluster.local;\n    proxy_pass http://$gateway_upstream:8008;\n    proxy_http_version 1.1;\n    proxy_set_header Host $host;\n    proxy_set_header X-Real-IP $remote_addr;\n    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;\n    proxy_set_header X-Forwarded-Proto $scheme;\n  }\n\n  location / {\n    try_files $uri $uri/ /index.html;\n  }\n}\n' > /etc/nginx/conf.d/default.conf
EXPOSE 8080
```

> 注意：`resolver` 不要写 `kube-dns.kube-system.svc.cluster.local`，Nginx 启动时不会先解析这个名称，可能报：
>
> ```text
> host not found in resolver "kube-dns.kube-system.svc.cluster.local"
> ```
>
> 应直接写 CoreDNS Service 的 ClusterIP，例如本次集群是 `10.96.0.10`。可用下面命令获取：
>
> ```bash
> kubectl -n kube-system get svc kube-dns -o jsonpath='{.spec.clusterIP}'
> ```

构建前端镜像：

```bash
sudo docker build --progress=plain \
  -t cloudys/frontend-service:latest \
  -f /tmp/cloudys-frontend.Dockerfile \
  .
```

### 4.4 导出并导入镜像

将所有镜像打包：

```bash
mkdir -p /home/master/cloudys-image-tars
sudo docker save \
  cloudys/eureka-service:latest \
  cloudys/gateway-service:latest \
  cloudys/auth-service:latest \
  cloudys/project-service:latest \
  cloudys/requirement-service:latest \
  cloudys/requirement-analysis-service:latest \
  cloudys/inference-service:latest \
  cloudys/python-sidecar:latest \
  cloudys/frontend-service:latest \
  -o /home/master/cloudys-image-tars/cloudys-all-latest.tar

sudo chmod 644 /home/master/cloudys-image-tars/cloudys-all-latest.tar
sha256sum /home/master/cloudys-image-tars/cloudys-all-latest.tar
```

导入 master 的 containerd：

```bash
sudo ctr -n k8s.io images import /home/master/cloudys-image-tars/cloudys-all-latest.tar
sudo ctr -n k8s.io images ls | grep 'docker.io/cloudys/'
```

用临时 HTTP 服务分发给 worker：

```bash
cd /home/master/cloudys-image-tars
python3 -m http.server 18081 --bind 0.0.0.0
```

worker 节点下载并导入：

```bash
mkdir -p ~/cloudys-image-tars
cd ~/cloudys-image-tars
curl -fL -o cloudys-all-latest.tar http://192.168.200.140:18081/cloudys-all-latest.tar
sha256sum cloudys-all-latest.tar
sudo ctr -n k8s.io images import cloudys-all-latest.tar
sudo ctr -n k8s.io images ls | grep 'docker.io/cloudys/'
```

> 经验：如果 Pod 可能调度到 worker，就必须在对应 worker 上导入镜像。只在 master 导入镜像是不够的。

## 5. Kubernetes 部署策略

### 5.1 小虚拟机资源调度

小内存 / 小磁盘课程虚拟机上，不建议把所有关键服务绑到 master。master 很容易出现 `DiskPressure`，导致被 nodeSelector 绑死到 master 的 Pod 一直 Pending。

本次稳定调度策略：

- `eureka-service` → `worker1`
- `auth-service` → `worker1`
- `requirement-service` → `worker1`
- `frontend-service` → `worker1`
- `project-service` → `worker2`
- `requirement-analysis-service` → `worker2`
- `gateway-service` → `worker2`
- `inference-service` → `worker2`
- `postgresql` → 根据现有 PV / 调度结果运行，本次最终在 `worker1`

示例 nodeSelector：

```yaml
spec:
  template:
    spec:
      nodeSelector:
        kubernetes.io/hostname: worker1
```

### 5.2 Apply 顺序

推荐顺序：

```bash
K=deploy/k8s-lite
kubectl apply -f "$K/00-namespace.yml"
kubectl apply -f "$K/10-jwt-secret.yml"
kubectl apply -f "$K/98-deepseek-secret.yml"
kubectl apply -f "$K/01-postgres-secret.yml"
kubectl apply -f "$K/01-postgres-configmap.yml"
kubectl apply -f "$K/01-postgres-pv-pvc.yml"
kubectl apply -f "$K/01-postgres-statefulset.yml"
kubectl apply -f "$K/02-eureka-service.yml"
kubectl apply -f "$K/04-auth-service.yml"
kubectl apply -f "$K/05-project-service.yml"
kubectl apply -f "$K/06-requirement-service.yml"
kubectl apply -f "$K/07-requirement-analysis-service.yml"
kubectl apply -f "$K/09-inference-service.yml"
kubectl apply -f "$K/03-gateway-service.yml"
kubectl apply -f "$K/12-frontend-service.yml"
kubectl apply -f "$K/08-ingress.yml" || true
```

然后滚动重启：

```bash
for d in eureka-service auth-service project-service requirement-service requirement-analysis-service inference-service gateway-service frontend-service; do
  kubectl rollout restart deployment/$d -n cloudys || true
  kubectl rollout status deployment/$d -n cloudys --timeout=240s || true
done
```

## 6. Dashboard 与 Jenkins 部署

### 6.1 Kubernetes Dashboard

部署 Dashboard：

```bash
kubectl apply -f https://raw.githubusercontent.com/kubernetes/dashboard/v2.7.0/aio/deploy/recommended.yaml
kubectl apply -f deploy/k8s-lite/11-dashboard-admin.yml
```

将 Dashboard 暴露为 NodePort：

```bash
kubectl -n kubernetes-dashboard patch svc kubernetes-dashboard \
  -p '{"spec":{"type":"NodePort","ports":[{"port":443,"targetPort":8443,"nodePort":30443,"protocol":"TCP"}]}}'
```

访问地址：

```text
https://192.168.200.140:30443/
```

生成管理员 token：

```bash
kubectl -n kubernetes-dashboard create token dashboard-admin --duration=24h
```

### 6.2 Jenkins 轻量部署

课程验收中可以部署一个轻量 Jenkins，证明 CI/CD 环境存在，并展示项目内 `deploy/jenkins/Jenkinsfile`。

示例资源：

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: jenkins
---
apiVersion: v1
kind: ServiceAccount
metadata:
  name: jenkins
  namespace: jenkins
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: jenkins-cloudys-admin
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: cluster-admin
subjects:
  - kind: ServiceAccount
    name: jenkins
    namespace: jenkins
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: jenkins
  namespace: jenkins
spec:
  replicas: 1
  strategy:
    type: Recreate
  selector:
    matchLabels:
      app: jenkins
  template:
    metadata:
      labels:
        app: jenkins
    spec:
      serviceAccountName: jenkins
      nodeSelector:
        kubernetes.io/hostname: worker2
      containers:
        - name: jenkins
          image: jenkins/jenkins:lts-jdk17
          imagePullPolicy: IfNotPresent
          ports:
            - name: http
              containerPort: 8080
            - name: agent
              containerPort: 50000
          env:
            - name: JAVA_OPTS
              value: "-Xms128m -Xmx512m -Djenkins.install.runSetupWizard=false"
          readinessProbe:
            httpGet:
              path: /login
              port: 8080
            initialDelaySeconds: 60
            periodSeconds: 15
            timeoutSeconds: 5
            failureThreshold: 12
          resources:
            requests:
              cpu: "100m"
              memory: "384Mi"
            limits:
              cpu: "1000m"
              memory: "768Mi"
---
apiVersion: v1
kind: Service
metadata:
  name: jenkins
  namespace: jenkins
spec:
  type: NodePort
  selector:
    app: jenkins
  ports:
    - name: http
      port: 8080
      targetPort: 8080
      nodePort: 30090
    - name: agent
      port: 50000
      targetPort: 50000
```

访问地址：

```text
http://192.168.200.140:30090/
```

## 7. 验收检查命令

### 7.1 Kubernetes 资源状态

```bash
kubectl get nodes -o wide
kubectl get pods,svc,deploy,statefulset,pvc -n cloudys -o wide
kubectl get pods,svc -n kubernetes-dashboard -o wide
kubectl get pods,svc -n jenkins -o wide
```

期望：

- 所有 Cloudys Pod 均 Running。
- `inference-service` 应为 `2/2 Running`。
- 所有 Deployment `AVAILABLE=1`。
- PostgreSQL PVC Bound。
- Dashboard / Jenkins Pod Running。

### 7.2 前端页面

```bash
curl -i http://192.168.200.140:30080/
```

期望：

```text
HTTP/1.1 200 OK
Content-Type: text/html
```

### 7.3 前端同源 API 代理

验证登录 API 已经从前端 Nginx 代理到 Gateway，而不是返回 405：

```bash
curl -i \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}' \
  http://192.168.200.140:30080/api/v2/auth/login
```

可接受结果：

- 如果账号密码不对，返回 `401` + `{"detail":"用户名或密码错误"}`。
- 不应返回 `405 Method Not Allowed`。

### 7.4 Gateway 健康检查

```bash
curl -i http://192.168.200.140:30008/actuator/health
```

期望：

```text
HTTP/1.1 200 OK
"status":"UP"
```

### 7.5 鉴权拦截

```bash
curl -i http://192.168.200.140:30008/api/v2/auth/profile
curl -i http://192.168.200.140:30008/api/v2/auth/me
```

期望：

```text
HTTP/1.1 401 Unauthorized
{"detail":"未认证或 token 已过期"}
```

### 7.6 Eureka 注册表

```bash
kubectl run -n cloudys curl-eureka-apps --rm -i --restart=Never \
  --image=curlimages/curl:8.10.1 \
  -- sh -c 'curl -sS http://eureka-service:8888/eureka/apps | grep -E "<name>|<status>" | head -80'
```

期望看到：

- `REQUIREMENT-SERVICE`：`UP`
- `AUTH-SERVICE`：`UP`
- `REQUIREMENT-ANALYSIS-SERVICE`：`UP`
- `GATEWAY-SERVICE`：`UP`
- `PROJECT-SERVICE`：`UP`
- `INFERENCE-SERVICE`：`UP`

## 8. 本次关键故障与解决经验

### 8.1 master DiskPressure 导致 Pod Pending

症状：

```text
0/3 nodes are available: 1 node(s) had untolerated taint {node.kubernetes.io/disk-pressure: }, 2 node(s) didn't match Pod's node affinity/selector.
```

原因：

- master 磁盘使用率过高。
- 部分服务通过 nodeSelector 绑定到 master。

处理：

- 清理失败 Pod、日志、Docker 构建缓存、镜像 tar。
- 将服务分散到 worker1 / worker2。

### 8.2 只在 master 导入镜像，worker 仍拉取失败

症状：

```text
ImagePullBackOff
pull access denied, repository does not exist
```

原因：

- 本地镜像没有远程仓库，Pod 调度到 worker 后，worker 没有该镜像。

处理：

- 将镜像 tar 分发到所有可能运行 Pod 的节点。
- 执行：

```bash
sudo ctr -n k8s.io images import image.tar
```

### 8.3 `python-sidecar` 漏构建导致 inference-service 不可用

症状：

```text
python-sidecar ImagePullBackOff
inference-service 0/2
```

原因：

- `inference-service` Pod 包含 Java 服务容器和 Python sidecar 容器。
- 只构建了 Java 镜像，没有构建 `cloudys/python-sidecar:latest`。

处理：

```bash
sudo docker build -t cloudys/python-sidecar:latest -f deploy/docker/Dockerfile.python-sidecar .
sudo ctr -n k8s.io images import python-sidecar.tar
```

### 8.4 前端 Nginx 启动时解析 Gateway 失败

症状：

```text
host not found in upstream "gateway-service.cloudys.svc.cluster.local"
```

原因：

- Nginx 在启动时解析 upstream 域名。
- 如果 DNS 暂时不可用，Nginx 直接退出。

处理：

- 使用变量形式 `proxy_pass`，让 DNS 解析发生在请求期。
- 配置 `resolver`。

### 8.5 `resolver` 写 kube-dns 域名导致 Nginx 启动失败

症状：

```text
host not found in resolver "kube-dns.kube-system.svc.cluster.local"
```

原因：

- Nginx 的 `resolver` 指令需要 IP 地址，不能依赖先解析 DNS 名称。

处理：

```bash
kubectl -n kube-system get svc kube-dns -o jsonpath='{.spec.clusterIP}'
```

然后配置：

```nginx
resolver 10.96.0.10 valid=10s ipv6=off;
```

### 8.6 登录界面 `Request failed with status code 405`

症状：

- 浏览器登录页报：

```text
Request failed with status code 405
```

原因：

- 前端请求 `POST /api/v2/auth/login`。
- Nginx 没有配置 `/api/` 反向代理，POST 请求落到了静态文件服务。

处理：

- 在前端 Nginx 中添加 `/api/` 到 Gateway 的代理。
- 验证：

```bash
curl -i \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}' \
  http://192.168.200.140:30080/api/v2/auth/login
```

如果返回 `401 用户名或密码错误`，说明请求已经到达后端；如果返回 `405`，说明仍然没有正确代理。

### 8.7 新注册用户不能立即登录

现象：

```text
{"registration_status":"pending","message":"注册成功，请等待管理员审核"}
{"detail":"注册审核中，请等待管理员审批"}
```

说明：

- 这是业务逻辑，不是部署故障。
- 验收登录时应使用已审核 / 已存在账号，或先通过后台 / 数据库调整用户状态。

## 9. 收尾清理

部署完成后清理构建残留，避免再次触发 DiskPressure：

```bash
rm -f /home/master/cloudys-image-tars/*.tar
sudo docker system prune -af
sudo apt-get clean
sudo journalctl --vacuum-time=1d
```

停止临时 HTTP 文件服务器：

```bash
if [ -f /tmp/cloudys-tar-http.pid ]; then
  kill $(cat /tmp/cloudys-tar-http.pid) 2>/dev/null || true
  rm -f /tmp/cloudys-tar-http.pid
fi
```

再次验证：

```bash
df -h /
kubectl describe node master | grep -E '^Taints:|DiskPressure' -A4
kubectl get pods,svc,deploy -n cloudys -o wide
```

## 10. 最终访问地址

在本次虚拟机环境中：

```text
Frontend:  http://192.168.200.140:30080/
Gateway:   http://192.168.200.140:30008/actuator/health
Dashboard: https://192.168.200.140:30443/
Jenkins:   http://192.168.200.140:30090/
```

## 11. 最终验收口径

部署完成后，至少应能证明：

- 三节点 Kubernetes 均 Ready。
- `cloudys` namespace 中所有核心 Pod Running。
- PostgreSQL StatefulSet 和 PVC 正常。
- Eureka 页面可访问，多个微服务注册为 UP。
- Gateway 健康检查 UP。
- 未认证访问受保护接口返回 401。
- 前端页面可访问，且登录请求不再返回 405。
- Dashboard 可访问并能生成管理员 token。
- Jenkins Pod Running，NodePort 可访问。

这套验收口径能覆盖云原生实践要求中的微服务、注册发现、网关鉴权、数据库、Kubernetes 编排、Dashboard、CI/CD 展示与前端运行效果。
