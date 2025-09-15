#!/bin/bash
# litemall微信小程序构建脚本 (Bash)
# 用于微信小程序代码检查和预览

set -e  # 遇到错误立即退出

# 默认参数
MODE="build"
UPLOAD=false
VERSION=""
DESC=""
WATCH=false

# 解析命令行参数
while [[ $# -gt 0 ]]; do
    case $1 in
        -m|--mode)
            MODE="$2"
            shift 2
            ;;
        --upload)
            UPLOAD=true
            shift
            ;;
        -v|--version)
            VERSION="$2"
            shift 2
            ;;
        -d|--desc)
            DESC="$2"
            shift 2
            ;;
        --watch)
            WATCH=true
            shift
            ;;
        -h|--help)
            echo "用法: $0 [选项]"
            echo ""
            echo "选项:"
            echo "  -m, --mode MODE      指定模式 (check/build/preview/watch)"
            echo "  --upload             执行上传操作"
            echo "  -v, --version VER    指定版本号"
            echo "  -d, --desc DESC      指定版本描述"
            echo "  --watch              启动自动监听"
            echo "  -h, --help           显示帮助信息"
            exit 0
            ;;
        *)
            echo "未知参数: $1"
            exit 1
            ;;
    esac
done

echo "========================================"
echo "Litemall 微信小程序构建脚本"
echo "========================================"

# 设置变量
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
WX_PROJECT_PATH="$PROJECT_ROOT/litemall-wx"

# 检查微信小程序项目目录
if [ ! -d "$WX_PROJECT_PATH" ]; then
    echo "错误: 微信小程序项目目录不存在: $WX_PROJECT_PATH"
    exit 1
fi

# 检查微信开发者工具CLI
WECHAT_CLI="cli"
if ! command -v "$WECHAT_CLI" &> /dev/null; then
    echo "警告: 微信开发者工具CLI未找到"
    echo "请确保微信开发者工具已安装，并添加CLI到系统PATH"
    echo "或者使用微信开发者工具手动上传"
    echo ""
    
    # 尝试常见安装路径
    COMMON_PATHS=(
        "/Applications/wechatwebdevtools.app/Contents/MacOS/cli"
        "/Applications/微信开发者工具.app/Contents/MacOS/cli"
    )
    
    CLI_FOUND=false
    for PATH_CHECK in "${COMMON_PATHS[@]}"; do
        if [ -f "$PATH_CHECK" ]; then
            WECHAT_CLI="$PATH_CHECK"
            CLI_FOUND=true
            echo "找到CLI: $PATH_CHECK"
            break
        fi
    done
    
    if [ "$CLI_FOUND" = false ]; then
        echo "将使用代码检查模式"
        MODE="check"
    fi
fi

# 切换到微信小程序项目目录
cd "$WX_PROJECT_PATH"

# 显示项目信息
echo "微信小程序项目目录: $WX_PROJECT_PATH"

# 检查项目配置文件
PROJECT_CONFIG="$WX_PROJECT_PATH/project.config.json"
if [ ! -f "$PROJECT_CONFIG" ]; then
    echo "错误: 项目配置文件不存在: $PROJECT_CONFIG"
    exit 1
fi

# 读取项目配置
PROJECT_NAME=$(jq -r '.projectname' "$PROJECT_CONFIG" 2>/dev/null || echo "未知")
APP_ID=$(jq -r '.appid' "$PROJECT_CONFIG" 2>/dev/null || echo "未知")
echo "项目名称: $PROJECT_NAME"
echo "AppID: $APP_ID"

# 执行不同模式的操作
case "$MODE" in
    "check")
        echo ""
        echo "正在进行代码检查..."
        
        # 检查app.json
        APP_JSON="$WX_PROJECT_PATH/app.json"
        if [ -f "$APP_JSON" ]; then
            echo "✅ app.json 存在"
            PAGE_COUNT=$(jq '.pages | length' "$APP_JSON" 2>/dev/null || echo "未知")
            echo "  页面数量: $PAGE_COUNT"
        else
            echo "❌ app.json 不存在"
        fi
        
        # 检查app.js
        APP_JS="$WX_PROJECT_PATH/app.js"
        if [ -f "$APP_JS" ]; then
            echo "✅ app.js 存在"
        else
            echo "❌ app.js 不存在"
        fi
        
        # 检查app.wxss
        APP_WXSS="$WX_PROJECT_PATH/app.wxss"
        if [ -f "$APP_WXSS" ]; then
            echo "✅ app.wxss 存在"
        else
            echo "❌ app.wxss 不存在"
        fi
        
        # 检查目录结构
        PAGES_DIR="$WX_PROJECT_PATH/pages"
        if [ -d "$PAGES_DIR" ]; then
            PAGE_DIR_COUNT=$(find "$PAGES_DIR" -maxdepth 1 -type d | wc -l)
            PAGE_DIR_COUNT=$((PAGE_DIR_COUNT - 1))  # 减去父目录
            echo "✅ pages目录存在，子页面数量: $PAGE_DIR_COUNT"
        fi
        
        UTILS_DIR="$WX_PROJECT_PATH/utils"
        if [ -d "$UTILS_DIR" ]; then
            echo "✅ utils目录存在"
        fi
        
        echo ""
        echo "代码检查完成!"
        ;;
    
    "build")
        echo ""
        echo "正在构建微信小程序..."
        
        # 检查是否需要上传
        if [ "$UPLOAD" = true ]; then
            if [ -z "$VERSION" ] || [ -z "$DESC" ]; then
                echo "错误: 上传模式需要指定版本号和描述"
                echo "示例: --upload --version \"1.0.0\" --desc \"更新内容\""
                exit 1
            fi
            
            echo "正在上传到微信小程序..."
            echo "版本号: $VERSION"
            echo "描述: $DESC"
            
            # 执行上传命令
            "$WECHAT_CLI" upload --project "$WX_PROJECT_PATH" --version "$VERSION" --desc "$DESC"
            
            if [ $? -eq 0 ]; then
                echo "✅ 上传成功!"
            else
                echo "❌ 上传失败"
                exit 1
            fi
        else
            echo "构建模式: 仅检查代码，不执行上传"
            echo "使用 --upload 参数可以执行上传操作"
        fi
        ;;
    
    "preview")
        echo ""
        echo "正在生成微信小程序预览..."
        
        # 生成预览二维码
        "$WECHAT_CLI" preview --project "$WX_PROJECT_PATH"
        
        if [ $? -eq 0 ]; then
            echo "✅ 预览二维码已生成!"
        else
            echo "❌ 预览生成失败"
            exit 1
        fi
        ;;
    
    "watch")
        echo ""
        echo "正在启动微信小程序自动构建..."
        echo "按 Ctrl+C 停止监听"
        
        # 监听文件变化并自动上传
        "$WECHAT_CLI" auto --project "$WX_PROJECT_PATH"
        ;;
    
    *)
        echo "未知模式: $MODE"
        echo "可用模式: check, build, preview, watch"
        exit 1
        ;;
esac

echo ""
echo "WeChat Mini Program build completed!"