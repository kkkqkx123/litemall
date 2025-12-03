package org.linlinjava.litemall.core.llm.model;

import java.util.Map;

/**
 * 查询意图模型
 * 用于表示LLM解析后的查询意图
 */
public class QueryIntent {
    
    /**
     * 查询类型
     * - price_range: 价格范围查询
     * - stock_check: 库存查询
     * - category_filter: 分类筛选
     * - keyword_search: 关键词搜索
     * - name_pattern: 名称模式匹配
     * - specific_product: 特定商品查询
     * - statistical: 统计查询
     */
    private String queryType;
    
    /**
     * 查询条件
     * 键值对形式，键为字段名，值为条件值
     * 支持多种条件格式：
     * - 简单值：{"is_on_sale": 1}
     * - 范围值：{"price": {"min": 100, "max": 200}}
     * - 模式匹配：{"name": {"pattern": "手机", "mode": "contains", "case_sensitive": false}}
     */
    private Map<String, Object> conditions;
    
    /**
     * 排序方式
     * 格式："字段名 排序方向"
     * 例如："price ASC", "name DESC"
     */
    private String sort;
    
    /**
     * 返回数量限制
     * 如果为null，表示不限制
     */
    private Integer limit;
    
    /**
     * 置信度
     * 范围：0.0 - 1.0
     */
    private Double confidence;
    
    /**
     * 解释说明
     * 对查询意图的自然语言解释
     */
    private String explanation;
    
    /**
     * 是否有效的查询意图
     * @return true表示有效，false表示无效
     */
    public boolean isValid() {
        return queryType != null && !queryType.trim().isEmpty();
    }
    
    /**
     * 创建新的QueryIntent实例
     * @param queryType 查询类型
     * @return QueryIntent实例
     */
    public static QueryIntent of(String queryType) {
        QueryIntent intent = new QueryIntent();
        intent.setQueryType(queryType);
        return intent;
    }
    
    /**
     * 添加条件
     * @param field 字段名
     * @param value 值
     * @return this
     */
    public QueryIntent withCondition(String field, Object value) {
        if (this.conditions == null) {
            this.conditions = new java.util.HashMap<>();
        }
        this.conditions.put(field, value);
        return this;
    }
    
    /**
     * 设置排序
     * @param sort 排序字段
     * @return this
     */
    public QueryIntent withSort(String sort) {
        this.sort = sort;
        return this;
    }
    
    /**
     * 设置限制数量
     * @param limit 限制数量
     * @return this
     */
    public QueryIntent withLimit(Integer limit) {
        this.limit = limit;
        return this;
    }
    
    /**
     * 设置置信度
     * @param confidence 置信度
     * @return this
     */
    public QueryIntent withConfidence(Double confidence) {
        this.confidence = confidence;
        return this;
    }
    
    /**
     * 设置解释说明
     * @param explanation 解释说明
     * @return this
     */
    public QueryIntent withExplanation(String explanation) {
        this.explanation = explanation;
        return this;
    }
    
    /**
     * 获取指定条件的值
     * @param conditionName 条件名称
     * @return 条件值，如果不存在返回null
     */
    public Object getConditionValue(String conditionName) {
        return conditions != null ? conditions.get(conditionName) : null;
    }
    
    /**
     * 检查是否包含指定条件
     * @param conditionName 条件名称
     * @return true表示包含，false表示不包含
     */
    public boolean hasCondition(String conditionName) {
        return conditions != null && conditions.containsKey(conditionName);
    }
    
    // Getters and Setters
    public String getQueryType() {
        return queryType;
    }
    
    public void setQueryType(String queryType) {
        this.queryType = queryType;
    }
    
    public Map<String, Object> getConditions() {
        return conditions;
    }
    
    public void setConditions(Map<String, Object> conditions) {
        this.conditions = conditions;
    }
    
    public String getSort() {
        return sort;
    }
    
    public void setSort(String sort) {
        this.sort = sort;
    }
    
    public Integer getLimit() {
        return limit;
    }
    
    public void setLimit(Integer limit) {
        this.limit = limit;
    }
    
    public Double getConfidence() {
        return confidence;
    }
    
    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }
    
    public String getExplanation() {
        return explanation;
    }
    
    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }
    
    @Override
    public String toString() {
        return "QueryIntent{" +
                "queryType='" + queryType + '\'' +
                ", conditions=" + conditions +
                ", sort=" + sort +
                ", limit=" + limit +
                ", confidence=" + confidence +
                ", explanation='" + explanation + '\'' +
                '}';
    }
}