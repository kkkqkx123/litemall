# LLM模块架构分析

## 概述

本文档分析了`litemall-core/src/main/java/org/linlinjava/litemall/core/llm`目录下的LLM（大语言模型）模块实现，重点阐述多轮对话、LLM响应解析和数据库操作执行三个核心功能的实现机制。

## 模块结构

LLM模块采用分层架构设计，包含以下核心组件：

```
llm/
├── exception/          # 异常定义
│   ├── LLMServiceException.java
│   ├── LLMOutputParseException.java
│   └── SessionException.java
├── model/              # 数据模型
│   ├── QueryIntent.java
│   ├── GoodsQARequest.java
│   └── GoodsQAResponse.java
├── parser/             # 解析器组件
│   ├── LLMOutputParser.java
│   ├── QueryIntentBuilder.java
│   ├── SQLBuilder.java
│   ├── JSONExtractor.java
│   └── ParameterBinder.java
└── service/            # 服务组件
    ├── LLMQAService.java
    ├── SessionManager.java
    ├── Qwen3Service.java
    └── GoodsQueryService.java
```

## 核心功能实现分析

### 1. 多轮对话实现

#### 1.1 会话管理机制

多轮对话通过`SessionManager`类实现，核心特性包括：

- **会话存储**：使用`ConcurrentHashMap<String, Session>`存储会话，支持线程安全访问
- **会话超时**：配置30分钟会话超时时间，自动清理过期会话
- **上下文管理**：每个会话维护历史对话记录，支持上下文理解

```java
// 会话数据结构
public class Session {
    private String sessionId;
    private List<ConversationTurn> conversationHistory;
    private LocalDateTime lastAccessTime;
    private Map<String, Object> attributes;
}
```

#### 1.2 对话流程

多轮对话处理流程：

1. **请求接收**：`LLMQAService.handleGoodsQA()`接收用户问题
2. **会话获取**：根据`sessionId`获取或创建会话
3. **上下文构建**：构建包含历史对话的提示词
4. **LLM调用**：调用`Qwen3Service`获取响应
5. **会话更新**：将当前对话添加到会话历史
6. **响应返回**：返回包含新`sessionId`的响应

#### 1.3 提示词工程

系统提示词设计考虑了商品问答场景：

```java
private static final String SYSTEM_PROMPT = "你是一个智能商品问答助手。\n" +
    "你的任务是根据用户的自然语言问题，生成相应的数据库查询。\n" +
    "数据库表结构如下：...\n" +
    "请严格按照以下JSON格式返回查询条件：...";
```

### 2. LLM响应解析机制

#### 2.1 解析流程

LLM响应解析采用多阶段处理：

1. **JSON提取**：`JSONExtractor`从LLM输出中提取JSON部分
2. **JSON解析**：`LLMOutputParser`将JSON转换为`QueryIntent`对象
3. **意图验证**：`QueryIntentBuilder`验证查询意图的有效性
4. **错误处理**：完善的异常处理机制

#### 2.2 JSON提取策略

`JSONExtractor`采用以下策略：

- **模式匹配**：使用正则表达式`\\{[\\s\\S]*\\}`提取JSON
- **Markdown清理**：移除可能的Markdown代码块标记
- **格式修复**：自动修复常见的JSON格式问题（多余逗号、单引号等）
- **格式验证**：使用`org.json.JSONObject`验证JSON有效性

#### 2.3 查询意图模型

`QueryIntent`定义了7种查询类型：

| 查询类型 | 描述 | 支持条件 |
|---------|------|----------|
| price_range | 价格范围查询 | min_price, max_price |
| stock_check | 库存查询 | min_number, max_number |
| category_filter | 分类筛选 | category_id |
| keyword_search | 关键词搜索 | keyword |
| name_pattern | 名称模式匹配 | pattern, mode, case_sensitive |
| specific_product | 特定商品查询 | id |
| statistical | 统计查询 | statistic_type |

#### 2.4 名称模式匹配

支持多种名称匹配模式：

- **exact**：精确匹配
- **contains**：包含匹配（默认）
- **starts_with**：前缀匹配
- **ends_with**：后缀匹配
- **regex**：正则表达式匹配

支持大小写敏感/不敏感配置。

### 3. 数据库操作执行

#### 3.1 SQL构建机制

`SQLBuilder`根据`QueryIntent`动态生成SQL：

```java
public String buildQuerySQL(QueryIntent queryIntent) {
    StringBuilder sql = new StringBuilder("SELECT * FROM litemall_goods WHERE deleted = 0");
    
    // 添加查询条件
    addConditions(sql, queryIntent.getConditions());
    
    // 添加排序
    if (queryIntent.getSort() != null) {
        sql.append(" ORDER BY ").append(queryIntent.getSort());
    }
    
    // 添加限制
    if (queryIntent.getLimit() != null) {
        sql.append(" LIMIT ").append(queryIntent.getLimit());
    }
    
    return sql.toString();
}
```

#### 3.2 参数绑定

`ParameterBinder`处理参数绑定：

- **类型支持**：String、Integer、Long、Double、Float、Boolean
- **模式处理**：根据名称匹配模式生成相应的LIKE语句
- **安全防护**：防止SQL注入攻击
- **空值处理**：正确处理NULL值

#### 3.3 统计查询

支持多种统计查询类型：

- **总数统计**：商品总数
- **价格统计**：价格范围、平均值
- **库存统计**：总库存、平均库存
- **分类统计**：各分类商品数量

#### 3.4 结果转换

`GoodsQueryService`将数据库结果转换为业务对象：

```java
private List<Map<String, Object>> convertResultSetToList(ResultSet rs) {
    List<Map<String, Object>> resultList = new ArrayList<>();
    ResultSetMetaData metaData = rs.getMetaData();
    int columnCount = metaData.getColumnCount();
    
    while (rs.next()) {
        Map<String, Object> row = new LinkedHashMap<>();
        for (int i = 1; i <= columnCount; i++) {
            String columnName = metaData.getColumnLabel(i);
            Object value = rs.getObject(i);
            row.put(columnName, value);
        }
        resultList.add(row);
    }
    
    return resultList;
}
```

## 技术特点

### 1. 异常处理

完善的异常处理体系：

- **LLMServiceException**：LLM服务相关异常
- **LLMOutputParseException**：输出解析异常
- **SessionException**：会话管理异常

### 2. 性能优化

- **连接池**：使用Spring的`DataSource`连接池
- **资源管理**：完善的资源关闭机制
- **日志记录**：详细的调试日志支持

### 3. 安全性

- **SQL注入防护**：使用PreparedStatement
- **输入验证**：参数类型检查和验证
- **会话安全**：会话ID生成和验证

### 4. 可扩展性

- **模块化设计**：各组件职责清晰
- **配置化**：支持多种配置参数
- **插件化**：易于添加新的查询类型

## 总结

LLM模块通过精心设计的架构实现了：

1. **多轮对话**：基于会话管理的上下文保持机制
2. **智能解析**：多阶段的LLM响应解析流程
3. **安全查询**：动态SQL构建和参数绑定
4. **丰富查询**：支持7种查询类型和多种统计功能

该模块为商品问答场景提供了完整的LLM集成解决方案，具有良好的可维护性和扩展性。