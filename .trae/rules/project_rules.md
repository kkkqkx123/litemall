始终实时更新todo list

后端日志查看litemall-all\logs\log.log

sql测试数据由js脚本生成

---
当前项目使用的数据库位于docker。
注意：本地没有mysql命令行程序，执行命令需要docker exec
username:  root
password:  root

操作数据库：
docker exec mysql mysql -uroot -proot -e "sql语句"

操作当前项目的litemall数据库：
docker exec mysql mysql -uroot -proot litemall -e "sql语句"

前端获取jwt token：
curl -X POST "http://localhost:8080/admin/auth/login" -H "Content-Type: application/json" -d '{"username":"admin123","password":"admin123"}' 

通过jwt访问后端：
curl -X GET "http://localhost:8080/admin/stat/order/enhanced?month=1" -H "Content-Type: application/json" -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbjEyMyIsImlhdCI6MTc2NDYzOTEwMywiZXhwIjoxNzY0NzI1NTAzfQ.bDB4uBs62J2uImb315tbTIZPRca1UK4EGPR5g7C8kpM"
---

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


---

**重要：修改后端代码后必须重新编译！**

```powershell
# 在项目根目录执行
cd litemall
mvn clean package -DskipTests
```

### 3. 启动应用
使用编译后的JAR文件启动：

```powershell
# 进入litemall-all目录
cd litemall-all

# 启动应用
java -jar target/litemall-all-0.1.0-exec.jar
```

## 常见问题和解决方案

### 问题：代码修改未生效
**原因**：应用通过Spring Boot Maven插件运行，使用的是旧的编译版本

**解决方案**：
1. 停止当前应用
2. 执行 `mvn clean package -DskipTests`
3. 使用新编译的JAR文件启动