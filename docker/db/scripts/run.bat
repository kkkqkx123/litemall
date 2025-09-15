@echo off
REM MySQL容器运行脚本（Windows版）
REM 作者: litemall项目
REM 描述: 构建并运行MySQL容器

setlocal enabledelayedexpansion

REM 颜色定义
set "RED=[91m"
set "GREEN=[92m"
set "YELLOW=[93m"
set "NC=[0m"

REM 打印信息函数
:print_info
echo %GREEN%[INFO]%NC% %~1
goto :eof

:print_warning
echo %YELLOW%[WARNING]%NC% %~1
goto :eof

:print_error
echo %RED%[ERROR]%NC% %~1
goto :eof

REM 检查Docker是否安装
:check_docker
where docker >nul 2>nul
if errorlevel 1 (
    call :print_error "Docker未安装，请先安装Docker"
    exit /b 1
)

where docker-compose >nul 2>nul
if errorlevel 1 (
    call :print_error "Docker Compose未安装，请先安装Docker Compose"
    exit /b 1
)
goto :eof

REM 创建必要的目录
:create_directories
call :print_info "创建必要的目录..."
if not exist "..\data" mkdir "..\data"
if not exist "..\logs" mkdir "..\logs"
goto :eof

REM 构建MySQL镜像
:build_mysql_image
call :print_info "构建MySQL镜像..."
docker build -t litemall-mysql:8.0 -f ..\Dockerfile ..\.
if errorlevel 1 (
    call :print_error "MySQL镜像构建失败"
    exit /b 1
)
call :print_info "MySQL镜像构建完成"
goto :eof

REM 运行MySQL容器
:run_mysql_container
call :print_info "运行MySQL容器..."

REM 检查容器是否已存在
docker ps -a --format "table {{.Names}}" | findstr "^mysql$" >nul
if not errorlevel 1 (
    call :print_warning "MySQL容器已存在，正在删除..."
    docker rm -f mysql
)

REM 运行新容器
docker run -d ^
    --name mysql ^
    --restart=always ^
    -p 3306:3306 ^
    -v "%cd%\..\data:/var/lib/mysql" ^
    -v "%cd%\..\conf.d:/etc/mysql/conf.d" ^
    -v "%cd%\..\init-sql:/docker-entrypoint-initdb.d" ^
    -v "%cd%\..\logs:/var/log/mysql" ^
    -e MYSQL_ROOT_PASSWORD=root ^
    -e MYSQL_DATABASE=litemall ^
    -e MYSQL_USER=root ^
    -e MYSQL_PASSWORD=root ^
    litemall-mysql:8.0

if errorlevel 1 (
    call :print_error "MySQL容器启动失败"
    exit /b 1
)

call :print_info "MySQL容器启动完成"
goto :eof

REM 等待MySQL启动完成
:wait_for_mysql
call :print_info "等待MySQL启动完成..."

set max_attempts=30
set attempt=1

:wait_loop
if !attempt! gtr !max_attempts! (
    call :print_error "MySQL启动超时"
    exit /b 1
)

timeout /t 5 /nobreak >nul

REM 检查MySQL是否启动成功
docker exec mysql mysqladmin ping -h localhost -u root -proot >nul 2>nul
if errorlevel 1 (
    call :print_info "等待MySQL启动... (尝试 !attempt!/!max_attempts!)"
    set /a attempt+=1
    goto wait_loop
)

call :print_info "MySQL启动成功！"
goto :eof

REM 主程序
:main
call :print_info "开始部署MySQL容器..."

call :check_docker
call :create_directories
call :build_mysql_image
call :run_mysql_container
call :wait_for_mysql

call :print_info "MySQL容器部署完成！"
call :print_info "数据库信息："
call :print_info "  主机: localhost"
call :print_info "  端口: 3306"
call :print_info "  用户名: root"
call :print_info "  密码: root"
call :print_info "  数据库: litemall"

goto :eof

REM 执行主程序
call :main
pause