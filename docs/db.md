# 数据库连接配置与Docker部署指南

## 1. 数据库连接配置

### 1.1 核心配置文件

项目数据库连接主要配置在以下文件中：

**文件路径**: `litemall-db/src/main/resources/application-db.yml`

```yaml
spring:
  datasource:
    druid:
      url: jdbc:mysql://127.0.0.1:3306/litemall?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&verifyServerCertificate=false&useSSL=false
      driver-class-name: com.mysql.cj.jdbc.Driver
      username: admin
      password: admin123
      initial-size: 10
      max-active: 50
      min-idle: 10
      max-wait: 60000
      pool-prepared-statements: true
      max-pool-prepared-statement-per-connection-size: 20
      validation-query: SELECT 1 FROM DUAL
      test-on-borrow: false
      test-on-return: false
      test-while-idle: true
      time-between-eviction-runs-millis: 60000
```

### 1.2 数据库初始化

**数据库创建脚本**: `litemall-db/sql/litemall_schema.sql`

```sql
-- 创建数据库和用户
drop database if exists litemall;
drop user if exists 'root'@'%';

-- 支持emoji：使用utf8mb4字符集
create database litemall default character set utf8mb4 collate utf8mb4_unicode_ci;
use litemall;

create user 'root'@'%' identified by 'root';
grant all privileges on litemall.* to 'root'@'%';
flush privileges;
```

**数据表结构**: `litemall-db/sql/litemall_table.sql`
**初始化数据**: `litemall-db/sql/litemall_data.sql`

### 1.3 MyBatis生成器配置

**生成器配置**: `litemall-db/mybatis-generator/generatorConfig.xml`

```xml
<jdbcConnection driverClass="com.mysql.cj.jdbc.Driver"
                connectionURL="jdbc:mysql://127.0.0.1:3306/litemall?useUnicode=true&amp;characterEncoding=UTF-8&amp;serverTimezone=UTC&amp;verifyServerCertificate=false&amp;useSSL=false&amp;nullCatalogMeansCurrent=true"
                userId="root"
                        password="root"/>
```

## 2. Docker部署MySQL

### 2.1 Docker Compose配置

**文件路径**: `docker/docker-compose.yml`

```yaml
version: '3'
services:
  mysql57:
    image: mysql:8
    container_name: mysql
    ports:
      - "3306:3306"
    command:
      --character-set-server=utf8mb4
      --collation-server=utf8mb4_unicode_ci
      --default-authentication-plugin=mysql_native_password
    volumes:
      - ./db/conf.d:/etc/mysql/conf.d
      - ./db/data:/var/lib/mysql
      - ./db/init-sql:/docker-entrypoint-initdb.d
    environment:
      MYSQL_ROOT_PASSWORD: root
    restart: always
```

### 2.2 MySQL配置文件

**文件路径**: `docker/db/conf.d/my.cnf`

```ini
[mysqld]
wait_timeout=1814400
max_allowed_packet = 100M
default-time_zone = '+8:00'
character-set-server=utf8mb4
collation-server=utf8mb4_unicode_ci
```

### 2.3 初始化数据目录

- `docker/db/init-sql/`: 放置数据库初始化SQL文件
- `docker/db/data/`: MySQL数据持久化目录
- `docker/db/conf.d/`: MySQL配置文件目录

## 3. 部署步骤

### 3.1 使用Docker部署

1. **准备初始化数据**
   ```bash
   # 将数据库文件复制到Docker初始化目录
   cp litemall-db/sql/litemall_schema.sql docker/db/init-sql/
   cp litemall-db/sql/litemall_table.sql docker/db/init-sql/
   cp litemall-db/sql/litemall_data.sql docker/db/init-sql/
   ```

2. **启动MySQL容器**
   ```bash
   cd docker
   docker-compose up -d mysql57
   ```

3. **验证数据库**
   ```bash
   docker exec -it mysql mysql -uroot -proot
   -- 在MySQL中执行
   SHOW DATABASES;
   USE litemall;
   SHOW TABLES;
   ```

### 3.2 修改应用配置

当使用Docker部署MySQL时，需要修改数据库连接配置：

**修改文件**: `litemall-db/src/main/resources/application-db.yml`

```yaml
spring:
  datasource:
    druid:
      # 如果MySQL容器和应用程序在同一Docker网络中，可以使用容器名
      url: jdbc:mysql://mysql57:3306/litemall?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&verifyServerCertificate=false&useSSL=false
      # 如果MySQL容器在本地运行，使用127.0.0.1
      # url: jdbc:mysql://127.0.0.1:3306/litemall?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&verifyServerCertificate=false&useSSL=false
      username: admin
      password: admin123
```

### 3.3 完整Docker部署

1. **构建并启动所有服务**
   ```bash
   cd docker
   docker-compose up -d
   ```

2. **查看日志**
   ```bash
   docker-compose logs -f
   ```

3. **访问应用**
   - 前端商城: http://localhost:8083/wx/index/index
   - 管理后台: http://localhost:8083/admin/index/index

## 4. 注意事项

### 4.1 字符集配置
- 数据库使用 `utf8mb4` 字符集以支持emoji表情
- 确保所有配置文件中字符集设置一致

### 4.2 时区配置
- 数据库时区设置为 `Asia/Shanghai` (+8:00)
- 应用程序时区与数据库时区保持一致

### 4.3 权限配置
- 数据库用户 `litemall` 拥有对 `litemall` 数据库的所有权限
- 生产环境应限制用户权限，避免使用 `%` 通配符

### 4.4 端口映射
- MySQL默认端口3306已映射到宿主机
- 如端口冲突，可修改docker-compose.yml中的端口映射

### 4.5 数据持久化
- MySQL数据已挂载到 `docker/db/data/` 目录
- 删除容器不会丢失数据，除非手动删除挂载目录

## 5. 故障排查

### 5.1 连接问题
- 检查MySQL容器是否正常运行
- 验证用户名和密码是否正确
- 确认网络连接和端口映射

### 5.2 字符集问题
- 检查数据库字符集是否为utf8mb4
- 确认表和字段字符集设置正确

### 5.3 时区问题
- 检查MySQL时区设置
- 确认应用程序时区配置

## 6. 相关文件清单

| 文件类型 | 文件路径 | 说明 |
|---------|----------|------|
| 数据库配置 | `litemall-db/src/main/resources/application-db.yml` | 主数据库连接配置 |
| 数据库配置 | `litemall-db/src/main/resources/application.yml` | 激活db配置文件 |
| 数据库脚本 | `litemall-db/sql/litemall_schema.sql` | 数据库和用户创建 |
| 数据库脚本 | `litemall-db/sql/litemall_table.sql` | 数据表结构 |
| 数据库脚本 | `litemall-db/sql/litemall_data.sql` | 初始化数据 |
| Docker配置 | `docker/docker-compose.yml` | Docker服务编排 |
| MySQL配置 | `docker/db/conf.d/my.cnf` | MySQL服务器配置 |
| 生成器配置 | `litemall-db/mybatis-generator/generatorConfig.xml` | MyBatis代码生成器配置 |