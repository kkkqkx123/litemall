# Jackson Java 8 时间类型序列化配置修复文档

## 问题描述

在 litemall 项目中，发现用户列表接口返回的 JSON 数据中，Java 8 时间类型（如 `LocalDate`、`LocalDateTime`）被序列化为数组格式，而不是期望的字符串格式。

**问题示例：**
```json
{
  "birthday": [1995, 7, 8],
  "addTime": [2025, 12, 8, 16, 16, 23]
}
```

**期望格式：**
```json
{
  "birthday": "1995-07-08",
  "addTime": "2025-12-08 16:16:23"
}
```

## 问题根因分析

1. **配置冲突**：`litemall-core` 模块和 `litemall-admin-api` 模块都有各自的 Jackson 配置，但配置方式不一致
2. **优先级问题**：`admin-api` 模块的 `WebConfig.configureMessageConverters()` 方法直接替换了默认的消息转换器，绕过了 Spring Boot 的自动配置机制
3. **模块作用域**：`core` 模块的 `JacksonConfig` 使用 `Jackson2ObjectMapperBuilderCustomizer`，而 `admin-api` 使用 `configureMessageConverters()`，两者作用域和优先级不同

## 解决方案

### 第一步：统一配置方式

**修改前**（`litemall-admin-api/src/main/java/org/linlinjava/litemall/admin/config/WebConfig.java`）：
```java
@Override
public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
    // 移除默认的MappingJackson2HttpMessageConverter
    Iterator<HttpMessageConverter<?>> iterator = converters.iterator();
    while (iterator.hasNext()) {
        HttpMessageConverter<?> converter = iterator.next();
        if (converter instanceof MappingJackson2HttpMessageConverter) {
            iterator.remove();
        }
    }
    
    // 添加自定义的转换器
    MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter();
    ObjectMapper objectMapper = new ObjectMapper();
    // ... 自定义配置
    jsonConverter.setObjectMapper(objectMapper);
    converters.add(jsonConverter);
}
```

**修改后**：
```java
// 注释掉自定义消息转换器配置，让Spring Boot使用core模块的Jackson配置
/*
@Override
public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
    // 移除默认的MappingJackson2HttpMessageConverter，让Spring Boot使用core模块配置的Jackson2ObjectMapperBuilderCustomizer
    Iterator<HttpMessageConverter<?>> converterIterator = converters.iterator();
    while (converterIterator.hasNext()) {
        HttpMessageConverter<?> converter = converterIterator.next();
        if (converter instanceof MappingJackson2HttpMessageConverter) {
            converterIterator.remove();
        }
    }

    // 添加自定义的转换器
    MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter();
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    
    // 注册JavaTimeModule
    JavaTimeModule javaTimeModule = new JavaTimeModule();
    javaTimeModule.addSerializer(LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
    javaTimeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
    javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    javaTimeModule.addSerializer(LocalTime.class, new LocalTimeSerializer(DateTimeFormatter.ofPattern("HH:mm:ss")));
    javaTimeModule.addDeserializer(LocalTime.class, new LocalTimeDeserializer(DateTimeFormatter.ofPattern("HH:mm:ss")));
    objectMapper.registerModule(javaTimeModule);
    
    jsonConverter.setObjectMapper(objectMapper);
    converters.add(jsonConverter);
}
*/
```

### 第二步：验证 core 模块配置

确保 `litemall-core/src/main/java/org/linlinjava/litemall/core/config/JacksonConfig.java` 配置正确：

```java
@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        
        // 基本配置
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        
        // 注册JavaTimeModule
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addSerializer(LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        javaTimeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        javaTimeModule.addSerializer(LocalTime.class, new LocalTimeSerializer(DateTimeFormatter.ofPattern("HH:mm:ss")));
        javaTimeModule.addDeserializer(LocalTime.class, new LocalTimeDeserializer(DateTimeFormatter.ofPattern("HH:mm:ss")));
        objectMapper.registerModule(javaTimeModule);
        
        return objectMapper;
    }
}
```

### 第三步：编译和重启

```bash
# 重新编译 admin-api 模块
cd d:\项目\Spring\litemall
mvn clean compile -pl litemall-admin-api -am

# 重启后端服务
cd litemall-all
java -jar target\litemall-all-0.1.0-exec.jar
```

## 验证结果

使用 curl 测试用户列表接口：

```bash
# 获取JWT token
curl -X POST "http://localhost:8080/admin/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin123","password":"admin123"}'

# 测试用户列表接口
curl -X GET "http://localhost:8080/admin/user/list?page=1&limit=10" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-jwt-token>"
```

**修复成功后**，响应数据中的时间字段应该显示为：
```json
{
  "birthday": "1995-07-08",
  "addTime": "2025-12-08 16:16:23",
  "updateTime": "2025-12-08 16:16:23"
}
```

## 最佳实践建议

1. **统一配置**：所有 Jackson 相关配置应该集中在 `core` 模块，其他模块通过 Spring Boot 的自动配置机制继承
2. **避免硬编码**：不要在各个模块中重复配置 ObjectMapper，应该使用 `Jackson2ObjectMapperBuilderCustomizer`
3. **模块依赖**：确保 `admin-api` 模块正确依赖 `core` 模块，能够继承其配置
4. **测试覆盖**：为时间类型序列化编写专门的单元测试，确保格式符合预期

## 相关依赖

确保项目中包含以下依赖：

```xml
<!-- Jackson Java 8 时间模块 -->
<dependency>
    <groupId>com.fasterxml.jackson.datatype</groupId>
    <artifactId>jackson-datatype-jsr310</artifactId>
</dependency>
```

## 参考资料

- [Jackson Java 8 时间模块官方文档](https://github.com/FasterXML/jackson-modules-java8)
- [Spring Boot Jackson 配置指南](https://docs.spring.io/spring-boot/docs/current/reference/html/howto.html#howto.spring-mvc.customize-jackson-objectmapper)
- [Jackson ObjectMapper 配置最佳实践](https://github.com/fasterxml/jackson-docs)