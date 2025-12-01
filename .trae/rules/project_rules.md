始终实时更新todo list

后端日志查看litemall-all\logs\log.log

---
当前项目使用的数据库位于docker。
注意：本地没有mysql命令行程序，执行命令需要docker exec
username:  root
password:  root

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