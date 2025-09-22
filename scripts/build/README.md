# Litemall 构建脚本使用指南

## 📁 脚本目录结构

```
scripts/build
├── README.md              # 本使用指南
├── build-backend.ps1      # 后端构建脚本 (PowerShell)
├── build-backend.sh       # 后端构建脚本 (Bash)
├── build-vue.ps1          # Vue前端构建脚本 (PowerShell)
├── build-vue.sh           # Vue前端构建脚本 (Bash)
├── build-wx.ps1           # 微信小程序构建脚本 (PowerShell)
├── build-wx.sh            # 微信小程序构建脚本 (Bash)
├── build-all.ps1          # 全量构建脚本 (PowerShell)
└── build-all.sh           # 全量构建脚本 (Bash)
```

## 🚀 快速开始

### 一键构建所有模块

**Windows (PowerShell):**
```powershell
# 基础构建
.\scripts\build-all.ps1

# 带参数构建
.\scripts\build-all.ps1 -Profile prod -Clean -Parallel
```

**Linux/macOS (Bash):**
```bash
# 基础构建
./scripts/build-all.sh

# 带参数构建
./scripts/build-all.sh -p prod --clean --parallel
```

### 单独构建模块

#### 后端构建
**Windows:**
```powershell
.\scripts\build-backend.ps1
.\scripts\build-backend.ps1 -Profile prod -Clean -SkipTests:$false
```

**Linux/macOS:**
```bash
./scripts/build-backend.sh
./scripts/build-backend.sh -p prod --clean --run-tests
```

#### Vue前端构建
**Windows:**
```powershell
.\scripts\build-vue.ps1
.\scripts\build-vue.ps1 -Environment production -Analyze
```

**Linux/macOS:**
```bash
./scripts/build-vue.sh
./scripts/build-vue.sh --env production --analyze
```

#### 微信小程序构建
**Windows:**
```powershell
.\scripts\build-wx.ps1 -Mode check
.\scripts\build-wx.ps1 -Mode build -Upload -Version "1.0.0" -Desc "更新内容"
```

**Linux/macOS:**
```bash
./scripts/build-wx.sh -m check
./scripts/build-wx.sh -m build --upload --version "1.0.0" --desc "更新内容"
```

## 📋 参数说明

### build-all 脚本参数

| 参数 | 说明 | 示例 |
|------|------|------|
| `-Profile/-p` | 构建环境 | `dev`, `test`, `prod` |
| `--skip-backend` | 跳过后端构建 | - |
| `--skip-vue` | 跳过Vue前端构建 | - |
| `--skip-wx` | 跳过微信小程序检查 | - |
| `--skip-tests` | 跳过测试 | - |
| `--run-tests` | 运行测试 | - |
| `--clean` | 执行clean操作 | - |
| `--parallel` | 启用并行构建 | - |

### build-backend 脚本参数

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `-Profile/-p` | 构建环境 | `dev` |
| `--skip-tests` | 跳过测试 | `true` |
| `--run-tests` | 运行测试 | - |
| `--clean` | 执行clean操作 | `false` |

### build-vue 脚本参数

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `-Environment/-e` | 构建环境 | `production` |
| `--dev` | 启动开发服务器 | - |
| `--skip-install` | 跳过npm install | - |
| `--analyze` | 启用构建分析 | - |

### build-wx 脚本参数

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `-Mode/-m` | 操作模式 | `build` |
| `--upload` | 执行上传操作 | - |
| `-Version/-v` | 版本号 | - |
| `-Desc/-d` | 版本描述 | - |
| `--watch` | 启动自动监听 | - |

## 📁 构建输出

### 后端构建输出
- **JAR文件**: `litemall-all/target/litemall-all-0.1.0.jar`
- **构建日志**: 控制台输出
- **测试报告**: `target/surefire-reports/` (如果运行测试)

### Vue前端构建输出
- **构建目录**: `litemall-vue/dist/`
- **静态资源**: HTML, CSS, JS文件
- **构建分析**: 如果使用 `--analyze` 参数

### 微信小程序构建输出
- **检查结果**: 控制台输出
- **上传结果**: 微信开发者工具反馈
- **预览二维码**: 如果使用 `preview` 模式

## 🔧 环境要求

### 后端构建
- **Java**: JDK 8 或更高版本
- **Maven**: 3.6+ (已配置环境变量)
- **MySQL**: 5.7+ (用于测试)

### Vue前端构建
- **Node.js**: 14.x 或更高版本
- **npm**: 6.x 或更高版本
- **Vue CLI**: 已安装项目依赖

### 微信小程序构建
- **微信开发者工具**: 已安装最新版本
- **CLI工具**: 已配置到系统PATH
- **项目配置**: 已配置 `project.config.json`

## 🐛 常见问题

### 后端构建失败
1. **检查Java版本**: `java -version`
2. **检查Maven版本**: `mvn -version`
3. **检查数据库连接**: 确保MySQL已启动
4. **清理构建缓存**: 使用 `--clean` 参数

### Vue构建失败
1. **检查Node版本**: `node --version`
2. **检查npm版本**: `npm --version`
3. **删除node_modules**: 手动删除后重新安装
4. **清除npm缓存**: `npm cache clean --force`

### 微信小程序构建失败
1. **检查微信开发者工具**: 确保已安装
2. **检查CLI路径**: 确保CLI在系统PATH中
3. **检查项目配置**: 确保 `project.config.json` 正确
4. **检查AppID**: 确保已配置正确的AppID

## 📊 性能优化

### 并行构建
- 使用 `--parallel` 参数可以同时构建后端和Vue前端
- 可以显著减少总构建时间
- 需要足够的系统资源

### 跳过测试
- 开发阶段可以使用 `--skip-tests` 加快构建速度
- 生产环境建议使用 `--run-tests` 确保质量

### 缓存优化
- Vue项目使用 `npm ci` 替代 `npm install` 加速依赖安装
- Maven使用本地缓存加速依赖下载

## 🎯 开发工作流

### 日常开发
```bash
# 快速构建，跳过测试
./scripts/build-all.sh --skip-tests

# 仅构建后端
./scripts/build-backend.sh --skip-tests

# Vue开发模式
./scripts/build-vue.sh --dev
```

### 生产部署
```bash
# 完整构建，运行测试
./scripts/build-all.sh -p prod --run-tests --clean

# 单独构建生产环境
./scripts/build-backend.sh -p prod --run-tests --clean
./scripts/build-vue.sh --env production
```

## 📞 技术支持

如有问题，请检查:
1. 环境变量配置
2. 依赖版本兼容性
3. 网络连接状态
4. 项目配置文件完整性