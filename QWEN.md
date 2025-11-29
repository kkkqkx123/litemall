# litemall 项目指南

## 项目概述

litemall 是一个开源的小商城系统，采用前后端分离开发模式，包含：

- **后端服务**：基于 Spring Boot 开发
- **管理后台前端**：基于 Vue.js + Element UI 开发
- **微信小程序前端**：基于微信小程序原生框架开发
- **轻商城前端**：基于 Vue.js + Vant 开发

### 项目架构

项目采用多模块 Maven 结构，主要包括：

- `litemall-core`：核心业务模块，包含存储、通知、微信接口等功能
- `litemall-db`：数据库访问模块，使用 MyBatis
- `litemall-wx-api`：小商城后端API
- `litemall-admin-api`：管理后台后端API
- `litemall-all`：打包模块，整合所有后端服务
- `litemall-admin`：管理后台前端
- `litemall-wx`：微信小程序前端
- `litemall-vue`：轻商城前端
- `litemall-all-war`：WAR包打包模块

### 技术栈

- **后端**：Spring Boot 3.5.6, MyBatis, MySQL, Druid 连接池
- **前端**：Vue.js, Element UI, Vant
- **小程序**：微信小程序原生开发
- **构建工具**：Maven, Node.js, npm

## 功能特性

### 小商城功能

- 首页、专题列表与详情
- 商品分类、品牌展示
- 新品首发、人气推荐
- 优惠券、团购功能
- 商品搜索、详情、评价
- 购物车、下单、订单管理
- 个人中心、地址管理、收藏、足迹
- 微信登录、微信支付

### 管理平台功能

- 会员管理（用户、地址、收藏、足迹等）
- 商城管理（品牌、订单、类目、关键词等）
- 商品管理（商品、评论）
- 推广管理（广告、专题、团购）
- 系统管理（管理员、权限、存储、日志）
- 配置管理（商城配置、运费配置）
- 统计报表（用户、订单、商品统计）

## 开发环境搭建

### 环境要求

- JDK 21+
- Maven 3.5+
- Node.js 12+
- MySQL 5.7+
- 微信开发者工具

### 快速启动

#### 0. Docker 环境准备

在开始之前，请确保已安装并启动 Docker 服务。
如果数据库运行在 Docker 容器中，请确保：
1. 已启动 MySQL Docker 容器
2. 容器名称为 mysql （或使用实际的容器名称替换命令中的 mysql）
3. MySQL 服务在容器内正常运行并监听 3306 端口

#### 1. 数据库配置

注意：当前项目使用的数据库位于 Docker 容器中，故应当使用 localhost:3306 访问，数据库用户名为 kkkqkx，密码为 1234567kk。

1. 如果使用 Docker 数据库，请确保 Docker 服务已启动并运行了 litemall 数据库容器。

2. 导入数据库脚本到 Docker 数据库（从项目根目录执行）：
   ```bash
   # 将SQL文件复制到MySQL容器中再执行，假设容器名为 mysql
   docker cp litemall-db/sql/litemall_schema.sql mysql:/tmp/
   docker exec -i mysql mysql -u kkkqkx -p1234567kk litemall < /tmp/litemall_schema.sql
   docker cp litemall-db/sql/litemall_table.sql mysql:/tmp/
   docker exec -i mysql mysql -u kkkqkx -p1234567kk litemall < /tmp/litemall_table.sql
   docker cp litemall-db/sql/litemall_data.sql mysql:/tmp/
   docker exec -i mysql mysql -u kkkqkx -p1234567kk litemall < /tmp/litemall_data.sql
   ```

#### 2. 后端服务启动

1. 安装依赖：
   ```bash
   cd litemall
   mvn install
   ```

2. 编译打包：
   ```bash
   mvn clean package
   ```

3. 启动后端服务：
   ```bash
   java -Dfile.encoding=UTF-8 -jar litemall-all/target/litemall-all-0.1.0-exec.jar
   ```

#### 3. 管理后台前端启动

1. 进入前端目录：
   ```bash
   cd litemall/litemall-admin
   ```

2. 安装依赖并启动：
   ```bash
   npm install --registry=https://registry.npm.taobao.org
   npm run dev
   ```

3. 浏览器访问：`http://localhost:9527`

#### 4. 小商城前端启动

1. 使用微信开发者工具导入 `litemall/litemall-wx` 项目
2. 配置 `litemall-wx/config/api.js` 中的API地址
3. 编译预览

#### 5. 轻商城前端启动

1. 进入前端目录：
   ```bash
   cd litemall/litemall-vue
   ```

2. 安装依赖并启动：
   ```bash
   npm install --registry=https://registry.npm.taobao.org
   npm run dev
   ```

3. 浏览器访问：`http://localhost:6255`

## 项目配置

### 数据库配置

在 `litemall-db/src/main/resources/application-db.yml` 中配置数据库连接：

```yaml
spring:
  datasource:
    druid:
      url: jdbc:mysql://localhost:3306/litemall?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&verifyServerCertificate=false&useSSL=false&useLegacyDatetimeCode=false
      driver-class-name: com.mysql.cj.jdbc.Driver
      username: kkkqkx
      password: 1234567kk
```

注意：当前项目使用的数据库位于 Docker 容器中，故应当使用 localhost:3306 访问，数据库用户名为 kkkqkx，密码为 1234567kk。

### 微信配置

在 `litemall-core/src/main/resources/application-core.yml` 中配置微信相关参数：

```yaml
litemall:
  wx:
    app-id: your-app-id
    app-secret: your-app-secret
    mch-id: your-mch-id
    mch-key: your-mch-key
    notify-url: https://your-domain.com/wx/order/pay-notify
```

## 开发规范

### 代码风格

- Java 代码使用 IDEA 默认格式化规则
- Vue 前端代码使用 ESLint 进行代码检查
- 小程序代码使用微信开发者工具默认格式化

### 项目管理

- 项目使用 Git 进行版本控制
- 采用多模块 Maven 管理
- 支持多环境配置（开发、测试、生产）

### API 设计

后端 API 采用 RESTful 风格，响应格式统一：

```json
{
  "errno": 0,
  "errmsg": "成功",
  "data": {}
}
```

错误码说明：
- 4xx: 前端错误
- 5xx: 后端系统错误
- 6xx: 管理后台业务错误码
- 7xx: 小商城业务错误码

## 部署说明

### 单机部署

1. 打包项目：
   ```bash
   mvn clean package
   ```

2. 上传到服务器并解压

3. 导入数据库：
   ```bash
   mysql -u root -p < litemall.sql
   ```

4. 启动服务：
   ```bash
   nohup java -jar litemall-all-0.1.0-exec.jar > litemall.log 2>&1 &
   ```

### Docker 部署

项目支持 Docker 部署，具体可参考 `docker` 目录下的部署脚本。

## 项目特点

- 数据库设计简单，无外键约束，便于修改
- 支持多种存储方式（本地、阿里云、腾讯云、七牛云）
- 支持短信、邮件、微信模板等多种通知方式
- 支持微信登录、支付等微信生态功能
- 权限管理系统完善
- 支持分布式部署方案
