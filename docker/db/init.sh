#!/bin/bash

# WSL环境下连接localhost的MySQL并初始化litemall数据库
# 作者: AI Assistant
# 日期: $(date +%Y-%m-%d)

# 颜色输出定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 默认配置
DB_HOST="localhost"
DB_PORT="3306"
DB_USER="root"
DB_PASS="1234567kk"
DB_NAME="litemall"

# SQL文件路径
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SQL_DIR="$SCRIPT_DIR/init-sql"
SCHEMA_FILE="$SQL_DIR/litemall_schema.sql"
TABLE_FILE="$SQL_DIR/litemall_table.sql"
DATA_FILE="$SQL_DIR/litemall_data.sql"

# 打印帮助信息
print_help() {
    echo "用法: $0 [选项]"
    echo ""
    echo "WSL环境下连接localhost的MySQL并初始化litemall数据库"
    echo ""
    echo "选项:"
    echo "  -h, --host      MySQL主机地址 (默认: 127.0.0.1)"
    echo "  -P, --port      MySQL端口 (默认: 3306)"
    echo "  -u, --user      MySQL用户名 (默认: root)"
    echo "  -p, --password  MySQL密码"
    echo "  -d, --database  数据库名称 (默认: litemall)"
    echo "  --help          显示此帮助信息"
    echo ""
    echo "示例:"
    echo "  $0 -u root -p 123456"
    echo "  $0 --host localhost --user root --password root"
}

# 检查命令是否存在
check_command() {
    if ! command -v "$1" &> /dev/null; then
        echo -e "${RED}错误: $1 命令未找到${NC}"
        echo "请确保已安装 $1"
        exit 1
    fi
}

# 检查MySQL连接
check_mysql_connection() {
    echo -e "${YELLOW}正在检查MySQL连接...${NC}"
    
    if ! command -v mysql &> /dev/null; then
        echo -e "${RED}错误: mysql 客户端未安装${NC}"
        echo "在Ubuntu/Debian上安装: sudo apt-get install mysql-client"
        echo "在CentOS/RHEL上安装: sudo yum install mysql"
        exit 1
    fi
    
    local cmd="mysql -h $DB_HOST -P $DB_PORT -u $DB_USER"
    if [ -n "$DB_PASS" ]; then
        cmd="$cmd -p$DB_PASS"
    fi
    
    if ! $cmd -e "SELECT 1" &> /dev/null; then
        echo -e "${RED}错误: 无法连接到MySQL服务器${NC}"
        echo "主机: $DB_HOST:$DB_PORT"
        echo "用户: $DB_USER"
        echo ""
        echo "请检查:"
        echo "1. MySQL服务是否在运行"
        echo "2. 防火墙是否允许连接"
        echo "3. 用户名和密码是否正确"
        echo "4. 用户是否有远程访问权限"
        exit 1
    fi
    
    echo -e "${GREEN}MySQL连接成功!${NC}"
}

