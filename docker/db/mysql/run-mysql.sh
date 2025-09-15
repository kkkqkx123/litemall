#!/bin/bash

# MySQL 8 Docker 独立运行脚本
# 描述: 使用官方mysql:8镜像运行MySQL容器

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
}

# 创建必要的目录
create_directories() {
    print_info "创建必要的目录..."
    mkdir -p ./data
    mkdir -p ./logs
    mkdir -p ./conf
    
    # 设置目录权限
    chmod -R 755 ./data
    chmod -R 755 ./logs
    chmod -R 755 ./conf
}

# 运行MySQL容器
run_mysql_container() {
    print_info "运行MySQL容器..."
    
    # 检查容器是否已存在
    if docker ps -a --format 'table {{.Names}}' | grep -q '^mysql$'; then
        print_warning "检测到已存在的MySQL容器"
        
        # 检查容器状态
        if docker ps --format 'table {{.Names}}' | grep -q '^mysql$'; then
            print_warning "MySQL容器正在运行，正在停止..."
            docker stop mysql
        fi
        
        print_info "启动现有容器..."
        docker start mysql
        return 0
    fi
    
    # 运行新容器
    docker run -d \
        --name mysql \
        --restart=always \
        -p 3306:3306 \
        -v "$(pwd)/data:/var/lib/mysql" \
        -v "$(pwd)/conf:/etc/mysql/conf.d" \
        -v "$(pwd)/logs:/var/log/mysql" \
        -e MYSQL_ROOT_PASSWORD=root \
        -e MYSQL_USER=kkkqkx \
        -e MYSQL_PASSWORD=1234567kk \
        mysql:8
    
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

# 显示使用信息
show_usage() {
    print_info "MySQL 8 Docker 独立运行脚本"
    print_info "用法:"
    print_info "  $0 [start|stop|restart|status]"
    print_info ""
    print_info "命令:"
    print_info "  start   - 启动MySQL容器"
    print_info "  stop    - 停止MySQL容器"
    print_info "  restart - 重启MySQL容器"
    print_info "  status  - 查看容器状态"
    print_info ""
    print_info "默认: 不带参数时执行start"
}

# 停止容器
stop_mysql() {
    print_info "停止MySQL容器..."
    if docker ps --format 'table {{.Names}}' | grep -q '^mysql$'; then
        docker stop mysql
        print_info "MySQL容器已停止"
    else
        print_warning "MySQL容器未运行"
    fi
}

# 重启容器
restart_mysql() {
    print_info "重启MySQL容器..."
    if docker ps -a --format 'table {{.Names}}' | grep -q '^mysql$'; then
        docker restart mysql
        print_info "MySQL容器已重启"
    else
        print_warning "MySQL容器不存在"
    fi
}

# 查看状态
show_status() {
    print_info "MySQL容器状态:"
    if docker ps -a --format 'table {{.Names}}' | grep -q '^mysql$'; then
        docker ps -a --filter name=mysql --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
    else
        print_warning "MySQL容器不存在"
    fi
}

# 主函数
main() {
    local command=${1:-start}
    
    case $command in
        start)
            check_docker
            create_directories
            run_mysql_container
            wait_for_mysql
            
            print_info "MySQL容器部署完成！"
            print_info "数据库信息："
            print_info "  主机: localhost"
            print_info "  端口: 3306"
            print_info "  用户名: kkkqkx"
            print_info "  密码: 1234567kk"
            print_info "  数据库: litemall"
            ;;
        stop)
            stop_mysql
            ;;
        restart)
            restart_mysql
            ;;
        status)
            show_status
            ;;
        *)
            show_usage
            ;;
    esac
}

# 执行主函数
main "$@"