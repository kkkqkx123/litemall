# 大模型商品问答功能 - 设计方案

## 1. 架构设计

### 1.1 整体架构
```
用户提问 → 提示词模板 → LLM API → JSON解析 → SQL生成 → 数据库查询 → 结果格式化 → 回答
```

### 1.2 核心组件
- **LLM服务层**: 调用大模型API，生成结构化查询
- **格式化解析器**: 解析LLM输出的JSON格式数据
- **查询执行器**: 执行生成的SQL查询
- **结果处理器**: 将查询结果格式化为自然语言回答

## 2. LLM提示词设计

### 2.1 查询生成提示词
```
你是一个商品数据库查询助手。请根据用户问题生成JSON格式的查询指令。

数据库表结构：
- litemall_goods: 商品表
  - id: 商品ID
  - name: 商品名称
  - brief: 商品简介
  - price: 价格
  - number: 库存数量
  - category_id: 分类ID
  - is_on_sale: 是否在售(0:下架,1:在售)

可用查询类型：
- price_range: 价格范围查询
- stock_check: 库存查询  
- category_filter: 分类筛选
- keyword_search: 关键词搜索
- statistical: 统计查询

输出格式要求：
{
  "query_type": "查询类型",
  "conditions": {
    "字段名": "条件值"
  },
  "sort": "排序方式",
  "limit": 返回数量限制
}

用户问题：{question}

请只返回JSON，不要其他解释。
```

### 2.2 回答生成提示词
```
你是一个商品销售助手。请根据查询结果生成友好的回答。

查询结果：{query_result}
用户问题：{question}

要求：
1. 用自然语言描述结果
2. 包含具体的商品信息
3. 如果结果为空，给出合适的建议
4. 保持回答简洁明了
```

## 3. 格式化输出解析模块设计

### 3.1 JSON解析器
```java
@Component
public class LLMOutputParser {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    public QueryIntent parseQueryIntent(String llmOutput) {
        try {
            // 提取JSON部分
            String jsonStr = extractJsonFromText(llmOutput);
            return objectMapper.readValue(jsonStr, QueryIntent.class);
        } catch (Exception e) {
            throw new LLMParseException("解析LLM输出失败", e);
        }
    }
    
    private String extractJsonFromText(String text) {
        // 提取文本中的JSON部分
        Pattern pattern = Pattern.compile("\\{[\\s\\S]*\\}");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group();
        }
        throw new LLMParseException("未找到JSON格式输出");
    }
}
```

### 3.2 查询意图模型
```java
@Data
public class QueryIntent {
    private String queryType;      // 查询类型
    private Map<String, Object> conditions;  // 查询条件
    private String sort;           // 排序方式
    private Integer limit;         // 返回限制
    
    // 验证查询意图的合法性
    public boolean isValid() {
        return queryType != null && conditions != null;
    }
}
```

### 3.3 SQL生成器
```java
@Component
public class SQLGenerator {
    
    public String generateSQL(QueryIntent intent) {
        switch (intent.getQueryType()) {
            case "price_range":
                return generatePriceRangeSQL(intent);
            case "stock_check":
                return generateStockCheckSQL(intent);
            case "category_filter":
                return generateCategoryFilterSQL(intent);
            case "keyword_search":
                return generateKeywordSearchSQL(intent);
            case "statistical":
                return generateStatisticalSQL(intent);
            default:
                throw new UnsupportedQueryTypeException("不支持的查询类型: " + intent.getQueryType());
        }
    }
    
    private String generatePriceRangeSQL(QueryIntent intent) {
        Map<String, Object> conditions = intent.getConditions();
        Double minPrice = (Double) conditions.get("min_price");
        Double maxPrice = (Double) conditions.get("max_price");
        
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT id, name, price, number FROM litemall_goods ");
        sql.append("WHERE is_on_sale = 1 ");
        
        if (minPrice != null) {
            sql.append("AND price >= ").append(minPrice).append(" ");
        }
        if (maxPrice != null) {
            sql.append("AND price <= ").append(maxPrice).append(" ");
        }
        
        sql.append("ORDER BY price ASC");
        if (intent.getLimit() != null) {
            sql.append(" LIMIT ").append(intent.getLimit());
        }
        
        return sql.toString();
    }
}
```

