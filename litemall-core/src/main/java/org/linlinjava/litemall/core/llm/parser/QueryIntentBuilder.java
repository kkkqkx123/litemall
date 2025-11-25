package org.linlinjava.litemall.core.llm.parser;

import org.linlinjava.litemall.core.llm.model.QueryIntent;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 查询意图构建器
 * 用于构建和验证查询意图
 */
@Component
public class QueryIntentBuilder {
    
    /**
     * 查询类型枚举
     */
    public enum QueryType {
        PRICE_RANGE("price_range"),
        STOCK_CHECK("stock_check"),
        CATEGORY_FILTER("category_filter"),
        KEYWORD_SEARCH("keyword_search"),
        NAME_PATTERN("name_pattern"),
        SPECIFIC_PRODUCT("specific_product"),
        STATISTICAL("statistical");
        
        private final String value;
        
        QueryType(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
        
        public static boolean isValid(String queryType) {
            for (QueryType type : values()) {
                if (type.value.equals(queryType)) {
                    return true;
                }
            }
            return false;
        }
    }
    
    /**
     * 名称匹配模式枚举
     */
    public enum NamePatternMode {
        EXACT("exact"),
        CONTAINS("contains"),
        STARTS_WITH("starts_with"),
        ENDS_WITH("ends_with"),
        REGEX("regex");
        
        private final String value;
        
        NamePatternMode(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
        
        public static boolean isValid(String mode) {
            for (NamePatternMode patternMode : values()) {
                if (patternMode.value.equals(mode)) {
                    return true;
                }
            }
            return false;
        }
    }
    
    /**
     * 创建价格范围查询意图
     * @param minPrice 最低价格
     * @param maxPrice 最高价格
     * @return 查询意图对象
     */
    public QueryIntent buildPriceRangeQuery(Double minPrice, Double maxPrice) {
        QueryIntent intent = new QueryIntent();
        intent.setQueryType(QueryType.PRICE_RANGE.getValue());
        
        Map<String, Object> conditions = new HashMap<>();
        if (minPrice != null) {
            conditions.put("min_price", minPrice);
        }
        if (maxPrice != null) {
            conditions.put("max_price", maxPrice);
        }
        conditions.put("is_on_sale", 1); // 只查询在售商品
        
        intent.setConditions(conditions);
        intent.setSort("price ASC");
        return intent;
    }
    
    /**
     * 创建库存查询意图
     * @param minStock 最低库存
     * @param maxStock 最高库存
     * @return 查询意图对象
     */
    public QueryIntent buildStockCheckQuery(Integer minStock, Integer maxStock) {
        QueryIntent intent = new QueryIntent();
        intent.setQueryType(QueryType.STOCK_CHECK.getValue());
        
        Map<String, Object> conditions = new HashMap<>();
        if (minStock != null) {
            conditions.put("min_number", minStock);
        }
        if (maxStock != null) {
            conditions.put("max_number", maxStock);
        }
        conditions.put("is_on_sale", 1); // 只查询在售商品
        
        intent.setConditions(conditions);
        return intent;
    }
    
    /**
     * 创建名称模式匹配查询意图
     * @param pattern 匹配模式
     * @param mode 匹配模式
     * @param caseSensitive 是否大小写敏感
     * @return 查询意图对象
     */
    public QueryIntent buildNamePatternQuery(String pattern, String mode, boolean caseSensitive) {
        QueryIntent intent = new QueryIntent();
        intent.setQueryType(QueryType.NAME_PATTERN.getValue());
        
        Map<String, Object> nameCondition = new HashMap<>();
        nameCondition.put("pattern", pattern);
        nameCondition.put("mode", mode);
        nameCondition.put("case_sensitive", caseSensitive);
        
        Map<String, Object> conditions = new HashMap<>();
        conditions.put("name", nameCondition);
        conditions.put("is_on_sale", 1); // 只查询在售商品
        
        intent.setConditions(conditions);
        return intent;
    }
    
