# 大模型商品问答功能 - 配置文档

## 1. ModelScope配置

### 1.1 ModelScope配置
```yaml
# application.yml
litemall:
  modelscope:
    api-key: demo-key  # 当前为演示模式，无需真实API密钥
    api-url: https://api.modelscope.cn/v1  # ModelScope API地址
    enabled: true  # 启用问答功能
    max-context-length: 5000  # 最大上下文长度
    session-timeout: 600  # 会话超时时间（秒）
```

### 1.2 环境变量配置
```bash
# ModelScope API配置
export MODELSCOPE_API_KEY="your-actual-api-key"
export MODELSCOPE_API_URL="https://api.modelscope.cn/v1"
export MODELSCOPE_MODEL_NAME="Qwen/Qwen3-32B"

# LLM参数配置
export LLM_MAX_TOKENS="2000"
export LLM_TEMPERATURE="0.7"
export LLM_TOP_P="0.9"

# 功能开关
export QA_ENABLED="true"
export QA_TIMEOUT="30"
export QA_MAX_RETRIES="3"
```

## 2. 数据库配置

### 2.1 连接配置
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/litemall?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC
    username: kkkqkx
    password: 1234567kk
    driver-class-name: com.mysql.cj.jdbc.Driver
    
    # 连接池配置
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      idle-timeout: 300000
      connection-timeout: 20000
      validation-timeout: 5000
```

### 2.2 Redis配置（可选）
```yaml
spring:
  redis:
    host: localhost
    port: 6379
    password: 
    database: 0
    timeout: 2000ms
    
    # 连接池配置
    lettuce:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0
        max-wait: -1ms
```

## 3. 日志配置

### 3.1 日志级别
```yaml
logging:
  level:
    org.lin.litemall.qa: DEBUG    # 问答模块日志
    org.springframework.web: INFO
    com.zaxxer.hikari: WARN
    
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
  
  file:
    name: logs/litemall-qa.log
    max-size: 10MB
    max-history: 30
```

### 3.2 问答模块专用日志
```xml
<!-- logback-spring.xml -->
<configuration>
    <appender name="QA_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/litemall-qa.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/litemall-qa.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <logger name="org.lin.litemall.qa" level="DEBUG" additivity="false">
        <appender-ref ref="QA_FILE"/>
    </logger>
</configuration>
```

## 4. 监控配置

### 4.1 Actuator配置
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus,info
      base-path: /actuator
  
  endpoint:
    health:
      show-details: always
      show-components: always
    
    metrics:
      enabled: true
  
  metrics:
    export:
      prometheus:
        enabled: true
    
    tags:
      application: ${spring.application.name}
      environment: ${spring.profiles.active}
```

### 4.2 自定义指标
```yaml
litemall:
  metrics:
    qa:
      enabled: true
      
      # 业务指标
      questions:
        counter: true        # 问题计数器
        timer: true          # 响应时间计时器
      
      # 技术指标
      llm:
        counter: true        # LLM调用计数器
        timer: true          # LLM响应时间
      
      database:
        counter: true        # 数据库查询计数器
        timer: true          # 数据库查询时间
```

## 5. 安全配置

### 5.1 认证配置
```yaml
litemall:
  auth:
    jwt:
      secret: ${JWT_SECRET:your-jwt-secret-key}
      expiration: 86400     # JWT过期时间（秒）
      
    # 问答功能权限
    qa:
      roles:
        - ADMIN            # 管理员角色
        - USER             # 用户角色
      permissions:
        - goods:qa:ask     # 提问权限
        - goods:qa:history # 查看历史权限
```

### 5.2 接口安全
```yaml
litemall:
  security:
    qa:
      # 限流配置
      rate-limiting:
        enabled: true
        
        # 基于IP的限流
        ip-based:
          requests-per-minute: 60
          burst-size: 10
        
        # 基于用户的限流
        user-based:
          requests-per-minute: 100
          burst-size: 20
      
      # 输入验证
      validation:
        max-question-length: 500    # 最大问题长度
        max-context-length: 5000    # 最大上下文长度
        allowed-characters: "[\\u4e00-\\u9fa5\\w\\s\\d.,!?;:'\"()\\-]+"  # 允许的字符
      
      # SQL注入防护
      sql-injection:
        enabled: true
        patterns:
          - "(\\b(union|select|insert|update|delete|drop|create|alter|exec|execute)\\b)"
          - "(--|#|/\\*|\\*/|xp_)"
```

