# Kubernetes Dashboard 使用说明

本仓库已提供 Dashboard 管理员账号资源，但**不强行绑定某一种安装清单**。这样做是为了避免把已经过时的 Dashboard 安装方式直接写死进仓库。

## 1. 前提

- 集群中已经安装 `kubernetes-dashboard`
- 本机已能使用 `kubectl` 访问该集群

## 2. 创建管理员账号

```bash
kubectl apply -f deploy/k8s/11-dashboard-admin.yml
```

## 3. 生成登录 token

```bash
kubectl -n kubernetes-dashboard create token dashboard-admin
```

把输出的 token 复制出来，登录时使用。

## 4. 本地访问 Dashboard

优先使用 `kubectl proxy`:

```bash
kubectl proxy
```

然后打开:

```text
http://localhost:8001/api/v1/namespaces/kubernetes-dashboard/services/https:kubernetes-dashboard:/proxy/
```

## 5. 验收时建议展示

- `kubectl get pods -n kubernetes-dashboard`
- `kubectl -n kubernetes-dashboard create token dashboard-admin`
- 浏览器登录成功页面
- Dashboard 中的 `Nodes`、`Pods`、`Services`、`Deployments`