    /**
     * 创建关键词搜索查询意图
     * @param keyword 关键词
     * @return 查询意图对象
     */
    public QueryIntent buildKeywordSearchQuery(String keyword) {
        QueryIntent intent = new QueryIntent();
        intent.setQueryType(QueryType.KEYWORD_SEARCH.getValue());
        
        Map<String, Object> conditions = new HashMap<>();
        conditions.put("keyword", keyword);
        conditions.put("is_on_sale", 1); // 只查询在售商品
        
        intent.setConditions(conditions);
        return intent;
    }
    
    /**
     * 创建分类筛选查询意图
     * @param categoryId 分类ID
     * @return 查询意图对象
     */
    public QueryIntent buildCategoryFilterQuery(Integer categoryId) {
        QueryIntent intent = new QueryIntent();
        intent.setQueryType(QueryType.CATEGORY_FILTER.getValue());
        
        Map<String, Object> conditions = new HashMap<>();
        if (categoryId != null) {
            conditions.put("category_id", categoryId);
        }
        conditions.put("is_on_sale", 1); // 只查询在售商品
        
        intent.setConditions(conditions);
        return intent;
    }
    
    /**
     * 创建特定商品查询意图
     * @param goodsId 商品ID
     * @return 查询意图对象
     */
    public QueryIntent buildSpecificProductQuery(Integer goodsId) {
        QueryIntent intent = new QueryIntent();
        intent.setQueryType(QueryType.SPECIFIC_PRODUCT.getValue());
        
        Map<String, Object> conditions = new HashMap<>();
        if (goodsId != null) {
            conditions.put("id", goodsId);
        }
        conditions.put("is_on_sale", 1); // 只查询在售商品
        
        intent.setConditions(conditions);
        return intent;
    }
    
    /**
     * 创建统计查询意图
     * @param statisticType 统计类型
     * @return 查询意图对象
     */
    public QueryIntent buildStatisticalQuery(String statisticType) {
        QueryIntent intent = new QueryIntent();
        intent.setQueryType(QueryType.STATISTICAL.getValue());
        
        Map<String, Object> conditions = new HashMap<>();
        conditions.put("statistic_type", statisticType);
        conditions.put("is_on_sale", 1); // 只统计在售商品
        
        intent.setConditions(conditions);
        return intent;
    }
    
    /**
     * 验证查询意图
     * @param intent 查询意图
     * @return true表示有效，false表示无效
     */
    public boolean validateQueryIntent(QueryIntent intent) {
        if (intent == null || !intent.isValid()) {
            return false;
        }
        
        // 验证查询类型
        if (!QueryType.isValid(intent.getQueryType())) {
            return false;
        }
        
        // 验证查询条件
        Map<String, Object> conditions = intent.getConditions();
        if (conditions == null || conditions.isEmpty()) {
            return false;
        }
        
        // 验证名称模式匹配的条件
        if (QueryType.NAME_PATTERN.getValue().equals(intent.getQueryType())) {
            Object nameCondition = conditions.get("name");
            if (nameCondition instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nameMap = (Map<String, Object>) nameCondition;
                String mode = (String) nameMap.get("mode");
                if (!NamePatternMode.isValid(mode)) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    /**
     * 获取查询类型描述
     * @param queryType 查询类型
     * @return 查询类型描述
     */
    public String getQueryTypeDescription(String queryType) {
        for (QueryType type : QueryType.values()) {
            if (type.value.equals(queryType)) {
                switch (type) {
                    case PRICE_RANGE:
                        return "价格范围查询";
                    case STOCK_CHECK:
                        return "库存查询";
                    case CATEGORY_FILTER:
                        return "分类筛选";
                    case KEYWORD_SEARCH:
                        return "关键词搜索";
                    case NAME_PATTERN:
                        return "名称模式匹配";
                    case SPECIFIC_PRODUCT:
                        return "特定商品查询";
                    case STATISTICAL:
                        return "统计查询";
                    default:
                        return "未知查询类型";
                }
            }
        }
        return "未知查询类型";
    }
}