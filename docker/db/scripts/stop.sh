#!/bin/bash

# MySQL容器停止脚本
# 作者: litemall项目
# 描述: 停止并可选删除MySQL容器

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 打印信息函数
print_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 显示使用帮助
show_usage() {
    echo "使用方法: $0 [选项]"
    echo ""
    echo "选项:"
    echo "  -r, --remove    停止后删除容器"
    echo "  -v, --volumes   删除容器时同时删除数据卷"
    echo "  -h, --help      显示此帮助信息"
    echo ""
    echo "示例:"
    echo "  $0              # 仅停止容器"
    echo "  $0 -r           # 停止并删除容器"
    echo "  $0 -r -v        # 停止并删除容器和数据卷"
}

# 停止容器
stop_container() {
    local container_name="mysql"
    
    if docker ps --format 'table {{.Names}}' | grep -q "^$container_name$"; then
        print_info "正在停止MySQL容器..."
        docker stop "$container_name"
        print_info "MySQL容器已停止"
    else
        print_info "MySQL容器未运行"
    fi
}

# 删除容器
delete_container() {
    local container_name="mysql"
    
    if docker ps -a --format 'table {{.Names}}' | grep -q "^$container_name$"; then
        print_info "正在删除MySQL容器..."
        docker rm "$container_name"
        print_info "MySQL容器已删除"
    else
        print_info "MySQL容器不存在"
    fi
}

# 删除数据卷
delete_volumes() {
    print_warning "正在清理数据卷..."
    
    # 清理未使用的卷
    docker volume prune -f
    
    # 清理项目相关的数据
    if [ -d "../data" ]; then
        print_warning "删除数据目录: ../data"
        rm -rf ../data/*
    fi
    
    print_info "数据卷清理完成"
}

# 主函数
main() {
    local remove_container=false
    local remove_volumes=false
    
    # 解析命令行参数
    while [[ $# -gt 0 ]]; do
        case $1 in
            -r|--remove)
                remove_container=true
                shift
                ;;
            -v|--volumes)
                remove_volumes=true
                shift
                ;;
            -h|--help)
                show_usage
                exit 0
                ;;
            *)
                print_error "未知选项: $1"
                show_usage
                exit 1
                ;;
        esac
    done
    
    print_info "开始停止MySQL容器..."
    
    stop_container
    
    if [[ "$remove_container" == true ]]; then
        delete_container
    fi
    
    if [[ "$remove_volumes" == true ]]; then
        delete_volumes
    fi
    
    print_info "操作完成！"
}

# 执行主函数
main "$@"