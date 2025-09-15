#!/bin/bash

# MySQL容器运行脚本
# 作者: litemall项目
# 描述: 构建并运行MySQL容器

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

# 检查Docker是否安装
check_docker() {
    if ! command -v docker &> /dev/null; then
        print_error "Docker未安装，请先安装Docker"
        exit 1
    fi
    
    if ! command -v docker-compose &> /dev/null; then
        print_error "Docker Compose未安装，请先安装Docker Compose"
        exit 1
    fi
}

# 创建必要的目录
create_directories() {
    print_info "创建必要的目录..."
    mkdir -p ../data
    mkdir -p ../logs
    
    # 设置目录权限
    chmod -R 755 ../data
    chmod -R 755 ../logs
}

# 构建MySQL镜像
build_mysql_image() {
    print_info "构建MySQL镜像..."
    docker build -t litemall-mysql:8.0 -f ../Dockerfile ..
    print_info "MySQL镜像构建完成"
}

# 运行MySQL容器
run_mysql_container() {
    print_info "运行MySQL容器..."
    
    # 检查容器是否已存在
    if docker ps -a --format 'table {{.Names}}' | grep -q '^mysql$'; then
        print_warning "MySQL容器已存在，正在删除..."
        docker rm -f mysql
    fi
    
    # 运行新容器
    docker run -d \
        --name mysql \
        --restart=always \
        -p 3306:3306 \
        -v "$(pwd)/../data:/var/lib/mysql" \
        -v "$(pwd)/../conf.d:/etc/mysql/conf.d" \
        -v "$(pwd)/../init-sql:/docker-entrypoint-initdb.d" \
        -v "$(pwd)/../logs:/var/log/mysql" \
        -e MYSQL_ROOT_PASSWORD=root \
    -e MYSQL_DATABASE=litemall \
    -e MYSQL_USER=root \
    -e MYSQL_PASSWORD=root \
        litemall-mysql:8.0
    
    print_info "MySQL容器启动完成"
}

# 等待MySQL启动完成
wait_for_mysql() {
    print_info "等待MySQL启动完成..."
    
    local max_attempts=30
    local attempt=1
    
    while [ $attempt -le $max_attempts ]; do
        if docker exec mysql mysqladmin ping -h localhost -u root -proot &> /dev/null; then
            print_info "MySQL启动成功！"
            return 0
        fi
        
        print_info "等待MySQL启动... (尝试 $attempt/$max_attempts)"
        sleep 5
        ((attempt++))
    done
    
    print_error "MySQL启动超时"
    return 1
}

# 主函数
main() {
    print_info "开始部署MySQL容器..."
    
    check_docker
    create_directories
    build_mysql_image
    run_mysql_container
    wait_for_mysql
    
    print_info "MySQL容器部署完成！"
    print_info "数据库信息："
    print_info "  主机: localhost"
    print_info "  端口: 3306"
    print_info "  用户名: root"
    print_info "  密码: root"
    print_info "  数据库: litemall"
}

# 执行主函数
main "$@"