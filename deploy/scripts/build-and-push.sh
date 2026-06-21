#!/bin/bash
# ================================
# 批量构建和推送微服务镜像脚本
# ================================
# 用法:
#   chmod +x build-and-push.sh
#   REGISTRY=registry.cn-hangzhou.aliyuncs.com/cloudys TAG=v1.0.0 ./build-and-push.sh
# ================================

set -euo pipefail

REGISTRY="${REGISTRY:-registry.cn-hangzhou.aliyuncs.com/cloudys}"
TAG="${TAG:-latest}"
BUILD_DIR="$(cd "$(dirname "$0")/../.." && pwd)"

echo "=== 镜像构建与推送 ==="
echo "REGISTRY: ${REGISTRY}"
echo "TAG: ${TAG}"
echo "BUILD_DIR: ${BUILD_DIR}"
echo ""

# 所有需要构建镜像的微服务
SERVICES=(
  "eureka-service"
  "gateway-service"
  "auth-service"
  "project-service"
  "requirement-service"
  "requirement-analysis-service"
  "inference-service"
  "python-sidecar"
)

# Dockerfile 映射（Dockerfile.<简名>）
declare -A DOCKERFILE_MAP=(
  ["eureka-service"]="Dockerfile.eureka"
  ["gateway-service"]="Dockerfile.gateway"
  ["auth-service"]="Dockerfile.auth"
  ["project-service"]="Dockerfile.project"
  ["requirement-service"]="Dockerfile.requirement"
  ["requirement-analysis-service"]="Dockerfile.requirement-analysis"
  ["inference-service"]="Dockerfile.inference"
  ["python-sidecar"]="Dockerfile.python-sidecar"
)

cd "${BUILD_DIR}"

# 1. Maven 打包所有模块
echo ">>> 1. Maven 打包 (mvn clean package -DskipTests)"
mvn clean package -DskipTests
echo ""

# 2. 构建镜像
echo ">>> 2. 构建各服务镜像"
for svc in "${SERVICES[@]}"; do
  dockerfile="${DOCKERFILE_MAP[$svc]}"
  echo "  Building ${svc} (${dockerfile})..."
  nerdctl build \
    -t "${REGISTRY}/${svc}:${TAG}" \
    -t "${REGISTRY}/${svc}:latest" \
    -f "deploy/docker/${dockerfile}" \
    .
done
echo ""

# 3. 推送镜像
echo ">>> 3. 推送镜像到远程仓库"
echo "  提示：如未登录，请先执行 nerdctl login ${REGISTRY}"
for svc in "${SERVICES[@]}"; do
  echo "  Pushing ${svc}:${TAG}..."
  nerdctl push "${REGISTRY}/${svc}:${TAG}"
  echo "  Pushing ${svc}:latest..."
  nerdctl push "${REGISTRY}/${svc}:latest"
done

echo ""
echo "=== 镜像构建和推送完成 ==="
echo "镜像列表:"
for svc in "${SERVICES[@]}"; do
  echo "  ${REGISTRY}/${svc}:${TAG}"
done
