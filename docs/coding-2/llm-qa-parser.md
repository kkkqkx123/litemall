# 大模型商品问答功能 - 格式化输出解析模块设计

## 1. 模块架构

### 1.1 核心组件
```
LLM输出 → JSON提取器 → 格式验证器 → 查询构建器 → SQL生成器
```

### 1.2 类结构
```
LLMOutputParser (LLM输出解析器)
├── JSONExtractor (JSON提取器)
├── SchemaValidator (模式验证器)
├── QueryIntentBuilder (查询意图构建器)
└── SQLQueryBuilder (SQL查询构建器)
```

## 2. LLM提示词设计

### 2.1 查询生成提示词
```
你是一个商品数据库查询专家。请根据用户问题生成结构化的JSON查询指令。

数据库表结构：
表名：litemall_goods
字段：
- id (商品ID，整数)
- name (商品名称，字符串)
- brief (商品简介，字符串)
- price (价格，小数)
- number (库存数量，整数)
- category_id (分类ID，整数)
- is_on_sale (是否在售，0:下架,1:在售)

可用查询类型：
1. price_range - 价格范围查询
2. stock_check - 库存检查
3. category_filter - 分类筛选
4. keyword_search - 关键词搜索
5. name_pattern - 名称模式匹配
6. specific_product - 特定商品查询
7. statistical - 统计查询

名称匹配模式：
- exact - 精确匹配
- contains - 包含匹配
- starts_with - 前缀匹配
- ends_with - 后缀匹配
- regex - 正则表达式匹配

输出要求：
- 必须返回有效的JSON格式
- 包含query_type、conditions、sort、limit字段
- conditions中的字段必须是数据库中存在的字段
- price字段使用数值范围，格式：{"min": 100, "max": 200}
- number字段使用库存条件，格式：{"min": 10, "max": null}
- name字段支持多种匹配模式，格式：{"pattern": "手机", "mode": "contains", "case_sensitive": false}
- 支持多个查询条件组合

示例输出：
{
  "query_type": "name_pattern",
  "conditions": {
    "name": {"pattern": "手机", "mode": "contains", "case_sensitive": false},
    "price": {"min": 100, "max": 200},
    "is_on_sale": 1
  },
  "sort": "price ASC",
  "limit": 10
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
2. 包含具体的商品信息（名称、价格、库存）
3. 如果结果为空，给出合适的建议
4. 保持回答简洁明了，不超过200字
5. 使用中文回答

示例回答：
"为您找到3个价格在100-200元的商品：
1. 商品A - 价格：150元，库存：50件
2. 商品B - 价格：180元，库存：30件  
3. 商品C - 价格：199元，库存：20件
这些商品都有现货，可以直接购买。"
```

## 3. JSON提取器设计

### 3.1 基础提取器
```java
@Component
public class JSONExtractor {
    
    private static final Pattern JSON_PATTERN = Pattern.compile(
        "\\{[\\s\\S]*\\}", Pattern.MULTILINE
    );
    
    public String extractJSON(String text) throws LLMOutputParseException {
        if (text == null || text.trim().isEmpty()) {
            throw new LLMOutputParseException("LLM输出为空");
        }
        
        // 移除Markdown代码块标记
        String cleanedText = removeMarkdownCodeBlocks(text);
        
        // 提取JSON部分
        Matcher matcher = JSON_PATTERN.matcher(cleanedText);
        if (matcher.find()) {
            String jsonStr = matcher.group();
            
            // 验证JSON格式
            try {
                new JSONObject(jsonStr);
                return jsonStr;
            } catch (JSONException e) {
                // 尝试修复常见的JSON格式问题
                return fixCommonJSONIssues(jsonStr);
            }
        }
        
        throw new LLMOutputParseException("未找到有效的JSON输出");
    }
    
    private String removeMarkdownCodeBlocks(String text) {
        // 移除```json和```标记
        return text.replaceAll("```json\\n?", "")
                  .replaceAll("```\\n?", "")
                  .trim();
    }
    
    private String fixCommonJSONIssues(String jsonStr) {
        // 修复末尾多余的逗号
        jsonStr = jsonStr.replaceAll(",\\s*}", "}");
        jsonStr = jsonStr.replaceAll(",\\s*]", "]");
        
        // 修复单引号
        jsonStr = jsonStr.replaceAll("'", "\"");
        
        // 验证修复后的JSON
        try {
            new JSONObject(jsonStr);
            return jsonStr;
        } catch (JSONException e) {
            throw new LLMOutputParseException("JSON格式修复失败: " + e.getMessage());
        }
    }
}
```

### 3.2 智能提取器（处理复杂输出）
```java
@Component
public class SmartJSONExtractor extends JSONExtractor {
    
