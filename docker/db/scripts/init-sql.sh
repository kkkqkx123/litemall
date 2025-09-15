#!/bin/bash

# MySQL初始化SQL执行脚本
# 作者: litemall项目
# 描述: 手动执行初始化SQL文件

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 默认配置
DEFAULT_HOST="localhost"
DEFAULT_PORT="3306"
DEFAULT_USER="root"
DEFAULT_PASSWORD="root"
DEFAULT_DATABASE="litemall"

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

print_debug() {
    echo -e "${BLUE}[DEBUG]${NC} $1"
}

# 显示使用帮助
show_usage() {
    echo "使用方法: $0 [选项]"
    echo ""
    echo "选项:"
    echo "  -h, --host      MySQL主机地址 (默认: $DEFAULT_HOST)"
    echo "  -P, --port      MySQL端口 (默认: $DEFAULT_PORT)"
    echo "  -u, --user      MySQL用户名 (默认: $DEFAULT_USER)"
    echo "  -p, --password  MySQL密码 (默认: $DEFAULT_PASSWORD)"
    echo "  -d, --database  数据库名 (默认: $DEFAULT_DATABASE)"
    echo "  -f, --file      指定要执行的SQL文件"
    echo "  -a, --all       执行所有初始化SQL文件"
    echo "  --help          显示此帮助信息"
    echo ""
    echo "示例:"
    echo "  $0 --all                    # 执行所有初始化SQL文件"
    echo "  $0 -f litemall_schema.sql   # 执行指定SQL文件"
    echo "  $0 -h 192.168.1.100 -u admin -p secret --all"
}

# 检查MySQL连接
check_mysql_connection() {
    print_info "检查MySQL连接..."
    
    if ! command -v mysql &> /dev/null; then
        print_error "未找到mysql命令，请确保MySQL客户端已安装"
        exit 1
    fi
    
    if ! mysql -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$MYSQL_USER" -p"$MYSQL_PASSWORD" -e "SELECT 1" &> /dev/null; then
        print_error "无法连接到MySQL服务器: $MYSQL_HOST:$MYSQL_PORT"
        exit 1
    fi
    
    print_info "MySQL连接成功"
}

# 检查数据库是否存在
check_database_exists() {
    local db_exists=$(mysql -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$MYSQL_USER" -p"$MYSQL_PASSWORD" -N -e "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name='$MYSQL_DATABASE'" 2>/dev/null)
    
    if [ "$db_exists" -eq 0 ]; then
        print_warning "数据库 $MYSQL_DATABASE 不存在，正在创建..."
        mysql -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$MYSQL_USER" -p"$MYSQL_PASSWORD" -e "CREATE DATABASE IF NOT EXISTS \`$MYSQL_DATABASE\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci" 2>/dev/null
        print_info "数据库 $MYSQL_DATABASE 创建成功"
    else
        print_info "数据库 $MYSQL_DATABASE 已存在"
    fi
}

# 执行单个SQL文件
execute_sql_file() {
    local sql_file="$1"
    
    if [ ! -f "$sql_file" ]; then
        print_error "SQL文件不存在: $sql_file"
        return 1
    fi
    
    print_info "执行SQL文件: $(basename "$sql_file")"
    
    if mysql -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE" < "$sql_file"; then
        print_info "SQL文件执行成功: $(basename "$sql_file")"
    else
        print_error "SQL文件执行失败: $(basename "$sql_file")"
        return 1
    fi
}

# 执行所有初始化SQL文件
execute_all_sql_files() {
    local init_sql_dir="../init-sql"
    
    if [ ! -d "$init_sql_dir" ]; then
        print_error "初始化SQL目录不存在: $init_sql_dir"
        exit 1
    fi
    
    print_info "开始执行所有初始化SQL文件..."
    
    # 按顺序执行SQL文件
    local sql_files=(
        "litemall_schema.sql"
        "litemall_table.sql"
        "litemall_data.sql"
    )
    
    for sql_file in "${sql_files[@]}"; do
        local full_path="$init_sql_dir/$sql_file"
        if [ -f "$full_path" ]; then
            execute_sql_file "$full_path"
        else
            print_warning "SQL文件不存在: $sql_file"
        fi
    done
    
    print_info "所有初始化SQL文件执行完成"
}

# 显示数据库状态
show_database_status() {
    print_info "数据库状态信息："
    
    # 显示数据库列表
    print_info "数据库列表："
    mysql -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$MYSQL_USER" -p"$MYSQL_PASSWORD" -e "SHOW DATABASES" 2>/dev/null
    
    # 显示当前数据库的表
    print_info "数据库 $MYSQL_DATABASE 中的表："
    mysql -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE" -e "SHOW TABLES" 2>/dev/null | head -20
    
    # 显示表数量
    local table_count=$(mysql -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE" -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$MYSQL_DATABASE'" 2>/dev/null)
    print_info "数据库 $MYSQL_DATABASE 中共有 $table_count 张表"
}

# 解析命令行参数
parse_arguments() {
    # 设置默认值
    MYSQL_HOST="$DEFAULT_HOST"
    MYSQL_PORT="$DEFAULT_PORT"
    MYSQL_USER="$DEFAULT_USER"
    MYSQL_PASSWORD="$DEFAULT_PASSWORD"
    MYSQL_DATABASE="$DEFAULT_DATABASE"
    EXECUTE_ALL=false
    SPECIFIC_FILE=""
    
    while [[ $# -gt 0 ]]; do
        case $1 in
            -h|--host)
                MYSQL_HOST="$2"
                shift 2
                ;;
            -P|--port)
                MYSQL_PORT="$2"
                shift 2
                ;;
            -u|--user)
                MYSQL_USER="$2"
                shift 2
                ;;
            -p|--password)
                MYSQL_PASSWORD="$2"
                shift 2
                ;;
            -d|--database)
                MYSQL_DATABASE="$2"
                shift 2
                ;;
            -f|--file)
                SPECIFIC_FILE="$2"
                shift 2
                ;;
            -a|--all)
                EXECUTE_ALL=true
                shift
                ;;
            --help)
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
}

# 主函数
main() {
    print_info "MySQL初始化SQL执行脚本"
    
    parse_arguments "$@"
    
    if [[ $# -eq 0 ]]; then
        show_usage
        exit 1
    fi
    
    print_info "连接信息: $MYSQL_HOST:$MYSQL_PORT, 用户: $MYSQL_USER, 数据库: $MYSQL_DATABASE"
    
    check_mysql_connection
    check_database_exists
    
    if [[ -n "$SPECIFIC_FILE" ]]; then
        execute_sql_file "../init-sql/$SPECIFIC_FILE"
    elif [[ "$EXECUTE_ALL" == true ]]; then
        execute_all_sql_files
    else
        print_error "请指定要执行的SQL文件或使用--all参数"
        show_usage
        exit 1
    fi
    
    show_database_status
    
    print_info "SQL执行完成！"
}

# 执行主函数
main "$@"