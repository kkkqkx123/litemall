# LLM响应解析机制

## 概述

LLM响应解析是将大语言模型的自然语言输出转换为结构化查询意图的关键过程。该模块采用多阶段解析策略，确保从非结构化的文本中提取准确的查询参数。

## 解析架构

### 核心组件关系

```
LLM输出文本
    ↓
JSONExtractor（提取JSON）
    ↓
LLMOutputParser（解析JSON）
    ↓
QueryIntentBuilder（构建意图）
    ↓
QueryIntent（结构化对象）
```

## JSON提取阶段

### JSONExtractor

`JSONExtractor`负责从LLM输出中提取JSON格式的内容：

```java
@Component
public class JSONExtractor {
    private static final Pattern JSON_PATTERN = Pattern.compile(
        "\\{[\\s\\S]*\\}", Pattern.MULTILINE
    );
    
    public String extractJSON(String text) throws LLMOutputParseException {
        // 清理Markdown标记
        String cleanedText = removeMarkdownCodeBlocks(text);
        
        // 提取JSON
        Matcher matcher = JSON_PATTERN.matcher(cleanedText);
        if (matcher.find()) {
            String jsonStr = matcher.group();
            
            // 验证JSON格式
            try {
                new JSONObject(jsonStr);
                return jsonStr;
            } catch (Exception e) {
                // 尝试修复常见JSON问题
                return fixCommonJSONIssues(jsonStr);
            }
        }
        
        throw new LLMOutputParseException("未找到有效的JSON输出");
    }
}
```

### 清理策略

1. **Markdown代码块清理**：
   ```java
   private String removeMarkdownCodeBlocks(String text) {
       return text.replaceAll("```json\\n?", "")
                 .replaceAll("```\\n?", "")
                 .trim();
   }
   ```

2. **常见JSON格式修复**：
   ```java
   private String fixCommonJSONIssues(String jsonStr) {
       // 修复末尾多余的逗号
       jsonStr = jsonStr.replaceAll(",\\s*}", "}");
       jsonStr = jsonStr.replaceAll(",\\s*]", "]");
       
       // 修复单引号
       jsonStr = jsonStr.replaceAll("'", "\"");
       
       return jsonStr;
   }
   ```

## JSON解析阶段

### LLMOutputParser

`LLMOutputParser`将提取的JSON转换为`QueryIntent`对象：

```java
public QueryIntent parse(String llmOutput) throws LLMOutputParseException {
    try {
        // 1. 提取JSON
        String jsonStr = jsonExtractor.extractJSON(llmOutput);
        
        // 2. 解析JSON
        JSONObject json = new JSONObject(jsonStr);
        
        // 3. 构建QueryIntent
        QueryIntent intent = new QueryIntent();
        intent.setQueryType(json.getString("query_type"));
        
        // 4. 解析条件
        JSONObject conditions = json.getJSONObject("conditions");
        Map<String, Object> conditionsMap = parseConditions(conditions);
        intent.setConditions(conditionsMap);
        
        // 5. 解析其他字段
        if (json.has("sort")) {
            intent.setSort(json.getString("sort"));
        }
        if (json.has("limit")) {
            intent.setLimit(json.getInt("limit"));
        }
        
        // 6. 验证
        if (!intent.isValid()) {
            throw new LLMOutputParseException("无效的查询意图");
        }
        
        return intent;
        
    } catch (Exception e) {
        throw new LLMOutputParseException("解析LLM输出失败: " + e.getMessage(), e);
    }
}
```

### 条件解析

```java
private Map<String, Object> parseConditions(JSONObject conditions) {
    Map<String, Object> result = new HashMap<>();
    
    Iterator<String> keys = conditions.keys();
    while (keys.hasNext()) {
        String key = keys.next();
        Object value = conditions.get(key);
        
        // 处理嵌套对象
        if (value instanceof JSONObject) {
            JSONObject nestedObj = (JSONObject) value;
            Map<String, Object> nestedMap = new HashMap<>();
            
            Iterator<String> nestedKeys = nestedObj.keys();
            while (nestedKeys.hasNext()) {
                String nestedKey = nestedKeys.next();
                nestedMap.put(nestedKey, nestedObj.get(nestedKey));
            }
            
            result.put(key, nestedMap);
        } else {
            result.put(key, value);
        }
    }
    
    return result;
}
```

## 查询意图构建

### QueryIntent模型

```java
public class QueryIntent {
    private String queryType;           // 查询类型
    private Map<String, Object> conditions; // 查询条件
    private String sort;                // 排序方式
    private Integer limit;              // 限制数量
    