    @Override
    public String extractJSON(String text) throws LLMOutputParseException {
        try {
            return super.extractJSON(text);
        } catch (LLMOutputParseException e) {
            // 尝试从复杂输出中提取JSON
            return extractJSONFromComplexOutput(text);
        }
    }
    
    private String extractJSONFromComplexOutput(String text) {
        // 查找可能的JSON开始标记
        int jsonStart = findJSONStart(text);
        if (jsonStart == -1) {
            throw new LLMOutputParseException("无法找到JSON开始位置");
        }
        
        // 查找JSON结束位置
        int jsonEnd = findJSONEnd(text, jsonStart);
        if (jsonEnd == -1) {
            throw new LLMOutputParseException("无法找到JSON结束位置");
        }
        
        String jsonStr = text.substring(jsonStart, jsonEnd + 1);
        
        // 清理和修复
        jsonStr = cleanAndFixJSON(jsonStr);
        
        return jsonStr;
    }
    
    private int findJSONStart(String text) {
        // 查找第一个{字符
        int start = text.indexOf('{');
        
        // 如果前面有"JSON:"或"输出:"等标记，从那里开始
        if (start > 0) {
            String beforeStart = text.substring(0, start).toLowerCase();
            if (beforeStart.contains("json:") || beforeStart.contains("输出:")) {
                return start;
            }
        }
        
        return start;
    }
    
    private int findJSONEnd(String text, int start) {
        int braceCount = 0;
        boolean inString = false;
        boolean escaped = false;
        
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            
            if (escaped) {
                escaped = false;
                continue;
            }
            
            if (c == '\\') {
                escaped = true;
                continue;
            }
            
            if (c == '"' && !inString) {
                inString = true;
            } else if (c == '"' && inString) {
                inString = false;
            }
            
            if (!inString) {
                if (c == '{') {
                    braceCount++;
                } else if (c == '}') {
                    braceCount--;
                    if (braceCount == 0) {
                        return i;
                    }
                }
            }
        }
        
        return -1;
    }
}
```

## 4. 模式验证器设计

### 4.1 JSON模式定义
```java
public class QuerySchema {
    
    private static final Map<String, Object> SCHEMA = Map.of(
        "type", "object",
        "properties", Map.of(
            "query_type", Map.of(
                "type", "string",
                "enum", List.of("price_range", "stock_check", "category_filter", "keyword_search", "name_pattern", "specific_product", "statistical")
            ),
            "conditions", Map.of(
                "type", "object",
                "properties", Map.of(
                    "price", Map.of("type", "object"),
                    "number", Map.of("type", "object"),
                    "category_id", Map.of("type", "integer"),
                    "is_on_sale", Map.of("type", "integer", "enum", List.of(0, 1)),
                    "name", Map.of(
                        "type", "object",
                        "properties", Map.of(
                            "pattern", Map.of("type", "string"),
                            "mode", Map.of("type", "string", "enum", List.of("exact", "contains", "starts_with", "ends_with", "regex")),
                            "case_sensitive", Map.of("type", "boolean")
                        )
                    ),
                    "brief", Map.of(
                        "type", "object",
                        "properties", Map.of(
                            "pattern", Map.of("type", "string"),
                            "mode", Map.of("type", "string", "enum", List.of("exact", "contains", "starts_with", "ends_with", "regex")),
                            "case_sensitive", Map.of("type", "boolean")
                        )
                    )
                )
            ),
            "sort", Map.of("type", "string"),
            "limit", Map.of("type", "integer", "minimum", 1, "maximum", 100)
        ),
        "required", List.of("query_type", "conditions")
    );
}
```

### 4.2 模式验证器实现
```java
@Component
public class SchemaValidator {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    public void validateQueryIntent(String jsonStr) throws ValidationException {
        try {
            JsonNode rootNode = objectMapper.readTree(jsonStr);
            
            // 验证必需字段
            validateRequiredFields(rootNode);
            
            // 验证查询类型
            validateQueryType(rootNode);
            
            // 验证条件字段
            validateConditions(rootNode);
            
            // 验证排序字段
            validateSort(rootNode);
            
            // 验证限制数量
            validateLimit(rootNode);
            
        } catch (IOException e) {
            throw new ValidationException("JSON解析失败: " + e.getMessage());
        }
    }
    
