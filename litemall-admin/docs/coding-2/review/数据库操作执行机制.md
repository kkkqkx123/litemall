# 数据库操作执行机制

## 概述

数据库操作执行机制负责将解析后的查询意图转换为实际的SQL语句，并执行数据库查询操作。该机制采用参数化查询和连接池管理，确保查询的安全性和性能。

## 架构设计

### 核心组件

```
QueryIntent（查询意图）
    ↓
SQLBuilder（SQL构建）
    ↓
ParameterBinder（参数绑定）
    ↓
GoodsQueryService（查询执行）
    ↓
数据库连接池
    ↓
数据库查询结果
```

## SQL构建机制

### SQLBuilder

`SQLBuilder`根据查询意图生成相应的SQL语句：

```java
@Component
public class SQLBuilder {
    
    // 基础查询字段
    private static final String BASE_QUERY_FIELDS = 
        "id, name, brief, pic_url, counter_price, retail_price, is_on_sale, add_time";
    
    public String buildQuerySQL(QueryIntent intent) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ").append(BASE_QUERY_FIELDS);
        sql.append(" FROM litemall_goods WHERE 1=1");
        
        // 添加查询条件
        appendConditions(sql, intent);
        
        // 添加排序
        if (intent.getSort() != null && !intent.getSort().trim().isEmpty()) {
            sql.append(" ORDER BY ").append(intent.getSort());
        }
        
        // 添加限制
        if (intent.getLimit() != null && intent.getLimit() > 0) {
            sql.append(" LIMIT ?");
        }
        
        return sql.toString();
    }
    
    private void appendConditions(StringBuilder sql, QueryIntent intent) {
        Map<String, Object> conditions = intent.getConditions();
        
        switch (intent.getQueryType()) {
            case "price_range":
                appendPriceRangeConditions(sql, conditions);
                break;
            case "stock_check":
                appendStockConditions(sql, conditions);
                break;
            case "category_filter":
                appendCategoryConditions(sql, conditions);
                break;
            case "keyword_search":
                appendKeywordConditions(sql, conditions);
                break;
            case "name_pattern":
                appendNamePatternConditions(sql, conditions);
                break;
            case "specific_product":
                appendSpecificProductConditions(sql, conditions);
                break;
            case "statistical":
                // 统计查询使用不同的方法
                break;
        }
    }
}
```

### 条件构建示例

#### 价格范围条件

```java
private void appendPriceRangeConditions(StringBuilder sql, Map<String, Object> conditions) {
    if (conditions.containsKey("min_price")) {
        sql.append(" AND retail_price >= ?");
    }
    if (conditions.containsKey("max_price")) {
        sql.append(" AND retail_price <= ?");
    }
    // 只查询在售商品
    sql.append(" AND is_on_sale = 1");
}
```

#### 名称模式匹配条件

```java
private void appendNamePatternConditions(StringBuilder sql, Map<String, Object> conditions) {
    Object nameCondition = conditions.get("name");
    if (nameCondition instanceof Map) {
        Map<String, Object> nameMap = (Map<String, Object>) nameCondition;
        String mode = (String) nameMap.get("mode");
        Boolean caseSensitive = (Boolean) nameMap.get("case_sensitive");
        
        switch (mode) {
            case "exact":
                if (caseSensitive != null && caseSensitive) {
                    sql.append(" AND name = ?");
                } else {
                    sql.append(" AND LOWER(name) = LOWER(?)");
                }
                break;
            case "contains":
                if (caseSensitive != null && caseSensitive) {
                    sql.append(" AND name LIKE ?");
                } else {
                    sql.append(" AND LOWER(name) LIKE LOWER(?)");
                }
                break;
            case "starts_with":
                if (caseSensitive != null && caseSensitive) {
                    sql.append(" AND name LIKE ?");
                } else {
                    sql.append(" AND LOWER(name) LIKE LOWER(?)");
                }
                break;
            case "ends_with":
                if (caseSensitive != null && caseSensitive) {
                    sql.append(" AND name LIKE ?");
                } else {
                    sql.append(" AND LOWER(name) LIKE LOWER(?)");
                }
                break;
            case "regex":
                sql.append(" AND name REGEXP ?");
                break;
        }
    }
    
    sql.append(" AND is_on_sale = 1");
}
```