## 4. 多轮对话支持

### 4.1 会话管理
```java
@Service
public class ConversationManager {
    
    private final Map<String, ConversationSession> sessions = new ConcurrentHashMap<>();
    private static final int MAX_HISTORY = 5;  // 最大历史记录数
    
    public ConversationSession getSession(String sessionId) {
        return sessions.computeIfAbsent(sessionId, k -> new ConversationSession());
    }
    
    public void addToHistory(String sessionId, String question, String answer, List<Goods> results) {
        ConversationSession session = getSession(sessionId);
        session.addHistory(question, answer, results);
        
        // 限制历史记录数量
        if (session.getHistory().size() > MAX_HISTORY) {
            session.getHistory().remove(0);
        }
    }
}
```

### 4.2 上下文构建
```java
public class ContextBuilder {
    
    public String buildContext(String sessionId, String currentQuestion, 
                                ConversationManager conversationManager) {
        ConversationSession session = conversationManager.getSession(sessionId);
        List<ConversationTurn> history = session.getHistory();
        
        if (history.isEmpty()) {
            return currentQuestion;
        }
        
        StringBuilder context = new StringBuilder();
        context.append("历史对话：\\n");
        
        for (int i = 0; i < history.size(); i++) {
            ConversationTurn turn = history.get(i);
            context.append(i + 1).append(". 用户：").append(turn.getQuestion()).append("\\n");
            context.append("   回答：").append(turn.getAnswer()).append("\\n");
        }
        
        context.append("\\n当前问题：").append(currentQuestion);
        return context.toString();
    }
}
```

## 5. 错误处理与降级

### 5.1 解析失败处理
```java
@Component
public class FallbackHandler {
    
    public String handleParseFailure(String question) {
        // 使用预定义的规则进行简单匹配
        if (question.contains("价格") && question.contains("元")) {
            return handlePriceQuestion(question);
        } else if (question.contains("库存")) {
            return handleStockQuestion(question);
        } else {
            return "抱歉，我无法理解您的问题。请尝试询问价格、库存或分类相关的问题。";
        }
    }
    
    private String handlePriceQuestion(String question) {
        // 简单的价格范围提取
        Pattern pattern = Pattern.compile("(\\d+).*?元");
        Matcher matcher = pattern.matcher(question);
        
        if (matcher.find()) {
            int price = Integer.parseInt(matcher.group(1));
            return "我可以帮您查询价格相关的商品信息。请告诉我具体的价格范围，比如'100-200元的商品'。";
        }
        
        return "请提供具体的价格范围，例如'100元以下的商品'。";
    }
}
```

### 5.2 LLM调用异常处理
```java
@Component
public class LLMExceptionHandler {
    
    public String handleLLMException(Exception e, String question) {
        if (e instanceof TimeoutException) {
            return "抱歉，AI服务响应超时，请稍后重试。";
        } else if (e instanceof LLMParseException) {
            return "抱歉，AI返回格式有误，请重新提问。";
        } else {
            log.error("LLM调用异常", e);
            return "抱歉，AI服务暂时不可用，请稍后再试。";
        }
    }
}
```

## 6. 性能优化

### 6.1 缓存策略
```java
@Service
public class QueryCacheService {
    
    private final Cache<String, List<Goods>> queryCache = Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build();
    
    public List<Goods> getCachedResult(String queryKey) {
        return queryCache.getIfPresent(queryKey);
    }
    
    public void cacheResult(String queryKey, List<Goods> result) {
        queryCache.put(queryKey, result);
    }
    
    public String generateCacheKey(QueryIntent intent) {
        return intent.getQueryType() + ":" + intent.getConditions().toString();
    }
}
```

### 6.2 异步处理
```java
@Service
public class AsyncQueryService {
    
    @Async
    public CompletableFuture<List<Goods>> executeQueryAsync(String sql) {
        return CompletableFuture.supplyAsync(() -> {
            // 执行数据库查询
            return goodsQueryService.executeQuery(sql);
        });
    }
}
```