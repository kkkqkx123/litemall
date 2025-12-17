# Jackson LocalDate 序列化问题修复报告

## 问题概述

项目存在严重的Jackson Java 8时间类型序列化问题，导致前端页面访问直接报错，小程序端完全不可用。

## 根本原因分析

### 1. 配置分散且不一致
- **litemall-core模块**：`JacksonConfig`配置了序列化器/反序列化器，但**未注册JavaTimeModule**
- **litemall-admin-api模块**：`WebConfig`正确配置了JavaTimeModule，管理后台正常
- **litemall-wx-api模块**：**无任何Jackson配置**，完全依赖默认配置

### 2. 关键配置缺失
虽然项目包含`jackson-datatype-jsr310:2.19.2`依赖，但`litemall-core`的`JacksonConfig`缺少关键的：
```java
mapper.registerModule(new JavaTimeModule());
```

### 3. 模块间配置隔离
- `admin-api`模块配置仅影响管理后台
- `wx-api`模块使用默认Jackson配置，导致LocalDate/LocalDateTime序列化为数组格式

## 影响范围

### 严重受影响的功能
1. **用户管理页面**：用户生日(birthday)字段显示异常
2. **订单管理页面**：订单时间字段序列化错误
3. **商品管理页面**：商品添加/更新时间字段异常
4. **整个小程序端**：所有涉及时间字段的接口都返回数组格式而非字符串

### 错误表现
后端返回格式：`["1990","1","1"]` (数组格式)
前端期望格式：`"1990-01-01"` (字符串格式)

## 修复方案

### 1. 核心配置修复
修改`litemall-core/src/main/java/org/linlinjava/litemall/core/config/JacksonConfig.java`：

```java
@Bean
@Order(Ordered.HIGHEST_PRECEDENCE)
public Jackson2ObjectMapperBuilderCustomizer customJackson() {
    return new Jackson2ObjectMapperBuilderCustomizer() {
        @Override
        public void customize(Jackson2ObjectMapperBuilder builder) {
            // 关键修复：注册JavaTimeModule
            builder.modules(new JavaTimeModule());
            
            // 配置LocalDateTime序列化
            builder.serializerByType(LocalDateTime.class,
                    new LocalDateTimeSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            builder.deserializerByType(LocalDateTime.class,
                    new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
            // 配置LocalDate序列化
            builder.serializerByType(LocalDate.class,
                    new LocalDateSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            builder.deserializerByType(LocalDate.class,
                    new LocalDateDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            
            // 配置LocalTime序列化
            builder.serializerByType(LocalTime.class,
                    new LocalTimeSerializer(DateTimeFormatter.ofPattern("HH:mm:ss")));
            builder.deserializerByType(LocalTime.class,
                    new LocalTimeDeserializer(DateTimeFormatter.ofPattern("HH:mm:ss")));
            
            // 其他配置
            builder.serializationInclusion(JsonInclude.Include.NON_NULL);
            builder.failOnUnknownProperties(false);
            builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        }
    };
}

/**
 * 提供全局ObjectMapper配置，确保所有模块使用统一的Jackson配置
 */
@Bean
@Primary
public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
    return builder.build();
}
```

### 2. 配置要点
1. **注册JavaTimeModule**：`builder.modules(new JavaTimeModule())`
2. **禁用时间戳格式**：`builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)`
3. **提供全局ObjectMapper**：确保所有模块共享统一配置

## 验证步骤

### 1. 后端验证
```bash
# 获取JWT令牌
curl -X POST "http://localhost:8080/admin/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin123","password":"admin123"}'

# 测试用户列表接口
curl -X GET "http://localhost:8080/admin/user/list" \
  -H "Authorization: Bearer <JWT_TOKEN>"
```

### 2. 前端验证
1. 访问用户管理页面，检查生日字段显示
2. 访问订单管理页面，检查时间字段显示
3. 测试小程序端相关功能

### 3. 数据库验证
```sql
SELECT id, username, birthday FROM litemall_user LIMIT 3;
```

## 预防措施

### 1. 统一配置管理
- 所有Jackson配置集中在`litemall-core`模块
- 其他模块通过依赖注入共享配置

### 2. 配置规范
- 必须注册`JavaTimeModule`
- 统一时间格式配置
- 禁用`WRITE_DATES_AS_TIMESTAMPS`

### 3. 测试验证
- 单元测试覆盖时间类型序列化
- 集成测试验证前后端数据格式一致性

## 相关文件

- `litemall-core/src/main/java/org/linlinjava/litemall/core/config/JacksonConfig.java`
- `litemall-admin-api/src/main/java/org/linlinjava/litemall/admin/config/WebConfig.java`
- `litemall-wx-api/src/main/java/org/linlinjava/litemall/wx/config/WxWebMvcConfiguration.java`

## 修复状态

- [x] 分析根本原因
- [x] 修复Jackson配置
- [ ] 验证修复效果
- [ ] 更新测试用例
- [ ] 文档更新完成