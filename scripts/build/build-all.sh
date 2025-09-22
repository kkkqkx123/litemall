#!/bin/bash
# litemall全量构建脚本 (Bash)
# 一键构建后端、Vue前端和微信小程序

set -e  # 遇到错误立即退出

# 默认参数
PROFILE="dev"
SKIP_BACKEND=false
SKIP_VUE=false
SKIP_WX=false
SKIP_TESTS=true
CLEAN=false
PARALLEL=false

# 解析命令行参数
while [[ $# -gt 0 ]]; do
    case $1 in
        -p|--profile)
            PROFILE="$2"
            shift 2
            ;;
        --skip-backend)
            SKIP_BACKEND=true
            shift
            ;;
        --skip-vue)
            SKIP_VUE=true
            shift
            ;;
        --skip-wx)
            SKIP_WX=true
            shift
            ;;
        --skip-tests)
            SKIP_TESTS=true
            shift
            ;;
        --run-tests)
            SKIP_TESTS=false
            shift
            ;;
        --clean)
            CLEAN=true
            shift
            ;;
        --parallel)
            PARALLEL=true
            shift
            ;;
        -h|--help)
            echo "用法: $0 [选项]"
            echo ""
            echo "选项:"
            echo "  -p, --profile PROFILE    指定构建环境 (dev/test/prod)"
            echo "  --skip-backend           跳过后端构建"
            echo "  --skip-vue               跳过Vue前端构建"
            echo "  --skip-wx                跳过微信小程序检查"
            echo "  --skip-tests             跳过测试 (默认)"
            echo "  --run-tests              运行测试"
            echo "  --clean                  执行clean操作"
            echo "  --parallel               启用并行构建"
            echo "  -h, --help               显示帮助信息"
            exit 0
            ;;
        *)
            echo "未知参数: $1"
            exit 1
            ;;
    esac
done

echo "========================================"
echo "Litemall 全量构建脚本"
echo "========================================"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
START_TIME=$(date +%s)

# 显示构建计划
echo "构建计划:"
echo "  后端构建: $(if [ "$SKIP_BACKEND" = true ]; then echo '跳过'; else echo '执行'; fi)"
echo "  Vue前端: $(if [ "$SKIP_VUE" = true ]; then echo '跳过'; else echo '执行'; fi)"
echo "  微信小程序: $(if [ "$SKIP_WX" = true ]; then echo '跳过'; else echo '执行'; fi)"
echo "  并行构建: $PARALLEL"

# 构建函数
build_backend() {
    echo "📦 开始构建后端..."
    if "$SCRIPT_DIR/build-backend.sh" -p "$PROFILE" --skip-tests="$SKIP_TESTS" --clean="$CLEAN"; then
        return 0
    else
        echo "❌ 后端构建失败"
        return 1
    fi
}

build_vue() {
    echo "📱 开始构建Vue前端..."
    if "$SCRIPT_DIR/build-vue.sh" --env production --skip-install=false; then
        return 0
    else
        echo "❌ Vue前端构建失败"
        return 1
    fi
}

build_wx() {
    echo "🎯 开始检查微信小程序..."
    if "$SCRIPT_DIR/build-wx.sh" -m check; then
        return 0
    else
        echo "❌ 微信小程序检查失败"
        return 1
    fi
}

# 执行构建
SUCCESS=true

if [ "$PARALLEL" = true ] && [ "$SKIP_BACKEND" = false ] && [ "$SKIP_VUE" = false ]; then
    # 并行构建后端和Vue前端
    echo "🔄 启动并行构建..."
    
    # 使用后台进程并行构建
    build_backend &
    BACKEND_PID=$!
    
    build_vue &
    VUE_PID=$!
    
    # 等待并行任务完成
    wait $BACKEND_PID
    BACKEND_RESULT=$?
    
    wait $VUE_PID
    VUE_RESULT=$?
    
    if [ $BACKEND_RESULT -ne 0 ] || [ $VUE_RESULT -ne 0 ]; then
        SUCCESS=false
    fi
    
    # 微信小程序不能并行，因为可能需要CLI交互
    if [ "$SKIP_WX" = false ] && [ "$SUCCESS" = true ]; then
        build_wx || SUCCESS=false
    fi
else
    # 串行构建
    if [ "$SKIP_BACKEND" = false ]; then
        build_backend || SUCCESS=false
    fi
    
    if [ "$SUCCESS" = true ] && [ "$SKIP_VUE" = false ]; then
        build_vue || SUCCESS=false
    fi
    
    if [ "$SUCCESS" = true ] && [ "$SKIP_WX" = false ]; then
        build_wx || SUCCESS=false
    fi
fi

END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))

echo ""
echo "========================================"

if [ "$SUCCESS" = true ]; then
    echo "✅ 全量构建成功!"
    echo "总耗时: ${DURATION} 秒"
    
    echo ""
    echo "构建结果:"
    
    if [ "$SKIP_BACKEND" = false ]; then
        JAR_PATH="$PROJECT_ROOT/litemall-all/target/litemall-all-0.1.0.jar"
        if [ -f "$JAR_PATH" ]; then
            JAR_SIZE=$(du -h "$JAR_PATH" | cut -f1)
            echo "  后端JAR: $JAR_SIZE"
        fi
    fi
    
    if [ "$SKIP_VUE" = false ]; then
        VUE_DIST="$PROJECT_ROOT/litemall-vue/dist"
        if [ -d "$VUE_DIST" ]; then
            VUE_SIZE=$(du -sh "$VUE_DIST" | cut -f1)
            VUE_FILES=$(find "$VUE_DIST" -type f | wc -l)
            echo "  Vue前端: $VUE_SIZE ($VUE_FILES files)"
        fi
    fi
    
    echo ""
    echo "Next steps:"
    echo "  Start backend: java -jar litemall-all/target/litemall-all-0.1.0.jar"
    echo "  Docker start: docker-compose up"
    echo "  Vue dev: npm run serve (in litemall-vue directory)"
else
    echo "❌ 全量构建失败!"
    echo "总耗时: ${DURATION} 秒"
    exit 1
fi