    public boolean isValid() {
        return queryType != null && conditions != null && !conditions.isEmpty();
    }
}
```

### 支持的查询类型

| 查询类型 | 描述 | JSON格式示例 |
|---------|------|-------------|
| price_range | 价格范围查询 | `{"query_type": "price_range", "conditions": {"min_price": 100, "max_price": 500}}` |
| stock_check | 库存查询 | `{"query_type": "stock_check", "conditions": {"min_number": 10}}` |
| category_filter | 分类筛选 | `{"query_type": "category_filter", "conditions": {"category_id": 123}}` |
| keyword_search | 关键词搜索 | `{"query_type": "keyword_search", "conditions": {"keyword": "手机"}}` |
| name_pattern | 名称模式匹配 | `{"query_type": "name_pattern", "conditions": {"name": {"pattern": "苹果", "mode": "contains"}}}` |
| specific_product | 特定商品查询 | `{"query_type": "specific_product", "conditions": {"id": 1001}}` |
| statistical | 统计查询 | `{"query_type": "statistical", "conditions": {"statistic_type": "total_count"}}` |

## 查询意图构建器

### QueryIntentBuilder

`QueryIntentBuilder`提供查询意图的构建和验证功能：

```java
@Component
public class QueryIntentBuilder {
    
    // 构建价格范围查询
    public QueryIntent buildPriceRangeQuery(Double minPrice, Double maxPrice) {
        QueryIntent intent = new QueryIntent();
        intent.setQueryType(QueryType.PRICE_RANGE.getValue());
        
        Map<String, Object> conditions = new HashMap<>();
        if (minPrice != null) conditions.put("min_price", minPrice);
        if (maxPrice != null) conditions.put("max_price", maxPrice);
        conditions.put("is_on_sale", 1); // 只查询在售商品
        
        intent.setConditions(conditions);
        intent.setSort("price ASC");
        return intent;
    }
    
    // 构建名称模式查询
    public QueryIntent buildNamePatternQuery(String pattern, String mode, boolean caseSensitive) {
        QueryIntent intent = new QueryIntent();
        intent.setQueryType(QueryType.NAME_PATTERN.getValue());
        
        Map<String, Object> nameCondition = new HashMap<>();
        nameCondition.put("pattern", pattern);
        nameCondition.put("mode", mode);
        nameCondition.put("case_sensitive", caseSensitive);
        
        Map<String, Object> conditions = new HashMap<>();
        conditions.put("name", nameCondition);
        conditions.put("is_on_sale", 1);
        
        intent.setConditions(conditions);
        return intent;
    }
}
```

### 验证机制

```java
public boolean validateQueryIntent(QueryIntent intent) {
    if (intent == null || !intent.isValid()) {
        return false;
    }
    
    // 验证查询类型
    if (!QueryType.isValid(intent.getQueryType())) {
        return false;
    }
    
    // 验证名称模式匹配的条件
    if (QueryType.NAME_PATTERN.getValue().equals(intent.getQueryType())) {
        Object nameCondition = intent.getConditions().get("name");
        if (nameCondition instanceof Map) {
            Map<String, Object> nameMap = (Map<String, Object>) nameCondition;
            String mode = (String) nameMap.get("mode");
            if (!NamePatternMode.isValid(mode)) {
                return false;
            }
        }
    }
    
    return true;
}
```

## 名称模式匹配详解

### 匹配模式

支持5种名称匹配模式：

```java
public enum NamePatternMode {
    EXACT("exact"),           // 精确匹配
    CONTAINS("contains"),     // 包含匹配（默认）
    STARTS_WITH("starts_with"), // 前缀匹配
    ENDS_WITH("ends_with"),   // 后缀匹配
    REGEX("regex");          // 正则表达式匹配
}
```

### 参数绑定

`ParameterBinder`根据模式生成相应的SQL参数：

```java
private void extractNameParameters(Map<String, Object> nameCondition, List<Object> parameters) {
    String pattern = (String) nameCondition.get("pattern");
    String mode = (String) nameCondition.get("mode");
    Boolean caseSensitive = (Boolean) nameCondition.get("case_sensitive");
    
    if (pattern == null || pattern.trim().isEmpty()) {
        return;
    }
    
    // 默认模式为contains
    if (mode == null || mode.trim().isEmpty()) {
        mode = "contains";
    }
    
    switch (mode) {
        case "exact":
            parameters.add(caseSensitive != null && caseSensitive ? pattern : pattern.toLowerCase());
            break;
        case "contains":
            parameters.add(caseSensitive != null && caseSensitive ? "%" + pattern + "%" : "%" + pattern.toLowerCase() + "%");
            break;
        case "starts_with":
            parameters.add(caseSensitive != null && caseSensitive ? pattern + "%" : pattern.toLowerCase() + "%");
            break;
        case "ends_with":
            parameters.add(caseSensitive != null && caseSensitive ? "%" + pattern : "%" + pattern.toLowerCase());
            break;
        case "regex":
            parameters.add(pattern);
            break;
    }
}
```

## 错误处理

### 解析异常

```java
public class LLMOutputParseException extends RuntimeException {
    public LLMOutputParseException(String message) {
        super(message);
    }
    