    private void validateRequiredFields(JsonNode rootNode) throws ValidationException {
        if (!rootNode.has("query_type") || !rootNode.has("conditions")) {
            throw new ValidationException("缺少必需字段: query_type 或 conditions");
        }
    }
    
    private void validateQueryType(JsonNode rootNode) throws ValidationException {
        String queryType = rootNode.get("query_type").asText();
        List<String> validTypes = Arrays.asList(
            "price_range", "stock_check", "category_filter", "keyword_search", "statistical"
        );
        
        if (!validTypes.contains(queryType)) {
            throw new ValidationException("无效的查询类型: " + queryType);
        }
    }
    
    private void validateConditions(JsonNode rootNode) throws ValidationException {
        JsonNode conditions = rootNode.get("conditions");
        if (!conditions.isObject()) {
            throw new ValidationException("conditions必须是对象类型");
        }
        
        // 验证价格条件
        if (conditions.has("price")) {
            validatePriceCondition(conditions.get("price"));
        }
        
        // 验证库存条件
        if (conditions.has("number")) {
            validateStockCondition(conditions.get("number"));
        }
        
        // 验证其他条件
        validateOtherConditions(conditions);
    }
    
    private void validatePriceCondition(JsonNode priceNode) throws ValidationException {
        if (!priceNode.isObject()) {
            throw new ValidationException("price条件必须是对象类型");
        }
        
        JsonNode minNode = priceNode.get("min");
        JsonNode maxNode = priceNode.get("max");
        
        if (minNode != null && !minNode.isNumber()) {
            throw new ValidationException("price.min必须是数字");
        }
        
        if (maxNode != null && !maxNode.isNumber()) {
            throw new ValidationException("price.max必须是数字");
        }
        
        // 验证价格范围逻辑
        if (minNode != null && maxNode != null) {
            double min = minNode.asDouble();
            double max = maxNode.asDouble();
            if (min > max) {
                throw new ValidationException("price.min不能大于price.max");
            }
        }
    }
    
    private void validateStockCondition(JsonNode stockNode) throws ValidationException {
        if (!stockNode.isObject()) {
            throw new ValidationException("number条件必须是对象类型");
        }
        
        JsonNode minNode = stockNode.get("min");
        JsonNode maxNode = stockNode.get("max");
        
        if (minNode != null && !minNode.isInt()) {
            throw new ValidationException("number.min必须是整数");
        }
        
        if (maxNode != null && !maxNode.isInt()) {
            throw new ValidationException("number.max必须是整数");
        }
    }
}
```

## 5. 查询意图构建器

### 5.1 查询意图模型
```java
@Data
@Builder
public class QueryIntent {
    private String queryType;                    // 查询类型
    private Map<String, Condition> conditions;   // 查询条件
    private String sort;                         // 排序方式
    private Integer limit;                       // 返回限制
    
    @Data
    @Builder
    public static class Condition {
        private String field;                    // 字段名
        private String operator;                 // 操作符
        private Object value;                    // 值
        private Object minValue;                 // 最小值（范围查询）
        private Object maxValue;                 // 最大值（范围查询）
        private String pattern;                  // 模式匹配字符串
        private String matchMode;                // 匹配模式: exact, contains, starts_with, ends_with, regex
        private Boolean caseSensitive;           // 是否大小写敏感
        private List<String> patterns;           // 多个模式匹配
    }
}
```

### 5.2 意图构建器实现
```java
@Component
public class QueryIntentBuilder {
    
