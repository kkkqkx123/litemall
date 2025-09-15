# Litemall前后端启动脚本使用说明

## 概述

本目录包含两个PowerShell脚本，用于快速启动litemall项目的前后端服务：

1. `start-web.ps1` - 启动Web商城前后端服务
2. `start-wx.ps1` - 启动微信小程序前后端服务

## 脚本说明

### 1. start-web.ps1 - Web商城启动脚本

#### 功能
- 启动Spring Boot后端服务 (litemall-all)
- 启动Vue前端开发服务器 (litemall-vue)
- 自动构建缺失的组件
- 支持自定义端口配置

#### 使用方法
```powershell
# 基本使用
.\start-web.ps1

# 自定义端口
.\start-web.ps1 -BackendPort 8081 -FrontendPort 3000

# 跳过构建步骤
.\start-web.ps1 -SkipBuild

# 只启动后端
.\start-web.ps1 -SkipFrontend

# 只启动前端
.\start-web.ps1 -SkipBackend

# 指定环境配置
.\start-web.ps1 -Profile prod
```

#### 参数说明
| 参数 | 默认值 | 说明 |
|------|--------|------|
| Profile | dev | 环境配置 (dev/test/prod) |
| BackendPort | 8080 | 后端服务端口 |
| FrontendPort | 9527 | 前端服务端口 |
| SkipBuild | false | 跳过构建步骤 |
| SkipFrontend | false | 跳过前端启动 |
| SkipBackend | false | 跳过后端启动 |

#### 访问地址
- 后端服务: http://localhost:8080
- 管理后台: http://localhost:8080/admin
- API文档: http://localhost:8080/swagger-ui.html
- 前端服务: http://localhost:9527

### 2. start-wx.ps1 - 微信小程序启动脚本

#### 功能
- 启动微信小程序后端服务
- 自动打开微信开发者工具
- 检查数据库连接状态
- 支持自定义端口配置

#### 使用方法
```powershell
# 基本使用
.\start-wx.ps1

# 自定义端口
.\start-wx.ps1 -WxApiPort 8083

# 跳过构建步骤
.\start-wx.ps1 -SkipBuild

# 只启动后端
.\start-wx.ps1 -SkipWxFrontend

# 只启动前端
.\start-wx.ps1 -SkipWxBackend

# 指定环境配置
.\start-wx.ps1 -Profile prod
```

#### 参数说明
| 参数 | 默认值 | 说明 |
|------|--------|------|
| Profile | dev | 环境配置 (dev/test/prod) |
| WxApiPort | 8082 | 微信小程序后端端口 |
| SkipBuild | false | 跳过构建步骤 |
| SkipWxBackend | false | 跳过微信小程序后端启动 |
| SkipWxFrontend | false | 跳过微信小程序前端启动 |

#### 访问地址
- 微信小程序后端: http://localhost:8082
- 微信小程序API: http://localhost:8082/wx
- 微信小程序项目: litemall-wx目录

## 环境要求

### 必需软件
1. **Java 8+** - 运行Spring Boot后端
2. **Maven 3.6+** - 构建后端项目
3. **Node.js 14+** - 运行前端开发服务器
4. **npm** - 管理前端依赖

### 微信小程序额外要求
1. **微信开发者工具** - 开发和调试微信小程序
2. **微信账号** - 用于小程序开发和测试

### 数据库要求
1. **MySQL 5.7+** 或 **MySQL 8.0**
2. **Docker** (可选) - 使用Docker运行数据库

## 数据库配置

### 使用Docker启动数据库
```bash
cd docker
docker-compose up -d
```

### 数据库初始化
数据库初始化脚本位于: `litemall-db/sql/`
- `litemall_schema.sql` - 数据库结构
- `litemall_table.sql` - 数据表
- `litemall_data.sql` - 初始数据

## 常见问题

### 1. 端口冲突
如果端口被占用，可以使用自定义端口：
```powershell
.\start-web.ps1 -BackendPort 8081 -FrontendPort 3000
```

### 2. 构建失败
检查环境配置：
- Java版本: `java -version`
- Maven版本: `mvn -version`
- Node版本: `node --version`
- npm版本: `npm --version`

### 3. 微信开发者工具未找到
如果微信开发者工具未自动打开：
1. 手动打开微信开发者工具
2. 选择"导入项目"
3. 选择项目路径: `litemall-wx`
4. 点击"编译"运行

### 4. 数据库连接失败
检查数据库配置：
- 确保MySQL服务已启动
- 检查application-dev.yml中的数据库连接配置
- 确认数据库已初始化

## 项目结构

```
litemall/
├── scripts/
│   ├── start-web.ps1        # Web商城启动脚本
│   ├── start-wx.ps1         # 微信小程序启动脚本
│   └── 启动脚本使用说明.md   # 本说明文档
├── litemall-all/            # 主后端项目
├── litemall-vue/            # Vue前端项目
├── litemall-wx/             # 微信小程序项目
├── litemall-db/sql/         # 数据库脚本
└── docker/                  # Docker配置
```

## 技术支持

如果遇到问题，请检查：
1. 环境配置是否正确
2. 端口是否被占用
3. 数据库是否已启动并初始化
4. 查看控制台输出获取详细错误信息

## 快捷方式

可以在PowerShell配置文件中添加快捷方式：
```powershell
# 添加到PowerShell配置文件
Set-Alias web-start ".\scripts\start-web.ps1"
Set-Alias wx-start ".\scripts\start-wx.ps1"
```

然后可以直接使用：
```powershell
web-start
wx-start
```