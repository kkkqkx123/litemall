#!/bin/bash
# litemall WeChat Mini Program build script (Bash)
# Used for WeChat Mini Program code checking and preview

set -e  # Exit on error

# Default parameters
MODE="build"
UPLOAD=false
VERSION=""
DESC=""
WATCH=false

# Parse command line arguments
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
            echo "Usage: $0 [options]"
            echo ""
            echo "Options:"
            echo "  -m, --mode MODE      Specify mode (check/build/preview/watch)"
            echo "  --upload             Perform upload operation"
            echo "  -v, --version VER    Specify version number"
            echo "  -d, --desc DESC      Specify version description"
            echo "  --watch              Start auto watch"
            echo "  -h, --help           Show help information"
            exit 0
            ;;
        *)
            echo "Unknown parameter: $1"
            exit 1
            ;;
    esac
done

echo "========================================"
echo "Litemall WeChat Mini Program Build Script"
echo "========================================"

# Set variables
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
WX_PROJECT_PATH="$PROJECT_ROOT/litemall-wx"

# Check WeChat Mini Program project directory
if [ ! -d "$WX_PROJECT_PATH" ]; then
    echo "Error: WeChat Mini Program project directory not found: $WX_PROJECT_PATH"
    exit 1
fi

# Check WeChat Developer Tools CLI
WECHAT_CLI="cli"
CLI_FOUND=false

if command -v "$WECHAT_CLI" &> /dev/null; then
    CLI_FOUND=true
    echo "Found CLI: $WECHAT_CLI"
else
    # Try common installation paths
    COMMON_PATHS=(
        "/Applications/wechatwebdevtools.app/Contents/MacOS/cli"
        "/Applications/微信开发者工具.app/Contents/MacOS/cli"
        "/Applications/微信web开发者工具.app/Contents/MacOS/cli"
    )
    
    for PATH_CHECK in "${COMMON_PATHS[@]}"; do
        if [ -f "$PATH_CHECK" ]; then
            WECHAT_CLI="$PATH_CHECK"
            CLI_FOUND=true
            echo "Found CLI: $PATH_CHECK"
            break
        fi
    done
    
    if [ "$CLI_FOUND" = false ]; then
        echo "Warning: WeChat Developer Tools CLI not found"
        echo "Please ensure WeChat Developer Tools is installed and CLI is added to system PATH"
        echo "Or use WeChat Developer Tools to manually upload"
    fi
fi

# Switch to WeChat Mini Program project directory
cd "$WX_PROJECT_PATH"

# Display project information
echo "WeChat Mini Program project directory: $WX_PROJECT_PATH"

# Check project configuration file
PROJECT_CONFIG="$WX_PROJECT_PATH/project.config.json"
if [ ! -f "$PROJECT_CONFIG" ]; then
    echo "Error: Project configuration file not found: $PROJECT_CONFIG"
    exit 1
fi

# Read project configuration
if command -v jq &> /dev/null; then
    PROJECT_NAME=$(jq -r '.projectname // "Unknown"' "$PROJECT_CONFIG")
    APP_ID=$(jq -r '.appid // "Unknown"' "$PROJECT_CONFIG")
    echo "Project name: $PROJECT_NAME"
    echo "App ID: $APP_ID"
else
    echo "Warning: jq not found, cannot parse project configuration"
    echo "Please install jq: brew install jq (macOS) or apt-get install jq (Ubuntu)"
fi