## 6. 前端配置

### 6.1 API配置
```javascript
// config/qa.js
export const QA_CONFIG = {
  // API基础路径
  baseURL: process.env.VUE_APP_BASE_API || 'http://localhost:8080/admin',
  
  // 超时配置
  timeout: 30000, // 30秒
  
  // 重试配置
  retry: {
    count: 3,     // 重试次数
    delay: 1000   // 重试延迟（毫秒）
  },
  
  // 限流配置
  rateLimit: {
    requestsPerMinute: 100,
    burstSize: 20
  },
  
  // UI配置
  ui: {
    maxMessageLength: 500,      // 最大消息长度
    autoScroll: true,           // 自动滚动
    showTimestamp: true,        // 显示时间戳
    enableQuickQuestions: true, // 启用快速提问
    maxContextHistory: 5       // 最大上下文历史
  }
}
```

### 6.2 环境变量
```bash
# 开发环境
VUE_APP_BASE_API=http://localhost:8080/admin
VUE_APP_QA_ENABLED=true
VUE_APP_QA_TIMEOUT=30000

# 生产环境
VUE_APP_BASE_API=https://your-domain.com/admin
VUE_APP_QA_ENABLED=true
VUE_APP_QA_TIMEOUT=30000
```

## 7. 部署配置

### 7.1 Docker配置
```dockerfile
# Dockerfile
FROM openjdk:8-jre-slim

# 安装必要的工具
RUN apt-get update && apt-get install -y \
    curl \
    && rm -rf /var/lib/apt/lists/*

# 设置工作目录
WORKDIR /app

# 复制应用文件
COPY target/litemall-all.jar app.jar

# 设置环境变量
ENV JAVA_OPTS="-Xms512m -Xmx1024m"
ENV SPRING_PROFILES_ACTIVE=prod

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# 运行应用
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

### 7.2 Docker Compose配置
```yaml
# docker-compose.yml
version: '3.8'

services:
  litemall:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - MODELSCOPE_API_KEY=${MODELSCOPE_API_KEY}
      - DB_HOST=mysql
      - DB_PORT=3306
      - DB_NAME=litemall
      - DB_USERNAME=kkkqkx
      - DB_PASSWORD=1234567kk
    depends_on:
      - mysql
      - redis
    networks:
      - litemall-network
    
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: rootpassword
      MYSQL_DATABASE: litemall
      MYSQL_USER: kkkqkx
      MYSQL_PASSWORD: 1234567kk
    ports:
      - "3306:3306"
    volumes:
      - mysql-data:/var/lib/mysql
    networks:
      - litemall-network
  
  redis:
    image: redis:6-alpine
    ports:
      - "6379:6379"
    volumes:
      - redis-data:/data
    networks:
      - litemall-network

volumes:
  mysql-data:
  redis-data:

networks:
  litemall-network:
    driver: bridge
```

## 8. 测试配置

### 8.1 单元测试配置
```yaml
# application-test.yml
spring:
  profiles:
    active: test
  
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
    username: sa
    password: 
  
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true

litemall:
  modelscope:
    api:
      key: test-key
      url: https://api.modelscope.cn/v1
    qa:
      enabled: true
      timeout: 10
      max-retries: 1
```

### 8.2 集成测试配置
```javascript
// test/config/qa.config.js
export const TEST_CONFIG = {
  baseURL: 'http://localhost:8080/admin',
  timeout: 10000,
  
  testQuestions: [
    '价格在100-200元的商品有哪些？',
    '库存充足的商品有哪些？',
    '电子产品分类下的商品有哪些？',
    '统计在售商品总数'
  ],
  
  expectedPatterns: [
    /价格.*元/,
    /库存.*充足/,
    /电子.*产品/,
    /统计.*总数/
  ]
}
```