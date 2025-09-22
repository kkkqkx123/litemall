#!/bin/bash
# litemall后端构建脚本 (Bash)
# 用于构建所有后端模块

set -e  # 遇到错误立即退出

# 默认参数
PROFILE="dev"
SKIP_TESTS=true
CLEAN=false

# 解析命令行参数
while [[ $# -gt 0 ]]; do
    case $1 in
        -p|--profile)
            PROFILE="$2"
            shift 2
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
        -h|--help)
            echo "用法: $0 [选项]"
            echo ""
            echo "选项:"
            echo "  -p, --profile PROFILE    指定构建环境 (dev/test/prod)"
            echo "  --skip-tests             跳过测试 (默认)"
            echo "  --run-tests              运行测试"
            echo "  --clean                  执行clean操作"
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
echo "Litemall 后端构建脚本"
echo "========================================"

# 设置变量
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# 检查Maven是否安装
if ! command -v mvn &> /dev/null; then
    echo "错误: Maven未找到，请先安装Maven并配置环境变量"
    exit 1
fi

# 切换到项目根目录
cd "$PROJECT_ROOT"

# 构建参数
MAVEN_ARGS=()
if [ "$CLEAN" = true ]; then
    MAVEN_ARGS+=("clean")
fi
MAVEN_ARGS+=("install")

if [ "$SKIP_TESTS" = true ]; then
    MAVEN_ARGS+=("-DskipTests")
fi

if [ "$PROFILE" != "dev" ]; then
    MAVEN_ARGS+=("-P$PROFILE")
fi

# 显示构建信息
echo "项目根目录: $PROJECT_ROOT"
echo "构建命令: mvn ${MAVEN_ARGS[*]}"
echo "跳过测试: $SKIP_TESTS"
echo "构建环境: $PROFILE"
echo ""

# 开始构建
echo "开始构建后端模块..."
START_TIME=$(date +%s)

# 执行构建
if mvn "${MAVEN_ARGS[@]}"; then
    echo ""
    echo "✅ 后端构建成功!"
    
    # 显示构建结果
    JAR_PATH="$PROJECT_ROOT/litemall-all/target/litemall-all-0.1.0.jar"
    if [ -f "$JAR_PATH" ]; then
        echo "生成的JAR文件: $JAR_PATH"
        JAR_SIZE=$(du -h "$JAR_PATH" | cut -f1)
        echo "文件大小: $JAR_SIZE"
    fi
else
    echo ""
    echo "❌ 后端构建失败，请检查错误信息"
    exit 1
fi

END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))
echo "构建耗时: ${DURATION} 秒"

# 构建完成后可选操作
echo ""
echo "Next steps after build:"
echo "1. Run: java -jar $JAR_PATH"
echo "2. Docker: docker-compose up"
echo "3. View logs: tail -f logs/litemall.log"