## 参数绑定机制

### ParameterBinder

`ParameterBinder`负责将查询条件中的参数绑定到PreparedStatement：

```java
@Component
public class ParameterBinder {
    
    public void bindParameters(PreparedStatement stmt, QueryIntent intent) throws SQLException {
        List<Object> parameters = new ArrayList<>();
        extractParameters(intent, parameters);
        
        for (int i = 0; i < parameters.size(); i++) {
            Object param = parameters.get(i);
            setParameter(stmt, i + 1, param);
        }
    }
    
    private void extractParameters(QueryIntent intent, List<Object> parameters) {
        Map<String, Object> conditions = intent.getConditions();
        
        switch (intent.getQueryType()) {
            case "price_range":
                extractPriceParameters(conditions, parameters);
                break;
            case "stock_check":
                extractStockParameters(conditions, parameters);
                break;
            case "category_filter":
                extractCategoryParameters(conditions, parameters);
                break;
            case "keyword_search":
                extractKeywordParameters(conditions, parameters);
                break;
            case "name_pattern":
                extractNameParameters(conditions, parameters);
                break;
            case "specific_product":
                extractSpecificProductParameters(conditions, parameters);
                break;
        }
        
        // 添加LIMIT参数
        if (intent.getLimit() != null && intent.getLimit() > 0) {
            parameters.add(intent.getLimit());
        }
    }
    
    private void extractPriceParameters(Map<String, Object> conditions, List<Object> parameters) {
        if (conditions.containsKey("min_price")) {
            parameters.add(conditions.get("min_price"));
        }
        if (conditions.containsKey("max_price")) {
            parameters.add(conditions.get("max_price"));
        }
    }
}
```

### 参数类型处理

```java
private void setParameter(PreparedStatement stmt, int index, Object param) throws SQLException {
    if (param == null) {
        stmt.setNull(index, Types.NULL);
    } else if (param instanceof String) {
        stmt.setString(index, (String) param);
    } else if (param instanceof Integer) {
        stmt.setInt(index, (Integer) param);
    } else if (param instanceof Long) {
        stmt.setLong(index, (Long) param);
    } else if (param instanceof Double) {
        stmt.setDouble(index, (Double) param);
    } else if (param instanceof Float) {
        stmt.setFloat(index, (Float) param);
    } else if (param instanceof Boolean) {
        stmt.setBoolean(index, (Boolean) param);
    } else if (param instanceof Date) {
        stmt.setTimestamp(index, new Timestamp(((Date) param).getTime()));
    } else {
        stmt.setObject(index, param);
    }
}
```

## 查询执行服务

### GoodsQueryService

`GoodsQueryService`负责执行数据库查询：

```java
@Service
public class GoodsQueryService {
    
    @Autowired
    private DataSource dataSource;
    
    @Autowired
    private SQLBuilder sqlBuilder;
    
    @Autowired
    private ParameterBinder parameterBinder;
    
    public List<Goods> executeQuery(QueryIntent intent) throws SQLException {
        String sql = sqlBuilder.buildQuerySQL(intent);
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            // 绑定参数
            parameterBinder.bindParameters(stmt, intent);
            
            // 执行查询
            try (ResultSet rs = stmt.executeQuery()) {
                return convertResultSetToGoodsList(rs);
            }
        }
    }
    
    private List<Goods> convertResultSetToGoodsList(ResultSet rs) throws SQLException {
        List<Goods> goodsList = new ArrayList<>();
        
        while (rs.next()) {
            Goods goods = new Goods();
            goods.setId(rs.getInt("id"));
            goods.setName(rs.getString("name"));
            goods.setBrief(rs.getString("brief"));
            goods.setPicUrl(rs.getString("pic_url"));
            goods.setCounterPrice(rs.getBigDecimal("counter_price"));
            goods.setRetailPrice(rs.getBigDecimal("retail_price"));
            goods.setIsOnSale(rs.getBoolean("is_on_sale"));
            goods.setAddTime(rs.getTimestamp("add_time"));
            
            goodsList.add(goods);
        }
        
        return goodsList;
    }
}
```

### 统计查询执行

