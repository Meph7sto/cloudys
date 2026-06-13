#!/bin/bash
# ================================
# Cloudys Performance Test Runner
# ================================
# 依赖: k6 (https://k6.io)
# 安装: brew install k6  /  apt install k6  /  winget install k6
#
# 用法:
#   ./deploy/perf/run-perf-test.sh                    # 默认地址
#   ./deploy/perf/run-perf-test.sh http://localhost:25698  # 指定地址
#   TEST_TYPE=stress ./deploy/perf/run-perf-test.sh   # 压力测试
# ================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$(dirname "$SCRIPT_DIR")")"
BASE_URL="${1:-http://localhost:25698}"
TEST_TYPE="${TEST_TYPE:-smoke}"

K6_SCRIPT="$SCRIPT_DIR/k6-load-test.js"
RESULTS_DIR="$PROJECT_DIR/target/perf-results"
mkdir -p "$RESULTS_DIR"

echo "==========================================="
echo "Cloudys Performance Test"
echo "Base URL: $BASE_URL"
echo "Test Type: $TEST_TYPE"
echo "==========================================="

# Map test type to k6 options
case "$TEST_TYPE" in
    smoke)
        STAGES="--stage 10s:0 --stage 10s:5 --stage 20s:5 --stage 5s:0"
        ;;
    load)
        STAGES="--stage 30s:0 --stage 30s:20 --stage 60s:20 --stage 30s:0"
        ;;
    stress)
        STAGES="--stage 60s:0 --stage 60s:50 --stage 120s:50 --stage 60s:0"
        ;;
    *)
        echo "Unknown test type: $TEST_TYPE. Use smoke, load, or stress."
        exit 1
        ;;
esac

# Check if k6 is available
if ! command -v k6 &> /dev/null; then
    echo "ERROR: k6 is not installed. Install from https://k6.io/docs/get-started/installation/"
    echo ""
    echo "Alternative: Run with Docker"
    echo "  docker run --rm -i grafana/k6 run - < deploy/perf/k6-load-test.js"
    echo "    (but you'll need to use host.docker.internal or host networking for localhost)"
    exit 1
fi

echo "Running $TEST_TYPE test..."
echo "Results will be saved to: $RESULTS_DIR"
echo ""

k6 run \
    --env BASE_URL="$BASE_URL" \
    --out json="$RESULTS_DIR/results.json" \
    --summary-export="$RESULTS_DIR/summary.json" \
    "$K6_SCRIPT"

EXIT_CODE=$?

echo ""
echo "==========================================="
echo "Test completed with exit code: $EXIT_CODE"
echo "Summary: $RESULTS_DIR/summary.json"
echo "Raw data: $RESULTS_DIR/results.json"
echo "==========================================="

exit $EXIT_CODE
