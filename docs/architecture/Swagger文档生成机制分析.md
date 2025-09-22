# Swagger文档生成机制分析

## 概述

本项目使用Springfox Swagger2和Swagger UI来生成和管理API文档。当服务启动后，可以通过访问 `http://localhost:8080/swagger-ui.html` 来查看完整的API文档。

## 核心组件

### 1. Swagger依赖配置

项目通过Maven管理Swagger相关依赖，在根目录的 `pom.xml` 中定义了版本：

```xml
<!-- Swagger2 -->
<dependency>
    <groupId>io.springfox</groupId>
    <artifactId>springfox-swagger2</artifactId>
    <version>2.9.2</version>
</dependency>

<!-- Swagger UI -->
<dependency>
    <groupId>io.springfox</groupId>
    <artifactId>springfox-swagger-ui</artifactId>
    <version>2.10.0</version>
</dependency>

<!-- Swagger Bootstrap UI 增强版 -->
<dependency>
    <groupId>com.github.xiaoymin</groupId>
    <artifactId>swagger-bootstrap-ui</artifactId>
    <version>1.9.6</version>
</dependency>
```

### 2. Swagger配置类

项目为不同的API模块分别配置了Swagger分组：

#### 2.1 管理后台API配置
- **文件路径**: `litemall-admin-api/src/main/java/org/linlinjava/litemall/admin/config/AdminSwagger2Configuration.java`
- **分组名称**: admin
- **扫描包**: org.linlinjava.litemall.admin.web
- **文档标题**: litemall-admin API
- **文档描述**: litemall管理后台API
- **访问地址**: http://localhost:8080/swagger-ui.html#/admin

#### 2.2 微信小程序API配置
- **文件路径**: `litemall-wx-api/src/main/java/org/linlinjava/litemall/wx/config/WxSwagger2Configuration.java`
- **分组名称**: wx
- **扫描包**: org.linlinjava.litemall.wx.web
- **文档标题**: litemall-wx API
- **文档描述**: litemall小商场API
- **访问地址**: http://localhost:8080/swagger-ui.html#/wx

### 3. 配置文件

在 `application.yml` 中配置了Swagger的启用状态：

```yaml
swagger:
  production: false
```

当 `production: true` 时，可以在生产环境中禁用Swagger文档。

## API分组详情

### 管理后台API分组 (admin)

**基础信息**:
- **端口号**: 8083 (独立服务时)
- **基础路径**: /admin/
- **控制器位置**: litemall-admin-api/src/main/java/org/linlinjava/litemall/admin/web/

**主要控制器**:
- AdminGoodsController - 商品管理
- AdminOrderController - 订单管理
- AdminUserController - 用户管理
- AdminRoleController - 角色权限管理
- AdminTopicController - 专题管理
- AdminBrandController - 品牌管理
- AdminCategoryController - 分类管理
- AdminAddressController - 地址管理
- AdminAftersaleController - 售后服务
- AdminCouponController - 优惠券管理
- AdminLogController - 系统日志
- AdminNoticeController - 通知管理

### 微信小程序API分组 (wx)

**基础信息**:
- **端口号**: 8082 (独立服务时)
- **基础路径**: /wx/
- **控制器位置**: litemall-wx-api/src/main/java/org/linlinjava/litemall/wx/web/

**主要控制器**:
- WxGoodsController - 商品浏览
- WxAuthController - 认证授权
- WxCartController - 购物车
- WxOrderController - 订单操作
- WxUserController - 用户信息
- WxAddressController - 收货地址
- WxCollectController - 收藏管理
- WxFootprintController - 足迹管理
- WxFeedbackController - 反馈管理
- WxCouponController - 优惠券
- WxTopicController - 专题浏览
- WxBrandController - 品牌浏览

## 服务启动方式

### 1. 单体服务启动

通过 `litemall-all` 模块启动，集成了所有API：

```bash
cd litemall-all
java -jar target/litemall-all-0.1.0-exec.jar --server.port=8080
```

启动后访问：http://localhost:8080/swagger-ui.html

### 2. 独立服务启动

可以分别启动各个API模块：

```bash
# 启动微信小程序API
cd litemall-wx-api
java -jar target/litemall-wx-api-0.1.0-exec.jar --server.port=8082

# 启动管理后台API
cd litemall-admin-api
java -jar target/litemall-admin-api-0.1.0-exec.jar --server.port=8083
```

### 3. 通过脚本启动

项目提供了PowerShell脚本进行快速启动：

```bash
# 启动完整服务
.\scripts\start-web.ps1

# 仅启动后端服务
.\scripts\start-web.ps1 -SkipFrontend

# 指定端口启动
.\scripts\start-web.ps1 -Port 8081
```

## 文档访问地址

### 单体服务模式
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **增强版UI**: http://localhost:8080/doc.html

### 独立服务模式
- **微信小程序API**: http://localhost:8082/swagger-ui.html
- **管理后台API**: http://localhost:8083/swagger-ui.html

## 技术特点

1. **分组管理**: 将不同类型的API分为微信小程序和管理后台两组，便于管理
2. **注解驱动**: 通过Spring MVC注解自动生成API文档
3. **实时更新**: 代码修改后，文档自动更新
4. **在线测试**: 支持直接在文档页面进行API测试
5. **增强版UI**: 使用swagger-bootstrap-ui提供更友好的界面

## 注意事项

1. **生产环境**: 在生产环境中建议设置 `swagger.production=true` 来禁用文档
2. **包扫描**: 确保控制器类在配置的扫描包路径下
3. **版本兼容**: 当前使用的Springfox 2.x版本与Spring Boot 2.1.x兼容
4. **依赖冲突**: 注意与其他Swagger相关依赖的版本冲突问题

## 相关文件清单

### 配置文件
- `pom.xml` - 根项目依赖版本管理
- `litemall-all/pom.xml` - 单体服务依赖
- `litemall-admin-api/pom.xml` - 管理后台API依赖
- `litemall-wx-api/pom.xml` - 微信小程序API依赖
- `litemall-all/src/main/resources/application.yml` - 单体服务配置

### Swagger配置类
- `litemall-admin-api/src/main/java/org/linlinjava/litemall/admin/config/AdminSwagger2Configuration.java`
- `litemall-wx-api/src/main/java/org/linlinjava/litemall/wx/config/WxSwagger2Configuration.java`

### 启动脚本
- `scripts/start-web.ps1` - Web服务启动脚本
- `scripts/start-wx.ps1` - 微信小程序服务启动脚本
- `scripts/build-backend.sh` - 后端构建脚本