```java
public Object executeStatisticalQuery(QueryIntent intent) throws SQLException {
    Map<String, Object> conditions = intent.getConditions();
    String statisticType = (String) conditions.get("statistic_type");
    
    switch (statisticType) {
        case "total_count":
            return getTotalGoodsCount(conditions);
        case "price_range":
            return getPriceRange(conditions);
        case "stock_stats":
            return getStockStatistics(conditions);
        case "category_stats":
            return getCategoryStatistics(conditions);
        default:
            throw new IllegalArgumentException("不支持的统计类型: " + statisticType);
    }
}

private Long getTotalGoodsCount(Map<String, Object> conditions) throws SQLException {
    StringBuilder sql = new StringBuilder();
    sql.append("SELECT COUNT(*) FROM litemall_goods WHERE 1=1");
    
    List<Object> parameters = new ArrayList<>();
    appendConditions(sql, conditions, parameters);
    
    try (Connection conn = dataSource.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
        
        for (int i = 0; i < parameters.size(); i++) {
            setParameter(stmt, i + 1, parameters.get(i));
        }
        
        try (ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0L;
        }
    }
}
```

## 连接池管理

### 数据源配置

```java
@Configuration
public class DatabaseConfig {
    
    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://localhost:3306/litemall?useSSL=false&serverTimezone=UTC");
        config.setUsername("kkkqkx");
        config.setPassword("1234567kk");
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        
        // 连接池配置
        config.setMaximumPoolSize(20);
        config.setMinimumIdle(5);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        
        // 性能优化
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        
        return new HikariDataSource(config);
    }
}
```

## 事务管理

### 查询事务

```java
@Service
@Transactional(readOnly = true)
public class GoodsQueryService {
    
    // 只读查询，不需要事务管理
    public List<Goods> executeQuery(QueryIntent intent) {
        // 查询逻辑
    }
    
    // 需要事务的操作
    @Transactional
    public void updateGoodsStock(Integer goodsId, Integer newStock) {
        // 更新库存逻辑
    }
}
```

## 性能优化

### 查询优化

```java
public class QueryOptimizer {
    
    // 索引优化
    public void optimizeQuery(QueryIntent intent) {
        // 添加必要的索引提示
        if (intent.getQueryType().equals("keyword_search")) {
            // 使用全文索引
            intent.setHint("USE INDEX (idx_goods_name_fulltext)");
        }
        
        // 限制查询范围
        if (intent.getLimit() == null || intent.getLimit() > 100) {
            intent.setLimit(100);
        }
    }
    
    // 查询缓存
    public List<Goods> executeQueryWithCache(QueryIntent intent) {
        String cacheKey = generateCacheKey(intent);
        
        List<Goods> cached = cache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        
        List<Goods> result = executeQuery(intent);
        cache.put(cacheKey, result, Duration.ofMinutes(5));
        
        return result;
    }
}
```

### 批量查询

```java
public List<Goods> executeBatchQuery(List<QueryIntent> intents) throws SQLException {
    List<Goods> allResults = new ArrayList<>();
    
    try (Connection conn = dataSource.getConnection()) {
        conn.setAutoCommit(false);
        
        for (QueryIntent intent : intents) {
            String sql = sqlBuilder.buildQuerySQL(intent);
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                parameterBinder.bindParameters(stmt, intent);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    allResults.addAll(convertResultSetToGoodsList(rs));
                }
            }
        }
        
        conn.commit();
    }
    
    return allResults;
}
```

## 错误处理

### 数据库异常处理

```java
public class DatabaseExceptionHandler {
    
    public List<Goods> executeQueryWithErrorHandling(QueryIntent intent) {
        try {
            return executeQuery(intent);
        } catch (SQLException e) {
            logger.error("数据库查询失败: {}", e.getMessage(), e);
            
            // 根据错误类型进行处理
            if (e.getErrorCode() == 1045) {
                throw new RuntimeException("数据库连接失败，请检查配置");
            } else if (e.getErrorCode() == 1146) {
                throw new RuntimeException("数据表不存在");
            } else {
                throw new RuntimeException("数据库查询异常");
            }
        }
    }
}
```

### 连接池监控