    public QueryIntent buildIntent(String jsonStr) throws IntentBuildException {
        try {
            JSONObject json = new JSONObject(jsonStr);
            
            QueryIntent.QueryIntentBuilder builder = QueryIntent.builder();
            
            // 设置查询类型
            builder.queryType(json.getString("query_type"));
            
            // 设置条件
            JSONObject conditionsJson = json.getJSONObject("conditions");
            Map<String, QueryIntent.Condition> conditions = buildConditions(conditionsJson);
            builder.conditions(conditions);
            
            // 设置排序
            if (json.has("sort")) {
                builder.sort(json.getString("sort"));
            }
            
            // 设置限制
            if (json.has("limit")) {
                builder.limit(json.getInt("limit"));
            }
            
            return builder.build();
            
        } catch (Exception e) {
            throw new IntentBuildException("构建查询意图失败: " + e.getMessage(), e);
        }
    }
    
    private Map<String, QueryIntent.Condition> buildConditions(JSONObject conditionsJson) {
        Map<String, QueryIntent.Condition> conditions = new HashMap<>();
        
        for (String key : conditionsJson.keySet()) {
            Object value = conditionsJson.get(key);
            QueryIntent.Condition condition = buildCondition(key, value);
            conditions.put(key, condition);
        }
        
        return conditions;
    }
    
    private QueryIntent.Condition buildCondition(String field, Object value) {
        QueryIntent.Condition.ConditionBuilder builder = QueryIntent.Condition.builder();
        builder.field(field);
        
        if (value instanceof JSONObject) {
            JSONObject jsonValue = (JSONObject) value;
            
            // 检查是否为模式匹配条件
            if (jsonValue.has("pattern")) {
                return buildPatternCondition(builder, jsonValue);
            }
            
            // 范围查询
            builder.operator("BETWEEN");
            
            if (jsonValue.has("min")) {
                builder.minValue(jsonValue.get("min"));
            }
            if (jsonValue.has("max")) {
                builder.maxValue(jsonValue.get("max"));
            }
            
        } else if (value instanceof String) {
            // 字符串查询（向后兼容）
            builder.operator("LIKE");
            builder.value("%" + value + "%");
            builder.pattern((String) value);
            builder.matchMode("contains");
            builder.caseSensitive(false);
            
        } else {
            // 精确匹配
            builder.operator("=");
            builder.value(value);
        }
        
        return builder.build();
    }
    
    private QueryIntent.Condition buildPatternCondition(QueryIntent.Condition.ConditionBuilder builder, JSONObject patternObj) {
        String pattern = patternObj.getString("pattern");
        String matchMode = patternObj.has("mode") ? patternObj.getString("mode") : "contains";
        Boolean caseSensitive = patternObj.has("case_sensitive") ? patternObj.getBoolean("case_sensitive") : false;
        
        builder.pattern(pattern);
        builder.matchMode(matchMode);
        builder.caseSensitive(caseSensitive);
        
        // 根据匹配模式生成对应的值
        switch (matchMode) {
            case "exact":
                builder.operator("=");
                builder.value(pattern);
                break;
            case "contains":
                builder.operator("LIKE");
                builder.value("%" + pattern + "%");
                break;
            case "starts_with":
                builder.operator("LIKE");
                builder.value(pattern + "%");
                break;
            case "ends_with":
                builder.operator("LIKE");
                builder.value("%" + pattern);
                break;
            case "regex":
                builder.operator("REGEXP");
                builder.value(pattern);
                break;
            default:
                builder.operator("LIKE");
                builder.value("%" + pattern + "%");
                break;
            default:
                builder.operator("LIKE");
                builder.value("%" + pattern + "%");
        }
        
        return builder.build();
    }
    
    // 构建多个模式匹配条件
    public QueryIntent.Condition buildMultiPatternCondition(String field, List<String> patterns, String matchMode, Boolean caseSensitive) {
        QueryIntent.Condition.ConditionBuilder builder = QueryIntent.Condition.builder();
        builder.field(field);
        builder.operator("IN");
        builder.patterns(patterns);
        builder.matchMode(matchMode);
        builder.caseSensitive(caseSensitive);
        
        // 生成IN查询的值列表
        List<String> values = patterns.stream()
            .map(pattern -> transformPattern(pattern, matchMode))
            .collect(Collectors.toList());
        
        builder.value(values);
        return builder.build();
    }
    
