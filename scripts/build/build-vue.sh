#!/bin/bash
# litemall Vue前端构建脚本 (Bash)
# 用于构建Vue移动端商城

set -e  # 遇到错误立即退出

# 默认参数
ENVIRONMENT="production"
DEV_MODE=false
SKIP_INSTALL=false
ANALYZE=false

# 解析命令行参数
while [[ $# -gt 0 ]]; do
    case $1 in
        -e|--env)
            ENVIRONMENT="$2"
            shift 2
            ;;
        --dev)
            DEV_MODE=true
            shift
            ;;
        --skip-install)
            SKIP_INSTALL=true
            shift
            ;;
        --analyze)
            ANALYZE=true
            shift
            ;;
        -h|--help)
            echo "用法: $0 [选项]"
            echo ""
            echo "选项:"
            echo "  -e, --env ENVIRONMENT    指定构建环境 (production/dep)"
            echo "  --dev                    启动开发服务器"
            echo "  --skip-install           跳过npm install"
            echo "  --analyze                启用构建分析"
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
echo "Litemall Vue前端构建脚本"
echo "========================================"

# 设置变量
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$(dirname "$SCRIPT_DIR")")"
VUE_PROJECT_PATH="$PROJECT_ROOT/litemall-vue"

# 检查Node.js和npm是否安装
if ! command -v node &> /dev/null; then
    echo "错误: Node.js未找到，请先安装Node.js"
    exit 1
fi

if ! command -v npm &> /dev/null; then
    echo "错误: npm未找到，请先安装npm"
    exit 1
fi

# 检查Vue项目目录
if [ ! -d "$VUE_PROJECT_PATH" ]; then
    echo "错误: Vue项目目录不存在: $VUE_PROJECT_PATH"
    exit 1
fi

# 切换到Vue项目目录
cd "$VUE_PROJECT_PATH"

# 显示项目信息
echo "Vue项目目录: $VUE_PROJECT_PATH"
echo "Node版本: $(node --version)"
echo "npm版本: $(npm --version)"

# 安装依赖（如果跳过则跳过）
if [ "$SKIP_INSTALL" = false ]; then
    echo ""
    echo "正在安装依赖..."
    
    if [ -f "package-lock.json" ]; then
        echo "检测到package-lock.json，使用npm ci..."
        npm ci
    else
        echo "使用npm install..."
        npm install
    fi
    
    if [ $? -ne 0 ]; then
        echo "❌ 依赖安装失败"
        exit 1
    fi
fi

# 构建命令
if [ "$DEV_MODE" = true ]; then
    echo ""
    echo "正在启动开发服务器..."
    echo "访问地址: http://localhost:8080"
    npm run serve
else
    echo ""
    echo "正在构建生产环境..."
    
    # 清理旧的构建文件
    DIST_PATH="$VUE_PROJECT_PATH/dist"
    if [ -d "$DIST_PATH" ]; then
        echo "清理旧的构建文件..."
        rm -rf "$DIST_PATH"
    fi
    
    # 执行构建
    BUILD_COMMAND="npm run build"
    if [ "$ENVIRONMENT" != "production" ]; then
        BUILD_COMMAND="npm run build:$ENVIRONMENT"
    fi
    
    if [ "$ANALYZE" = true ]; then
        echo "启用构建分析..."
        export ANALYZE=true
    fi
    
    $BUILD_COMMAND
    
    if [ $? -eq 0 ]; then
        echo ""
        echo "✅ Vue构建成功!"
        
        # 显示构建结果
        if [ -d "$DIST_PATH" ]; then
            FILE_COUNT=$(find "$DIST_PATH" -type f | wc -l)
            TOTAL_SIZE=$(du -sh "$DIST_PATH" | cut -f1)
            echo "构建文件数量: $FILE_COUNT"
            echo "总大小: $TOTAL_SIZE"
            
            # 显示主要文件
            echo "主要文件:"
            find "$DIST_PATH" -type f -exec ls -lh {} + | sort -k5 -hr | head -5 | awk '{print "  " $9 ": " $5}'
        fi
    else
        echo ""
        echo "❌ Vue构建失败，请检查错误信息"
        exit 1
    fi
fi

echo ""
echo "构建完成!"
if [ "$DEV_MODE" = false ]; then
    echo "Deploy path: $DIST_PATH"
fi