# 检查SQL文件是否存在
check_sql_files() {
    echo -e "${YELLOW}正在检查SQL文件...${NC}"
    
    local missing_files=()
    
    if [ ! -f "$SCHEMA_FILE" ]; then
        missing_files+=("$SCHEMA_FILE")
    fi
    
    if [ ! -f "$TABLE_FILE" ]; then
        missing_files+=("$TABLE_FILE")
    fi
    
    if [ ! -f "$DATA_FILE" ]; then
        missing_files+=("$DATA_FILE")
    fi
    
    if [ ${#missing_files[@]} -gt 0 ]; then
        echo -e "${RED}错误: 缺少以下SQL文件:${NC}"
        for file in "${missing_files[@]}"; do
            echo "  - $file"
        done
        exit 1
    fi
    
    echo -e "${GREEN}所有SQL文件检查通过!${NC}"
}

# 执行SQL文件
execute_sql_file() {
    local file_path="$1"
    local description="$2"
    
    echo -e "${YELLOW}正在执行: $description${NC}"
    
    local cmd="mysql -h $DB_HOST -P $DB_PORT -u $DB_USER"
    if [ -n "$DB_PASS" ]; then
        cmd="$cmd -p$DB_PASS"
    fi
    
    # 显示SQL文件前几行
    echo -e "${YELLOW}SQL文件预览:${NC}"
    head -5 "$file_path" | sed 's/^/  /'
    
    # 确保使用正确的数据库
    if $cmd -e "USE $DB_NAME" 2>/dev/null; then
        # 执行SQL文件并捕获错误
        local output=$(mktemp)
        if $cmd -D $DB_NAME < "$file_path" 2>&1 | tee "$output"; then
            echo -e "${GREEN}✓ $description 执行成功${NC}"
            rm -f "$output"
            return 0
        else
            echo -e "${RED}✗ $description 执行失败${NC}"
            echo -e "${RED}错误详情:${NC}"
            cat "$output" | tail -10
            rm -f "$output"
            return 1
        fi
    else
        echo -e "${RED}错误: 无法选择数据库 $DB_NAME${NC}"
        return 1
    fi
}

# 验证数据库初始化
validate_database() {
    echo -e "${YELLOW}正在验证数据库初始化...${NC}"
    
    local cmd="mysql -h $DB_HOST -P $DB_PORT -u $DB_USER"
    if [ -n "$DB_PASS" ]; then
        cmd="$cmd -p$DB_PASS"
    fi
    
    # 检查数据库是否存在
    if ! $cmd -e "USE $DB_NAME" 2>/dev/null; then
        echo -e "${RED}错误: 数据库 $DB_NAME 不存在${NC}"
        return 1
    fi
    
    local table_count=$($cmd -D $DB_NAME -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$DB_NAME'" 2>/dev/null)
    
    if [ "$table_count" -gt 0 ]; then
        echo -e "${GREEN}数据库验证成功! 找到 $table_count 个表${NC}"
        
        # 显示主要表的信息
        echo -e "${GREEN}主要数据表:${NC}"
        $cmd -D $DB_NAME -e "SELECT table_name, table_rows FROM information_schema.tables WHERE table_schema='$DB_NAME' AND table_name IN ('litemall_admin', 'litemall_user', 'litemall_goods', 'litemall_order') ORDER BY table_name" 2>/dev/null
    else
        echo -e "${RED}数据库验证失败! 未找到任何表${NC}"
        return 1
    fi
}

# 主函数
main() {
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}  litemall数据库初始化脚本 (WSL版)  ${NC}"
    echo -e "${GREEN}========================================${NC}"
    
    # 检查是否在WSL环境中
    if [[ "$(uname -r)" == *microsoft* ]]; then
        echo -e "${GREEN}检测到WSL环境${NC}"
    else
        echo -e "${YELLOW}警告: 未检测到WSL环境，但仍将继续执行${NC}"
    fi
    
    # 检查必要命令
    check_command "mysql"
    
    # 检查SQL文件
    check_sql_files
    
    # 检查MySQL连接
    check_mysql_connection
    
    # 执行SQL文件
    echo -e "${GREEN}开始执行数据库初始化...${NC}"
    
    local success=true
    
    execute_sql_file "$SCHEMA_FILE" "数据库架构和权限设置" || success=false
    
    if [ "$success" = true ]; then
        execute_sql_file "$TABLE_FILE" "数据表创建" || success=false
    fi
    
    if [ "$success" = true ]; then
        execute_sql_file "$DATA_FILE" "基础数据插入" || success=false
    fi
    
    # 验证结果
    if [ "$success" = true ]; then
        validate_database
        
        echo -e "${GREEN}========================================${NC}"
        echo -e "${GREEN}  数据库初始化成功完成!  ${NC}"
        echo -e "${GREEN}========================================${NC}"
        echo ""
        echo "连接信息:"
        echo "  主机: $DB_HOST:$DB_PORT"
        echo "  数据库: $DB_NAME"
        echo "  用户: admin/admin123 (管理员)"
        echo ""
        echo "测试连接命令:"
        echo "  mysql -h $DB_HOST -P $DB_PORT -u admin -p $DB_NAME"
    else
        echo -e "${RED}========================================${NC}"
        echo -e "${RED}  数据库初始化失败!  ${NC}"
        echo -e "${RED}========================================${NC}"
        echo ""
        echo -e "${YELLOW}请检查以上错误信息并修正后重试${NC}"
        exit 1
    fi
}

# 解析命令行参数
while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--host)
            DB_HOST="$2"
            shift 2
            ;;
        -P|--port)
            DB_PORT="$2"
            shift 2
            ;;
        -u|--user)
            DB_USER="$2"
            shift 2
            ;;
        -p|--password)
            DB_PASS="$2"
            shift 2
            ;;
        -d|--database)
            DB_NAME="$2"
            shift 2
            ;;
        --help)
            print_help
            exit 0
            ;;
        *)
            echo -e "${RED}错误: 未知参数 $1${NC}"
            print_help
            exit 1
            ;;
    esac
done

# 执行主函数
main