# Litemall项目启动指南

## 项目概述
Litemall是一个基于Spring Boot的电商平台项目，包含管理后台、微信小程序、Vue前端等多个模块。

## 环境要求

### 必需环境
- **Java**: JDK 8+
- **Maven**: 3.6+
- **MySQL**: 5.7+ 或 8.0+
- **Node.js**: 14+ (用于前端开发)

### 可选环境
- **微信开发者工具**: 用于微信小程序开发
- **IDE**: IntelliJ IDEA、Eclipse等

## 数据库配置

### 1. 数据库初始化
```sql
-- 创建数据库
CREATE DATABASE IF NOT EXISTS litemall DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE litemall;

-- 导入数据
source litemall-db/sql/litemall_schema.sql
source litemall-db/sql/litemall_table.sql
source litemall-db/sql/litemall_data.sql
```

### 2. 数据库配置修改
修改配置文件：`litemall-db/src/main/resources/application-db.yml`

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/litemall?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC
    username: root
    password: 1234567kk  # 修改为你的MySQL密码
    driver-class-name: com.mysql.cj.jdbc.Driver
```

## 启动方式

### 方式一：一键启动脚本（推荐）

#### 1. 启动Web管理后台（包含后端+前端）
```powershell
# Windows
.\scripts\start-web.ps1

# Linux/Mac
./scripts/start-web.sh
```

#### 2. 启动微信小程序后端
```powershell
# Windows
.\scripts\start-wx.ps1

# Linux/Mac
./scripts/start-wx.sh
```

### 方式二：手动启动

#### 1. 构建项目
```bash
# 跳过测试构建
mvn clean install -DskipTests

# 完整构建
mvn clean install
```

#### 2. 启动后端服务
```bash
# 启动所有服务（包含管理后台和微信API）
java -jar litemall-all/target/litemall-all-0.1.0-exec.jar

# 指定端口启动
java -jar litemall-all/target/litemall-all-0.1.0-exec.jar --server.port=8080
```

#### 3. 启动前端服务（可选）

##### 管理后台前端（Vue）
```bash
cd litemall-vue
npm install
npm run dev
```

> **注意**：如果使用`start-web.ps1`脚本启动，前端服务会自动启动，无需手动启动。

##### 微信小程序前端
1. 安装微信开发者工具
2. 打开微信开发者工具
3. 导入项目：`litemall-wx`目录
4. 配置项目：填写自己的AppID（测试可使用测试号）
5. 编译运行

## 服务地址

### 后端服务
- **主服务**: http://localhost:8080
- **管理后台API**: http://localhost:8080/admin
- **微信小程序API**: http://localhost:8080/wx
- **Swagger文档**: http://localhost:8080/swagger-ui.html
- **H2控制台**: http://localhost:8080/h2-console

### 前端服务
- **管理后台**: http://localhost:9527
- **微信小程序**: 微信开发者工具中运行

### 测试账号
- **管理员**: admin/123456
- **普通用户**: user/123456

## 常见问题

### 1. 端口占用问题
如果端口被占用，可以修改启动端口：
```bash
# 修改后端端口
java -jar litemall-all/target/litemall-all-0.1.0-exec.jar --server.port=8081

# 修改前端端口（Vue）
# 修改 litemall-vue/vue.config.js
module.exports = {
  devServer: {
    port: 9528
  }
}
```

### 2. 数据库连接失败
- 检查MySQL服务是否启动
- 检查数据库配置是否正确
- 检查用户名密码是否正确
- 检查数据库是否已创建

### 3. 构建失败
- 检查Maven配置：mvn -v
- 检查网络连接（下载依赖）
- 清理后重试：mvn clean install -DskipTests

### 4. 微信小程序无法连接
- 检查后端服务是否启动
- 检查微信开发者工具配置
- 检查API地址配置：litemall-wx/config/api.js

## 项目结构

```
litemall/
├── litemall-admin/          # 管理后台前端（已废弃，使用litemall-vue）
├── litemall-admin-api/      # 管理后台API
├── litemall-all/           # 打包所有模块的启动器
├── litemall-core/          # 核心模块
├── litemall-db/            # 数据库模块
├── litemall-vue/           # Vue管理后台前端
├── litemall-wx/            # 微信小程序前端
├── litemall-wx-api/        # 微信小程序API
├── renard-wx/              # 另一个微信小程序版本
└── scripts/                # 启动脚本
```

## 开发模式

### 热部署开发
```bash
# 启动后端（支持热部署）
mvn spring-boot:run -pl litemall-all

# 启动前端（热重载）
cd litemall-vue
npm run dev
```

### 调试模式
```bash
# 调试启动
java -jar -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005 litemall-all/target/litemall-all-0.1.0-exec.jar
```

## 部署说明

### 生产环境部署
1. 修改配置文件中的生产环境参数
2. 使用生产环境配置启动：
   ```bash
   java -jar litemall-all/target/litemall-all-0.1.0-exec.jar --spring.profiles.active=prod
   ```
3. 配置反向代理（Nginx）
4. 配置HTTPS证书

### Docker部署
```bash
# 使用Docker Compose
cd docker
docker-compose up -d
```

## 联系与支持
- 项目地址：https://github.com/linlinjava/litemall
- 文档地址：https://linlinjava.gitbook.io/litemall