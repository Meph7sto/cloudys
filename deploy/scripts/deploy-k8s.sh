#!/bin/bash
# ================================
# Kubernetes 部署脚本
# ================================
# 用法:
#   chmod +x deploy-k8s.sh
#   REGISTRY=registry.cn-hangzhou.aliyuncs.com/cloudys TAG=v1.0.0 ./deploy-k8s.sh
# ================================

set -euo pipefail

REGISTRY="${REGISTRY:-registry.cn-hangzhou.aliyuncs.com/cloudys}"
TAG="${TAG:-latest}"
K8S_NAMESPACE="cloudys"
K8S_DIR="$(cd "$(dirname "$0")/../k8s" && pwd)"
NFS_SERVER="${NFS_SERVER:-}"
NFS_PATH="${NFS_PATH:-}"

echo "=== Kubernetes 部署 ==="
echo "REGISTRY: ${REGISTRY}"
echo "TAG: ${TAG}"
echo "NAMESPACE: ${K8S_NAMESPACE}"
echo "NFS_SERVER: ${NFS_SERVER:-<unset>}"
echo "NFS_PATH: ${NFS_PATH:-<unset>}"
echo ""

if [[ -z "${NFS_SERVER}" || -z "${NFS_PATH}" ]]; then
  echo "ERROR: NFS_SERVER 和 NFS_PATH 必须在部署前设置。"
  echo "示例:"
  echo "  export NFS_SERVER=192.168.1.10"
  echo "  export NFS_PATH=/data/nfs/cloudys/postgresql"
  exit 1
fi

# 1. 创建 Namespace
echo ">>> 1. 创建 Namespace"
kubectl apply -f "${K8S_DIR}/00-namespace.yml"

# 2. 部署数据库和持久化资源
echo ">>> 2. 部署 PostgreSQL 及持久化资源"
kubectl apply -f "${K8S_DIR}/10-jwt-secret.yml"
kubectl apply -f "${K8S_DIR}/98-deepseek-secret.yml"
kubectl apply -f "${K8S_DIR}/01-postgres-secret.yml"
kubectl apply -f "${K8S_DIR}/01-postgres-configmap.yml"
envsubst < "${K8S_DIR}/01-postgres-pv-pvc.yml" | kubectl apply -f -
kubectl apply -f "${K8S_DIR}/01-postgres-statefulset.yml"

echo ">>> 等待 PostgreSQL 就绪..."
kubectl wait --for=condition=ready pod -l app=postgresql -n "${K8S_NAMESPACE}" --timeout=120s

# 3. 部署 Eureka 注册中心
echo ">>> 3. 部署 Eureka 注册中心"
export REGISTRY TAG
envsubst < "${K8S_DIR}/02-eureka-service.yml" | kubectl apply -f -
kubectl wait --for=condition=ready pod -l app=eureka-service -n "${K8S_NAMESPACE}" --timeout=60s

# 4. 部署业务服务
echo ">>> 4. 部署业务服务"
for yaml in \
  "${K8S_DIR}/04-auth-service.yml" \
  "${K8S_DIR}/05-project-service.yml" \
  "${K8S_DIR}/06-requirement-service.yml" \
  "${K8S_DIR}/07-requirement-analysis-service.yml" \
  "${K8S_DIR}/09-inference-service.yml"; do
  envsubst < "${yaml}" | kubectl apply -f -
done

# 5. 部署 Gateway（最后部署，依赖其他服务注册到 Eureka）
echo ">>> 5. 部署 Gateway"
envsubst < "${K8S_DIR}/03-gateway-service.yml" | kubectl apply -f -

# 5b. 部署 Ingress (可选)
echo ">>> 5b. 部署 Ingress"
kubectl apply -f "${K8S_DIR}/08-ingress.yml" || echo "  Ingress 部署失败（如未安装 Ingress Controller 可忽略）"

# 6. 验证部署结果
echo ""
echo "=== 验证部署结果 ==="
echo ""
echo "--- Nodes ---"
kubectl get nodes
echo ""
echo "--- Pods (${K8S_NAMESPACE}) ---"
kubectl get pods -n "${K8S_NAMESPACE}" -o wide
echo ""
echo "--- Services (${K8S_NAMESPACE}) ---"
kubectl get svc -n "${K8S_NAMESPACE}"
echo ""
echo "=== 部署完成 ==="
echo "通过 Gateway 访问: http://<node-ip>:30008"
echo "Eureka Dashboard: 通过 kubectl port-forward 访问"
echo "  kubectl port-forward -n ${K8S_NAMESPACE} svc/eureka-service 8888:8888"
echo "  然后打开 http://localhost:8888"
if kubectl get namespace kubernetes-dashboard >/dev/null 2>&1; then
  echo ""
  echo "检测到 kubernetes-dashboard 命名空间，可创建管理员账号:"
  echo "  kubectl apply -f ${K8S_DIR}/11-dashboard-admin.yml"
  echo "  kubectl -n kubernetes-dashboard create token dashboard-admin"
fi