# Execute different mode operations
case "$MODE" in
    "check")
        echo ""
        echo "Performing code check..."
        
        # Check app.json
        APP_JSON="$WX_PROJECT_PATH/app.json"
        if [ -f "$APP_JSON" ]; then
            echo "✅ app.json exists"
            if command -v jq &> /dev/null; then
                PAGE_COUNT=$(jq '.pages | length' "$APP_JSON" 2>/dev/null || echo "Unknown")
                echo "  Page count: $PAGE_COUNT"
            fi
        else
            echo "❌ app.json not found"
        fi
        
        # Check app.js
        APP_JS="$WX_PROJECT_PATH/app.js"
        if [ -f "$APP_JS" ]; then
            echo "✅ app.js exists"
        else
            echo "❌ app.js not found"
        fi
        
        # Check app.wxss
        APP_WXSS="$WX_PROJECT_PATH/app.wxss"
        if [ -f "$APP_WXSS" ]; then
            echo "✅ app.wxss exists"
        else
            echo "❌ app.wxss not found"
        fi
        
        # Check directory structure
        PAGES_DIR="$WX_PROJECT_PATH/pages"
        if [ -d "$PAGES_DIR" ]; then
            if command -v find &> /dev/null; then
                PAGE_DIR_COUNT=$(find "$PAGES_DIR" -maxdepth 1 -type d 2>/dev/null | wc -l)
                PAGE_DIR_COUNT=$((PAGE_DIR_COUNT - 1))  # Subtract parent directory
                echo "✅ pages directory exists, sub-pages: $PAGE_DIR_COUNT"
            else
                echo "✅ pages directory exists"
            fi
        else
            echo "❌ pages directory not found"
        fi
        
        UTILS_DIR="$WX_PROJECT_PATH/utils"
        if [ -d "$UTILS_DIR" ]; then
            echo "✅ utils directory exists"
        else
            echo "❌ utils directory not found"
        fi
        
        # Check sitemap.json
        SITEMAP_JSON="$WX_PROJECT_PATH/sitemap.json"
        if [ -f "$SITEMAP_JSON" ]; then
            echo "✅ sitemap.json exists"
        else
            echo "❌ sitemap.json not found"
        fi
        
        echo ""
        echo "Code check completed!"
        ;;
    
    "build")
        echo ""
        echo "Building WeChat Mini Program..."
        
        # Check if upload is needed
        if [ "$UPLOAD" = true ]; then
            if [ -z "$VERSION" ] || [ -z "$DESC" ]; then
                echo "Error: Upload mode requires version and description"
                echo "Example: --upload --version \"1.0.0\" --desc \"Update content\""
                exit 1
            fi
            
            if [ "$CLI_FOUND" = false ]; then
                echo "Error: CLI not found, cannot upload"
                exit 1
            fi
            
            echo "Uploading to WeChat Mini Program..."
            echo "Version: $VERSION"
            echo "Description: $DESC"
            
            # Execute upload command
            "$WECHAT_CLI" upload --project "$WX_PROJECT_PATH" --version "$VERSION" --desc "$DESC"
            
            if [ $? -eq 0 ]; then
                echo "✅ Upload successful!"
            else
                echo "❌ Upload failed"
                exit 1
            fi
        else
            echo "Build mode: checking code only, no upload"
            echo "Use --upload parameter to perform upload"
            
            # Perform basic check
            "$SCRIPT_DIR/build-wx.sh" --mode check
        fi
        ;;
    
    "preview")
        echo ""
        echo "Generating WeChat Mini Program preview..."
        
        if [ "$CLI_FOUND" = false ]; then
            echo "Error: CLI not found, cannot generate preview"
            exit 1
        fi
        
        # Generate preview QR code
        "$WECHAT_CLI" preview --project "$WX_PROJECT_PATH"
        
        if [ $? -eq 0 ]; then
            echo "✅ Preview QR code generated!"
        else
            echo "❌ Preview generation failed"
            exit 1
        fi
        ;;
    
    "watch")
        echo ""
        echo "Starting WeChat Mini Program auto-build..."
        echo "Press Ctrl+C to stop watching"
        
        if [ "$CLI_FOUND" = false ]; then
            echo "Error: CLI not found, cannot start auto-build"
            exit 1
        fi
        
        # Watch file changes and auto-upload
        "$WECHAT_CLI" auto --project "$WX_PROJECT_PATH"
        ;;
    
    *)
        echo "Unknown mode: $MODE"
        echo "Available modes: check, build, preview, watch"
        exit 1
        ;;
esac

echo ""
echo "WeChat Mini Program build completed!"