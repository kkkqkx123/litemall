# MySQL 独立容器部署

本目录包含独立的MySQL容器部署配置，用于litemall项目的数据库服务。

## 目录结构

```
docker/db/
├── Dockerfile              # MySQL自定义镜像构建文件
├── conf.d/                 # MySQL配置文件目录
│   └── my.cnf             # MySQL配置文件
├── data/                   # MySQL数据目录
├── init-sql/               # 初始化SQL文件
│   ├── README.md          # SQL文件说明
│   ├── litemall_schema.sql # 数据库schema
│   ├── litemall_table.sql  # 数据表结构
│   └── litemall_data.sql   # 初始化数据
└── scripts/               # 脚本文件目录
    ├── run.sh             # 运行MySQL容器
    ├── restart.sh         # 重启MySQL容器
    ├── stop.sh            # 停止MySQL容器
    ├── logs.sh            # 查看容器日志
    └── init-sql.sh        # 手动执行初始化SQL
```

## 使用方法

### 1. 运行MySQL容器

```bash
# 进入scripts目录
cd docker/db/scripts

# 运行MySQL容器（首次运行会自动构建镜像）
./run.sh
```

### 2. 重启MySQL容器

```bash
./restart.sh
```

### 3. 停止MySQL容器

```bash
# 仅停止容器
./stop.sh

# 停止并删除容器
./stop.sh -r

# 停止、删除容器并清理数据
./stop.sh -r -v
```

### 4. 查看容器日志

```bash
# 查看最后50行日志
./logs.sh

# 实时跟踪日志
./logs.sh -f

# 查看最后100行日志
./logs.sh -n 100

# 查看最近10分钟的日志
./logs.sh --since 10m
```

### 5. 执行初始化SQL

```bash
# 执行所有初始化SQL文件
./init-sql.sh --all

# 执行指定的SQL文件
./init-sql.sh -f litemall_schema.sql

# 使用自定义连接信息
./init-sql.sh -h localhost -P 3306 -u root -p root --all
```

## 配置信息

### 默认配置

- **数据库主机**: localhost
- **数据库端口**: 3306
- **数据库名**: litemall
- **root用户**: root
- **root密码**: root
- **应用用户**: litemall
- **应用密码**: root

### 环境变量

可以在运行脚本时通过环境变量覆盖默认配置：

```bash
export MYSQL_ROOT_PASSWORD=your_password
export MYSQL_DATABASE=your_database
export MYSQL_USER=your_user
export MYSQL_PASSWORD=your_password

./run.sh
```

## 数据持久化

MySQL数据将持久化存储在以下目录：

- **数据文件**: `docker/db/data/`
- **配置文件**: `docker/db/conf.d/`
- **日志文件**: `docker/db/logs/` (自动创建)

## 故障排除

### 1. 容器启动失败

检查日志：
```bash
./logs.sh -f
```

检查端口占用：
```bash
# Linux/Mac
netstat -tulnp | grep 3306

# Windows
netstat -ano | findstr 3306
```

### 2. 连接问题

测试连接：
```bash
mysql -h localhost -P 3306 -u root -p
```

### 3. 数据初始化问题

手动重新执行初始化SQL：
```bash
./init-sql.sh --all
```

## 注意事项

1. **数据备份**: 定期备份 `docker/db/data/` 目录中的数据
2. **端口冲突**: 如果3306端口已被占用，需要修改脚本中的端口映射
3. **权限问题**: 确保脚本有执行权限 (`chmod +x *.sh`)
4. **Docker要求**: 需要安装Docker和Docker Compose

## 高级配置

### 自定义MySQL配置

编辑 `docker/db/conf.d/my.cnf` 文件来修改MySQL配置。

### 自定义Docker镜像

编辑 `docker/db/Dockerfile` 文件来自定义MySQL镜像。

### 环境变量配置

创建 `.env` 文件来设置环境变量：
```bash
# docker/db/.env
MYSQL_ROOT_PASSWORD=your_custom_password
MYSQL_DATABASE=your_custom_db
```