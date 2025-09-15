#!/bin/bash

# MySQL容器重启脚本
# 作者: litemall项目
# 描述: 重启MySQL容器

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

# 检查容器是否存在
check_container_exists() {
    if ! docker ps -a --format 'table {{.Names}}' | grep -q '^mysql$'; then
        print_error "MySQL容器不存在，请先运行run.sh脚本创建容器"
        exit 1
    fi
}

# 重启MySQL容器
restart_mysql_container() {
    print_info "正在重启MySQL容器..."
    
    # 检查容器状态
    if docker ps --format 'table {{.Names}}' | grep -q '^mysql$'; then
        print_info "容器正在运行，正在停止..."
        docker stop mysql
        print_info "容器已停止"
    else
        print_info "容器未运行，直接启动..."
    fi
    
    # 启动容器
    docker start mysql
    print_info "容器已启动"
}

# 等待MySQL启动完成
wait_for_mysql() {
    print_info "等待MySQL启动完成..."
    
    local max_attempts=30
    local attempt=1
    
    while [ $attempt -le $max_attempts ]; do
        if docker exec mysql mysqladmin ping -h localhost -u root -proot &> /dev/null; then
            print_info "MySQL重启成功！"
            return 0
        fi
        
        print_info "等待MySQL启动... (尝试 $attempt/$max_attempts)"
        sleep 5
        ((attempt++))
    done
    
    print_error "MySQL启动超时"
    return 1
}

# 显示容器状态
show_container_status() {
    print_info "容器状态信息："
    docker ps --filter "name=mysql" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
}

# 主函数
main() {
    print_info "开始重启MySQL容器..."
    
    check_container_exists
    restart_mysql_container
    wait_for_mysql
    show_container_status
    
    print_info "MySQL容器重启完成！"
}

# 执行主函数
main "$@"