    private String transformPattern(String pattern, String matchMode) {
        switch (matchMode) {
            case "exact":
                return pattern;
            case "contains":
                return "%" + pattern + "%";
            case "starts_with":
                return pattern + "%";
            case "ends_with":
                return "%" + pattern;
            case "regex":
                return pattern;
            default:
                return pattern;
        }
    }
}
```

## 6. SQL查询构建器

### 6.1 基础SQL构建器
```java
@Component
public class SQLQueryBuilder {
    
    private static final String BASE_TABLE = "litemall_goods";
    private static final String[] SELECT_FIELDS = {
        "id", "name", "brief", "price", "number", "category_id", "is_on_sale", "pic_url"
    };
    
    public String buildSQL(QueryIntent intent) {
        StringBuilder sql = new StringBuilder();
        
        // 构建SELECT子句
        buildSelectClause(sql);
        
        // 构建FROM子句
        buildFromClause(sql);
        
        // 构建WHERE子句
        buildWhereClause(sql, intent.getConditions());
        
        // 构建ORDER BY子句
        buildOrderByClause(sql, intent.getSort());
        
        // 构建LIMIT子句
        buildLimitClause(sql, intent.getLimit());
        
        return sql.toString();
    }
    
    private void buildSelectClause(StringBuilder sql) {
        sql.append("SELECT ")
           .append(String.join(", ", SELECT_FIELDS))
           .append(" ");
    }
    
    private void buildFromClause(StringBuilder sql) {
        sql.append("FROM ")
           .append(BASE_TABLE)
           .append(" ");
    }
    
    private void buildWhereClause(StringBuilder sql, Map<String, QueryIntent.Condition> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            return;
        }
        
        sql.append("WHERE 1=1 ");
        
        for (QueryIntent.Condition condition : conditions.values()) {
            buildCondition(sql, condition);
        }
    }
    
    private void buildCondition(StringBuilder sql, QueryIntent.Condition condition) {
        String field = condition.getField();
        String operator = condition.getOperator();
        
        switch (operator) {
            case "=":
                sql.append("AND ").append(field).append(" = ? ");
                break;
                
            case "LIKE":
                sql.append("AND ").append(field).append(" LIKE ? ");
                break;
                
            case "REGEXP":
                sql.append("AND ").append(field).append(" REGEXP ? ");
                break;
                
            case "BETWEEN":
                if (condition.getMinValue() != null && condition.getMaxValue() != null) {
                    sql.append("AND ").append(field).append(" BETWEEN ? AND ? ");
                } else if (condition.getMinValue() != null) {
                    sql.append("AND ").append(field).append(" >= ? ");
                } else if (condition.getMaxValue() != null) {
                    sql.append("AND ").append(field).append(" <= ? ");
                }
                break;
                
            case "IN":
                buildInCondition(sql, field, condition);
                break;
                
            default:
                throw new SQLBuildException("不支持的操作符: " + operator);
        }
    }
    
    private void buildInCondition(StringBuilder sql, String field, QueryIntent.Condition condition) {
        Object value = condition.getValue();
        if (value instanceof List) {
            List<?> values = (List<?>) value;
            if (values.isEmpty()) {
                sql.append("AND 1=0 "); // 空列表时返回无结果
            } else {
                sql.append("AND ").append(field).append(" IN (");
                sql.append(String.join(", ", Collections.nCopies(values.size(), "?")));
                sql.append(") ");
            }
        } else {
            sql.append("AND ").append(field).append(" = ? ");
        }
    }
    
    private void buildOrderByClause(StringBuilder sql, String sort) {
        if (sort == null || sort.trim().isEmpty()) {
            return;
        }
        
        // 验证排序字段
        String[] sortParts = sort.split("\\s+");
        if (sortParts.length != 2) {
            throw new SQLBuildException("无效的排序格式: " + sort);
        }
        
        String field = sortParts[0];
        String direction = sortParts[1].toUpperCase();
        
        if (!Arrays.asList("ASC", "DESC").contains(direction)) {
            throw new SQLBuildException("无效的排序方向: " + direction);
        }
        
        sql.append("ORDER BY ").append(field).append(" ").append(direction).append(" ");
    }
    
    private void buildLimitClause(StringBuilder sql, Integer limit) {
        if (limit != null && limit > 0) {
            sql.append("LIMIT ").append(limit);
        }
    }
}
```

### 6.2 参数绑定器
```java
@Component
public class SQLParameterBinder {
    
