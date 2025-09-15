#!/bin/bash

# MySQL容器日志查看脚本
# 作者: litemall项目
# 描述: 查看MySQL容器的日志

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
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
    echo "  -f, --follow    实时跟踪日志输出"
    echo "  -n, --lines     显示最后N行日志 (默认: 50)"
    echo "  -t, --tail      等同于 --lines"
    echo "  --since         显示从指定时间开始的日志 (例如: 10m, 1h, 2023-01-01T00:00:00)"
    echo "  --until         显示到指定时间结束的日志"
    echo "  -h, --help      显示此帮助信息"
    echo ""
    echo "示例:"
    echo "  $0                    # 显示最后50行日志"
    echo "  $0 -f                 # 实时跟踪日志"
    echo "  $0 -n 100             # 显示最后100行日志"
    echo "  $0 --since 10m        # 显示最近10分钟的日志"
}

# 检查容器是否存在
check_container_exists() {
    if ! docker ps -a --format 'table {{.Names}}' | grep -q '^mysql$'; then
        print_error "MySQL容器不存在"
        exit 1
    fi
}

# 显示容器状态
show_container_status() {
    print_info "容器状态信息："
    docker ps --filter "name=mysql" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
    echo ""
}

# 显示容器统计信息
show_container_stats() {
    print_info "容器统计信息："
    
    # 显示资源使用情况
    if docker ps --filter "name=mysql" --format "{{.Names}}" | grep -q "mysql"; then
        docker stats --no-stream --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.NetIO}}\t{{.BlockIO}}" mysql
    else
        print_warning "容器未运行，无法显示统计信息"
    fi
    
    echo ""
}

# 主函数
main() {
    local follow=false
    local lines=50
    local since=""
    local until=""
    
    # 解析命令行参数
    while [[ $# -gt 0 ]]; do
        case $1 in
            -f|--follow)
                follow=true
                shift
                ;;
            -n|--lines|-t|--tail)
                lines="$2"
                shift 2
                ;;
            --since)
                since="$2"
                shift 2
                ;;
            --until)
                until="$2"
                shift 2
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
    
    check_container_exists
    
    print_info "MySQL容器日志信息"
    echo "================================"
    
    show_container_status
    show_container_stats
    
    # 构建docker logs命令
    local log_cmd="docker logs"
    
    if [[ "$follow" == true ]]; then
        log_cmd="$log_cmd -f"
    fi
    
    if [[ -n "$lines" ]]; then
        log_cmd="$log_cmd --tail $lines"
    fi
    
    if [[ -n "$since" ]]; then
        log_cmd="$log_cmd --since $since"
    fi
    
    if [[ -n "$until" ]]; then
        log_cmd="$log_cmd --until $until"
    fi
    
    log_cmd="$log_cmd mysql"
    
    print_info "执行命令: $log_cmd"
    echo "================================"
    
    # 执行日志命令
    eval $log_cmd
}

# 执行主函数
main "$@"