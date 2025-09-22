# Litemall Build Scripts 使用说明

## 📋 目录结构

```
scripts/
├── README.md                    # 英文使用指南
├── build-all.ps1               # 全量构建脚本 (PowerShell)
├── build-all.sh                # 全量构建脚本 (Bash)
├── build-backend.ps1           # 后端构建脚本 (PowerShell)
├── build-backend.sh            # 后端构建脚本 (Bash)
├── build-vue.ps1               # Vue前端构建脚本 (PowerShell)
├── build-vue.sh                # Vue前端构建脚本 (Bash)
├── build-wx.ps1                # 微信小程序构建脚本 (PowerShell)
├── build-wx.sh                 # 微信小程序构建脚本 (Bash)
├── start-web.ps1               # Web商城启动脚本 (PowerShell)
├── start-wx.ps1                # 微信小程序启动脚本 (PowerShell)
└── 启动脚本使用说明.md          # 启动脚本中文说明
```

## 🚀 脚本功能概述

### 1. build-all 系列脚本
**用途**: 一键构建整个litemall项目的所有模块
- `build-all.ps1` - Windows PowerShell版本
- `build-all.sh` - Linux/macOS Bash版本

**主要功能**:
- 构建后端服务 (litemall-all)
- 构建Vue前端 (litemall-vue)
- 检查微信小程序项目 (litemall-wx)
- 支持并行构建模式
- 支持选择性跳过某些模块

**使用示例**:
```powershell
# Windows - 基础构建
.\scripts\build-all.ps1

# Windows - 生产环境并行构建
.\scripts\build-all.ps1 -Profile prod -Clean -Parallel

# Linux/macOS - 跳过测试的生产环境构建
./scripts/build-all.sh -p prod --clean --skip-tests
```

### 2. build-backend 系列脚本
**用途**: 单独构建后端服务
- `build-backend.ps1` - Windows PowerShell版本
- `build-backend.sh` - Linux/macOS Bash版本

**主要功能**:
- 使用Maven构建所有后端模块
- 支持多种构建环境 (dev/test/prod)
- 可选运行单元测试
- 支持clean操作
- 生成可执行JAR文件

**使用示例**:
```powershell
# Windows - 基础构建
.\scripts\build-backend.ps1

# Windows - 生产环境完整构建
.\scripts\build-backend.ps1 -Profile prod -Clean -SkipTests:$false

# Linux/macOS - 开发环境构建并运行测试
./scripts/build-backend.sh -p dev --run-tests
```

### 3. build-vue 系列脚本
**用途**: 构建Vue移动端商城前端
- `build-vue.ps1` - Windows PowerShell版本
- `build-vue.sh` - Linux/macOS Bash版本

**主要功能**:
- 安装npm依赖 (支持npm ci优化)
- 构建生产环境版本
- 启动开发服务器
- 支持构建分析
- 生成静态资源文件

**使用示例**:
```powershell
# Windows - 生产环境构建
.\scripts\build-vue.ps1

# Windows - 启动开发服务器
.\scripts\build-vue.ps1 -Dev

# Windows - 带分析的构建
.\scripts\build-vue.ps1 -Analyze

# Linux/macOS - 跳过依赖安装
./scripts/build-vue.sh --skip-install
```

### 4. build-wx 系列脚本
**用途**: 微信小程序项目检查和构建
- `build-wx.ps1` - Windows PowerShell版本
- `build-wx.sh` - Linux/macOS Bash版本

**主要功能**:
- 检查小程序项目结构完整性
- 验证必要的配置文件
- 支持通过微信开发者工具CLI上传
- 生成预览二维码
- 文件变更自动监听

**使用示例**:
```powershell
# Windows - 基础检查
.\scripts\build-wx.ps1

# Windows - 检查模式
.\scripts\build-wx.ps1 -Mode check

# Windows - 构建并上传
.\scripts\build-wx.ps1 -Mode build -Upload -Version "1.0.0" -Desc "功能更新"

# Windows - 生成预览二维码
.\scripts\build-wx.ps1 -Mode preview
```

### 5. start-web 脚本
**用途**: 一键启动Web商城前后端服务
- `start-web.ps1` - Windows PowerShell版本

**主要功能**:
- 自动构建缺失的后端服务
- 启动Spring Boot后端 (端口8080)
- 启动Vue开发服务器 (端口9527)
- 支持自定义端口配置
- 数据库连接检查

**使用示例**:
```powershell
# 基础启动
.\scripts\start-web.ps1

# 自定义端口
.\scripts\start-web.ps1 -BackendPort 8081 -FrontendPort 3000

# 只启动后端
.\scripts\start-web.ps1 -SkipFrontend

# 生产环境启动
.\scripts\start-web.ps1 -Profile prod
```

### 6. start-wx 脚本
**用途**: 一键启动微信小程序相关服务
- `start-wx.ps1` - Windows PowerShell版本