    public List<Object> bindParameters(QueryIntent intent) {
        List<Object> parameters = new ArrayList<>();
        
        if (intent.getConditions() != null) {
            for (QueryIntent.Condition condition : intent.getConditions().values()) {
                bindConditionParameters(parameters, condition);
            }
        }
        
        return parameters;
    }
    
    private void bindConditionParameters(List<Object> parameters, QueryIntent.Condition condition) {
        String operator = condition.getOperator();
        
        switch (operator) {
            case "=":
            case "LIKE":
            case "REGEXP":
                parameters.add(condition.getValue());
                break;
                
            case "BETWEEN":
                if (condition.getMinValue() != null && condition.getMaxValue() != null) {
                    parameters.add(condition.getMinValue());
                    parameters.add(condition.getMaxValue());
                } else if (condition.getMinValue() != null) {
                    parameters.add(condition.getMinValue());
                } else if (condition.getMaxValue() != null) {
                    parameters.add(condition.getMaxValue());
                }
                break;
                
            case "IN":
                // 处理IN查询
                if (condition.getValue() instanceof List) {
                    parameters.addAll((List<?>) condition.getValue());
                } else {
                    parameters.add(condition.getValue());
                }
                break;
        }
    }
}
```

## 7. 完整解析流程

### 7.1 主解析器
```java
@Service
public class LLMOutputParser {
    
    @Autowired
    private JSONExtractor jsonExtractor;
    
    @Autowired
    private SchemaValidator schemaValidator;
    
    @Autowired
    private QueryIntentBuilder intentBuilder;
    
    @Autowired
    private SQLQueryBuilder sqlBuilder;
    
    @Autowired
    private SQLParameterBinder parameterBinder;
    
    public ParsedQuery parseLLMOutput(String llmOutput) throws ParseException {
        try {
            // 1. 提取JSON
            String jsonStr = jsonExtractor.extractJSON(llmOutput);
            
            // 2. 验证模式
            schemaValidator.validateQueryIntent(jsonStr);
            
            // 3. 构建查询意图
            QueryIntent intent = intentBuilder.buildIntent(jsonStr);
            
            // 4. 生成SQL
            String sql = sqlBuilder.buildSQL(intent);
            
            // 5. 绑定参数
            List<Object> parameters = parameterBinder.bindParameters(intent);
            
            return ParsedQuery.builder()
                .queryIntent(intent)
                .sql(sql)
                .parameters(parameters)
                .build();
                
        } catch (Exception e) {
            throw new ParseException("LLM输出解析失败", e);
        }
    }
}
```

### 7.2 解析结果模型
```java
@Data
@Builder
public class ParsedQuery {
    private QueryIntent queryIntent;     // 查询意图
    private String sql;                  // 生成的SQL
    private List<Object> parameters;     // SQL参数
    private Long parseTime;             // 解析耗时
}
```

## 8. 错误处理与降级

### 8.1 解析异常处理
```java
@Component
public class ParseExceptionHandler {
    
    private static final Logger log = LoggerFactory.getLogger(ParseExceptionHandler.class);
    
    public ParsedQuery handleParseException(String llmOutput, Exception e) {
        log.error("LLM输出解析失败: {}", llmOutput, e);
        
        // 尝试使用规则引擎进行解析
        return tryRuleBasedParsing(llmOutput);
    }
    
    private ParsedQuery tryRuleBasedParsing(String llmOutput) {
        // 基于关键词的简单规则解析
        if (llmOutput.contains("价格") && llmOutput.contains("元")) {
            return parsePriceQuestion(llmOutput);
        } else if (llmOutput.contains("库存")) {
            return parseStockQuestion(llmOutput);
        } else if (llmOutput.contains("分类")) {
            return parseCategoryQuestion(llmOutput);
        } else if (llmOutput.contains("手机") || llmOutput.contains("电脑") || llmOutput.contains("衣服")) {
            return parseProductCategoryQuestion(llmOutput);
        } else if (llmOutput.contains("苹果") || llmOutput.contains("华为") || llmOutput.contains("小米")) {
            return parseBrandQuestion(llmOutput);
        }
        
        // 默认返回错误查询
        return createErrorQuery("无法解析LLM输出，请重新提问");
    }
    