```java
@Component
public class ConnectionPoolMonitor {
    
    @Autowired
    private DataSource dataSource;
    
    @Scheduled(fixedDelay = 60000) // 每分钟检查一次
    public void monitorConnectionPool() {
        if (dataSource instanceof HikariDataSource) {
            HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
            
            HikariPoolMXBean poolMXBean = hikariDataSource.getHikariPoolMXBean();
            
            logger.info("连接池状态 - 活跃连接数: {}, 空闲连接数: {}, 等待连接数: {}",
                poolMXBean.getActiveConnections(),
                poolMXBean.getIdleConnections(),
                poolMXBean.getThreadsAwaitingConnection());
            
            // 告警机制
            if (poolMXBean.getThreadsAwaitingConnection() > 10) {
                logger.warn("连接池压力过大，等待连接数超过10个");
            }
        }
    }
}
```

## 测试策略

### 单元测试

```java
@SpringBootTest
public class GoodsQueryServiceTest {
    
    @Autowired
    private GoodsQueryService goodsQueryService;
    
    @Test
    public void testExecutePriceRangeQuery() throws SQLException {
        QueryIntent intent = new QueryIntent();
        intent.setQueryType("price_range");
        
        Map<String, Object> conditions = new HashMap<>();
        conditions.put("min_price", 100.0);
        conditions.put("max_price", 500.0);
        intent.setConditions(conditions);
        
        List<Goods> results = goodsQueryService.executeQuery(intent);
        
        assertNotNull(results);
        assertFalse(results.isEmpty());
        
        for (Goods goods : results) {
            assertTrue(goods.getRetailPrice().doubleValue() >= 100.0);
            assertTrue(goods.getRetailPrice().doubleValue() <= 500.0);
        }
    }
}
```

### 集成测试

```java
@Test
public void testFullQueryFlow() throws SQLException {
    // 1. 创建查询意图
    QueryIntent intent = createTestQueryIntent();
    
    // 2. 构建SQL
    String sql = sqlBuilder.buildQuerySQL(intent);
    assertNotNull(sql);
    
    // 3. 执行查询
    List<Goods> results = goodsQueryService.executeQuery(intent);
    
    // 4. 验证结果
    assertNotNull(results);
    assertFalse(results.isEmpty());
    
    // 5. 验证参数绑定
    verifyParameterBinding(intent);
}
```

## 安全考虑

### SQL注入防护

```java
// 使用参数化查询，避免字符串拼接
private String buildSafeQuery(QueryIntent intent) {
    // ❌ 错误：字符串拼接
    // String sql = "SELECT * FROM goods WHERE name = '" + name + "'";
    
    // ✅ 正确：参数化查询
    String sql = "SELECT * FROM goods WHERE name = ?";
    
    return sql;
}
```

### 输入验证

```java
public void validateQueryIntent(QueryIntent intent) {
    // 验证查询类型
    if (!isValidQueryType(intent.getQueryType())) {
        throw new IllegalArgumentException("无效的查询类型");
    }
    
    // 验证参数范围
    Map<String, Object> conditions = intent.getConditions();
    if (conditions.containsKey("limit")) {
        Integer limit = (Integer) conditions.get("limit");
        if (limit > 1000) {
            throw new IllegalArgumentException("查询限制不能超过1000");
        }
    }
}
```

## 监控和日志

### 查询日志

```java
@Component
public class QueryLogger {
    
    private static final Logger logger = LoggerFactory.getLogger(QueryLogger.class);
    
    public void logQuery(QueryIntent intent, String sql, long executionTime) {
        logger.info("查询执行 - 类型: {}, SQL: {}, 执行时间: {}ms", 
            intent.getQueryType(), sql, executionTime);
        
        // 慢查询告警
        if (executionTime > 1000) {
            logger.warn("慢查询告警 - 执行时间超过1秒: {}", intent);
        }
    }
}
```

## 总结

数据库操作执行机制通过以下方式确保查询的安全性和性能：

1. **参数化查询**：防止SQL注入攻击
2. **连接池管理**：提高数据库连接效率
3. **查询优化**：提升查询性能
4. **错误处理**：完善的异常处理机制
5. **监控日志**：实时监控和告警
6. **测试策略**：全面的测试覆盖

该机制为商品问答系统提供了可靠的数据库查询能力。