**主要功能**:
- 启动微信小程序后端API服务 (端口8082)
- 自动打开微信开发者工具
- 数据库连接状态检查
- 支持自定义端口配置
- 项目结构验证

**使用示例**:
```powershell
# 基础启动
.\scripts\start-wx.ps1

# 自定义API端口
.\scripts\start-wx.ps1 -WxApiPort 8083

# 只启动后端服务
.\scripts\start-wx.ps1 -SkipWxFrontend

# 生产环境启动
.\scripts\start-wx.ps1 -Profile prod
```

## 📊 构建输出说明

### 后端构建输出
- **主要输出**: `litemall-all/target/litemall-all-0.1.0.jar`
- **构建日志**: 控制台实时输出
- **测试报告**: `target/surefire-reports/` (如果运行测试)
- **依赖缓存**: Maven本地仓库

### Vue前端构建输出
- **构建目录**: `litemall-vue/dist/`
- **主要文件**: 
  - `index.html` - 入口页面
  - `static/` - 静态资源目录
  - `*.js` - JavaScript文件
  - `*.css` - 样式文件
- **构建分析**: 如果使用 `--analyze` 参数会生成分析报告

### 微信小程序构建输出
- **检查结果**: 控制台输出项目状态
- **上传结果**: 微信开发者工具反馈信息
- **预览二维码**: 如果使用预览模式会生成二维码

## 🔧 环境要求

### 基础环境
- **操作系统**: Windows 10+ / Linux / macOS
- **PowerShell**: 5.1+ (Windows) 或 PowerShell Core
- **Bash**: 4.0+ (Linux/macOS)

### 后端构建环境
- **Java**: JDK 8 或更高版本
- **Maven**: 3.6+ (已配置环境变量)
- **MySQL**: 5.7+ (用于测试和运行)

### 前端构建环境
- **Node.js**: 14.x 或更高版本
- **npm**: 6.x 或更高版本
- **Vue CLI**: 项目依赖中已包含

### 微信小程序环境
- **微信开发者工具**: 最新稳定版本
- **CLI工具**: 需要配置到系统PATH
- **小程序AppID**: 已配置的项目AppID

## ⚡ 性能优化建议

### 并行构建
- 使用 `--parallel` 参数可以同时构建后端和Vue前端
- 显著减少总构建时间，但需要更多系统资源
- 推荐在CI/CD环境中使用

### 缓存优化
- Vue项目使用 `npm ci` 替代 `npm install` 加速依赖安装
- Maven使用本地缓存，首次构建后速度会提升
- 合理使用 `--skip-install` 参数避免重复安装

### 选择性构建
- 开发阶段使用 `--skip-tests` 加快构建速度
- 使用模块跳过参数只构建需要的部分
- 生产环境建议完整构建并运行测试

## 🐛 常见问题排查

### 构建失败通用检查
1. **环境检查**: 确认所有必需软件已安装
2. **路径检查**: 确认项目路径和脚本路径正确
3. **权限检查**: 确认有足够的文件系统权限
4. **网络检查**: 确认可以访问外部依赖仓库

### 后端构建失败
1. **Java版本**: `java -version` 检查版本
2. **Maven版本**: `mvn -version` 检查配置
3. **数据库连接**: 确保MySQL服务正常运行
4. **端口占用**: 检查是否有其他服务占用端口

### 前端构建失败
1. **Node版本**: `node --version` 检查版本
2. **npm版本**: `npm --version` 检查配置
3. **依赖冲突**: 删除 `node_modules` 重新安装
4. **内存不足**: 增加Node.js内存限制

### 微信小程序构建失败
1. **开发者工具**: 确认已安装最新版本
2. **CLI路径**: 检查CLI是否在系统PATH中
3. **项目配置**: 验证 `project.config.json` 文件
4. **AppID配置**: 确认已配置正确的小程序AppID

## 🎯 开发工作流建议

### 日常开发流程
```bash
# 1. 快速构建（跳过测试）
./scripts/build-all.sh --skip-tests

# 2. 启动Web服务进行开发
./scripts/start-web.ps1

# 3. 修改代码后重新构建特定模块
./scripts/build-backend.sh --skip-tests
```

### 发布前流程
```bash
# 1. 完整构建（包含测试）
./scripts/build-all.sh -p prod --clean --run-tests

# 2. 验证微信小程序
./scripts/build-wx.ps1 -Mode check

# 3. 构建生产环境前端
./scripts/build-vue.sh --env production
```

### 持续集成建议
- 使用并行构建减少CI时间
- 缓存依赖文件加速后续构建
- 分离前后端构建任务便于并行处理
- 设置构建失败通知机制

## 📚 相关文档

- [启动脚本使用说明](启动脚本使用说明.md) - 启动脚本详细说明
- [README.md](README.md) - 英文版使用指南
- [项目文档](../docs/) - 项目架构和配置文档
- [部署文档](../deploy/) - 生产环境部署指南