    private ParsedQuery parsePriceQuestion(String text) {
        // 提取价格范围
        Pattern pattern = Pattern.compile("(\\d+).*?(\\d+).*?元");
        Matcher matcher = pattern.matcher(text);
        
        if (matcher.find()) {
            double minPrice = Double.parseDouble(matcher.group(1));
            double maxPrice = Double.parseDouble(matcher.group(2));
            
            QueryIntent intent = QueryIntent.builder()
                .queryType("price_range")
                .conditions(Map.of(
                    "price", QueryIntent.Condition.builder()
                        .field("price")
                        .operator("BETWEEN")
                        .minValue(minPrice)
                        .maxValue(maxPrice)
                        .build()
                ))
                .build();
            
            String sql = "SELECT id, name, price, number FROM litemall_goods WHERE price BETWEEN ? AND ? AND is_on_sale = 1 ORDER BY price ASC";
            List<Object> parameters = Arrays.asList(minPrice, maxPrice);
            
            return ParsedQuery.builder()
                .queryIntent(intent)
                .sql(sql)
                .parameters(parameters)
                .build();
        }
        
        return createErrorQuery("无法解析价格范围");
    }
    
    private ParsedQuery parseProductCategoryQuestion(String text) {
        // 提取商品类别关键词
        List<String> categories = new ArrayList<>();
        if (text.contains("手机")) categories.add("手机");
        if (text.contains("电脑")) categories.add("电脑");
        if (text.contains("衣服")) categories.add("衣服");
        
        if (!categories.isEmpty()) {
            QueryIntent intent = QueryIntent.builder()
                .queryType("name_pattern")
                .conditions(Map.of(
                    "name", QueryIntent.Condition.builder()
                        .field("name")
                        .operator("IN")
                        .patterns(categories)
                        .matchMode("contains")
                        .caseSensitive(false)
                        .build()
                ))
                .build();
            
            // 构建IN查询
            String placeholders = String.join(",", Collections.nCopies(categories.size(), "?"));
            String sql = "SELECT id, name, price, number FROM litemall_goods WHERE name IN (" + placeholders + ") AND is_on_sale = 1";
            List<Object> parameters = new ArrayList<>(categories);
            
            return ParsedQuery.builder()
                .queryIntent(intent)
                .sql(sql)
                .parameters(parameters)
                .build();
        }
        
        return createErrorQuery("无法解析商品类别");
    }
    
    private ParsedQuery parseBrandQuestion(String text) {
        // 提取品牌关键词
        List<String> brands = new ArrayList<>();
        if (text.contains("苹果")) brands.add("苹果");
        if (text.contains("华为")) brands.add("华为");
        if (text.contains("小米")) brands.add("小米");
        
        if (!brands.isEmpty()) {
            QueryIntent intent = QueryIntent.builder()
                .queryType("name_pattern")
                .conditions(Map.of(
                    "name", QueryIntent.Condition.builder()
                        .field("name")
                        .operator("LIKE")
                        .pattern(brands.get(0)) // 取第一个品牌
                        .matchMode("contains")
                        .caseSensitive(false)
                        .build()
                ))
                .build();
            
            String sql = "SELECT id, name, price, number FROM litemall_goods WHERE name LIKE ? AND is_on_sale = 1";
            List<Object> parameters = Arrays.asList("%" + brands.get(0) + "%");
            
            return ParsedQuery.builder()
                .queryIntent(intent)
                .sql(sql)
                .parameters(parameters)
                .build();
        }
        
        return createErrorQuery("无法解析品牌信息");
    }
    
    private ParsedQuery createErrorQuery(String errorMessage) {
        return ParsedQuery.builder()
            .queryIntent(null)
            .sql("")
            .parameters(Collections.emptyList())
            .build();
    }
}
```