    public LLMOutputParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

### 常见错误场景

1. **JSON提取失败**：
   ```
   错误：LLM输出为空
   处理：返回友好的错误消息
   ```

2. **JSON格式错误**：
   ```
   错误：JSON格式修复失败
   处理：提示用户重新表述问题
   ```

3. **查询意图无效**：
   ```
   错误：无效的查询意图
   处理：使用默认查询策略
   ```

## 性能优化

### 缓存机制

```java
// 解析结果缓存
private final Map<String, QueryIntent> parseCache = new ConcurrentHashMap<>();

public QueryIntent parseWithCache(String llmOutput) {
    String cacheKey = generateCacheKey(llmOutput);
    
    QueryIntent cached = parseCache.get(cacheKey);
    if (cached != null) {
        return cached;
    }
    
    QueryIntent intent = parse(llmOutput);
    parseCache.put(cacheKey, intent);
    return intent;
}
```

### 并发处理

```java
// 线程安全的解析
public synchronized QueryIntent parse(String llmOutput) {
    // 解析逻辑
}
```

## 测试和验证

### 单元测试

```java
@Test
public void testParsePriceRangeQuery() {
    String llmOutput = """
        我来帮您查找价格在100到500之间的商品。
        ```json
        {
            "query_type": "price_range",
            "conditions": {
                "min_price": 100,
                "max_price": 500
            },
            "sort": "price ASC"
        }
        ```
        """;
    
    QueryIntent intent = parser.parse(llmOutput);
    
    assertEquals("price_range", intent.getQueryType());
    assertEquals(100, intent.getConditions().get("min_price"));
    assertEquals(500, intent.getConditions().get("max_price"));
    assertEquals("price ASC", intent.getSort());
}
```

### 集成测试

```java
@Test
public void testFullParsingFlow() {
    // 模拟LLM响应
    String llmResponse = generateMockLLMResponse();
    
    // 完整解析流程
    QueryIntent intent = llmOutputParser.parse(llmResponse);
    
    // 验证解析结果
    assertTrue(intent.isValid());
    assertNotNull(intent.getQueryType());
    assertNotNull(intent.getConditions());
}
```

## 最佳实践

### 1. 提示词设计

```java
// 明确的JSON格式要求
private static final String SYSTEM_PROMPT = """
    你是一个智能商品问答助手。
    
    要求：
    1. 必须返回有效的JSON格式
    2. JSON必须包含query_type和conditions字段
    3. 使用双引号，不要使用单引号
    4. 不要添加注释或额外文本
    
    返回格式：
    {
        "query_type": "查询类型",
        "conditions": {查询条件},
        "sort": "排序方式",
        "limit": 数量限制
    }
    """;
```

### 2. 错误恢复

```java
public QueryIntent parseWithFallback(String llmOutput) {
    try {
        return parse(llmOutput);
    } catch (LLMOutputParseException e) {
        // 使用默认查询
        logger.warn("解析失败，使用默认查询: {}", e.getMessage());
        return createDefaultQueryIntent();
    }
}
```

### 3. 日志记录

```java
private static final Logger logger = LoggerFactory.getLogger(LLMOutputParser.class);

public QueryIntent parse(String llmOutput) {
    logger.debug("开始解析LLM输出: {}", llmOutput);
    
    try {
        QueryIntent intent = doParse(llmOutput);
        logger.debug("解析成功: {}", intent);
        return intent;
    } catch (Exception e) {
        logger.error("解析失败: {}", e.getMessage());
        throw e;
    }
}
```

## 总结

LLM响应解析机制通过多阶段处理，确保从自然语言输出中提取准确的查询意图：

1. **JSON提取**：从文本中提取结构化数据
2. **格式修复**：处理常见的JSON格式问题
3. **意图解析**：转换为标准化的查询意图
4. **验证检查**：确保查询意图的有效性
5. **错误处理**：完善的异常处理和容错机制

该机制为商品问答场景提供了可靠的LLM输出解析能力。