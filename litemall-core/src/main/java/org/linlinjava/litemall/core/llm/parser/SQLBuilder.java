package org.linlinjava.litemall.core.llm.parser;

import org.linlinjava.litemall.core.llm.model.QueryIntent;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * SQL构建器
 * 用于根据查询意图构建SQL查询语句
 */
@Component
public class SQLBuilder {
    
    /**
     * 商品表名
     */
    private static final String GOODS_TABLE = "litemall_goods";
    
    /**
     * 根据查询意图构建SQL查询语句
     * @param queryIntent 查询意图
     * @return SQL查询语句
     */
    public String buildQuerySQL(QueryIntent queryIntent) {
        if (queryIntent == null || !queryIntent.isValid()) {
            throw new IllegalArgumentException("无效的查询意图");
        }
        
        StringBuilder sql = new StringBuilder();
        List<Object> parameters = new ArrayList<>();
        
        // 构建基础查询
        sql.append("SELECT * FROM ").append(GOODS_TABLE);
        
        // 构建WHERE条件
        String whereClause = buildWhereClause(queryIntent, parameters);
        if (whereClause != null && !whereClause.trim().isEmpty()) {
            sql.append(" WHERE ").append(whereClause);
        }
        
        // 构建排序
        if (queryIntent.getSort() != null && !queryIntent.getSort().trim().isEmpty()) {
            String sortField = mapSortField(queryIntent.getSort());
            sql.append(" ORDER BY ").append(sortField);
        }
        
        // 构建限制
        if (queryIntent.getLimit() != null && queryIntent.getLimit() > 0) {
            sql.append(" LIMIT ").append(queryIntent.getLimit());
        }
        
        return sql.toString();
    }
    
    /**
     * 构建WHERE条件子句
     * @param queryIntent 查询意图
     * @param parameters 参数列表（用于预编译SQL）
     * @return WHERE条件子句
     */
    private String buildWhereClause(QueryIntent queryIntent, List<Object> parameters) {
        StringBuilder whereClause = new StringBuilder();
        Map<String, Object> conditions = queryIntent.getConditions();
        
        if (conditions == null || conditions.isEmpty()) {
            return "";
        }
        
        boolean firstCondition = true;
        
        // 处理通用条件
        for (Map.Entry<String, Object> entry : conditions.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (value == null) {
                continue;
            }
            
            // 跳过特殊处理的条件
            if ("name".equals(key)) {
                continue;
            }
            
            if (!firstCondition) {
                whereClause.append(" AND ");
            }
            firstCondition = false;
            
            // 根据查询类型处理条件
            switch (key) {
                case "min_price":
                    whereClause.append("retail_price >= ?");
                    parameters.add(value);
                    break;
                case "max_price":
                    whereClause.append("retail_price <= ?");
                    parameters.add(value);
                    break;
                case "min_number":
                    whereClause.append("number >= ?");
                    parameters.add(value);
                    break;
                case "max_number":
                    whereClause.append("number <= ?");
                    parameters.add(value);
                    break;
                case "is_on_sale":
                    whereClause.append("is_on_sale = ?");
                    parameters.add(value);
                    break;
                case "keyword":
                    whereClause.append("(name LIKE ? OR keywords LIKE ? OR brief LIKE ?)");
                    String keyword = "%" + value + "%";
                    parameters.add(keyword);
                    parameters.add(keyword);
                    parameters.add(keyword);
                    break;
                case "statistic_type":
                    // 统计查询不需要WHERE条件
                    break;
                default:
                    // 其他字段使用等值匹配
                    whereClause.append(key).append(" = ?");
                    parameters.add(value);
                    break;
            }
        }
        
        // 处理名称模式匹配
        Object nameCondition = conditions.get("name");
        if (nameCondition instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> nameMap = (Map<String, Object>) nameCondition;
            
            String nameClause = buildNameCondition(nameMap, parameters);
            if (nameClause != null && !nameClause.trim().isEmpty()) {
                if (!firstCondition) {
                    whereClause.append(" AND ");
                }
                whereClause.append(nameClause);
            }
        }
        
        return whereClause.toString();
    }
    
    /**
     * 构建名称条件子句
     * @param nameCondition 名称条件
     * @param parameters 参数列表
     * @return 名称条件子句
     */
    private String buildNameCondition(Map<String, Object> nameCondition, List<Object> parameters) {
        if (nameCondition == null || nameCondition.isEmpty()) {
            return "";
        }
        
        String pattern = (String) nameCondition.get("pattern");
        String mode = (String) nameCondition.get("mode");
        Boolean caseSensitive = (Boolean) nameCondition.get("case_sensitive");
        
        if (pattern == null || pattern.trim().isEmpty()) {
            return "";
        }
        
        StringBuilder nameClause = new StringBuilder();
        
        // 默认不区分大小写
        if (caseSensitive == null || !caseSensitive) {
            nameClause.append("LOWER(name)");
        } else {
            nameClause.append("name");
        }
        
        // 根据模式构建条件
        if (mode == null || mode.trim().isEmpty()) {
            mode = "contains"; // 默认模式
        }
        
        switch (mode) {
            case "exact":
                nameClause.append(" = ?");
                parameters.add(caseSensitive != null && caseSensitive ? pattern : pattern.toLowerCase());
                break;
            case "contains":
                nameClause.append(" LIKE ?");
                parameters.add(caseSensitive != null && caseSensitive ? "%" + pattern + "%" : "%" + pattern.toLowerCase() + "%");
                break;
            case "starts_with":
                nameClause.append(" LIKE ?");
                parameters.add(caseSensitive != null && caseSensitive ? pattern + "%" : pattern.toLowerCase() + "%");
                break;
            case "ends_with":
                nameClause.append(" LIKE ?");
                parameters.add(caseSensitive != null && caseSensitive ? "%" + pattern : "%" + pattern.toLowerCase());
                break;
            case "regex":
                // MySQL正则表达式匹配
                nameClause.append(" REGEXP ?");
                parameters.add(pattern);
                break;
            default:
                nameClause.append(" LIKE ?");
                parameters.add(caseSensitive != null && caseSensitive ? "%" + pattern + "%" : "%" + pattern.toLowerCase() + "%");
                break;
        }
        
        return nameClause.toString();
    }
    
    /**
     * 映射排序字段到数据库实际字段名
     * @param sortField 原始排序字段
     * @return 映射后的数据库字段名
     */
    private String mapSortField(String sortField) {
        if (sortField == null || sortField.trim().isEmpty()) {
            return sortField;
        }
        
        // 处理字段映射
        String mappedField = sortField.trim();
        
        // 将price映射为retail_price
        if ("price".equalsIgnoreCase(mappedField)) {
            mappedField = "retail_price";
        }
        
        // 处理带排序方向的字段（如"price desc"）
        if (mappedField.toLowerCase().startsWith("price ")) {
            String[] parts = mappedField.split("\\s+", 2);
            if (parts.length == 2) {
                mappedField = "retail_price " + parts[1];
            }
        }
        
        // 处理带排序方向的字段（如"price asc"）
        if (mappedField.toLowerCase().contains("price")) {
            String[] parts = mappedField.split("\\s+", 2);
            if (parts.length == 2) {
                if ("price".equalsIgnoreCase(parts[0])) {
                    mappedField = "retail_price " + parts[1];
                }
            }
        }
        
        return mappedField;
    }
    
    /**
     * 构建统计查询SQL和参数
     * @param queryIntent 查询意图
     * @return SQL和参数数组，[0]是SQL字符串，[1]是参数列表
     */
    public Object[] buildStatisticalSQLWithParams(QueryIntent queryIntent) {
        if (queryIntent == null || !queryIntent.isValid()) {
            throw new IllegalArgumentException("无效的查询意图");
        }
        
        Map<String, Object> conditions = queryIntent.getConditions();
        if (conditions == null || conditions.isEmpty()) {
            throw new IllegalArgumentException("统计查询需要条件");
        }
        
        String statisticType = (String) conditions.get("statistic_type");
        if (statisticType == null || statisticType.trim().isEmpty()) {
            throw new IllegalArgumentException("统计查询需要指定统计类型");
        }
        
        StringBuilder sql = new StringBuilder();
        List<Object> parameters = new ArrayList<>();
        
        switch (statisticType) {
            case "total_count":
                sql.append("SELECT COUNT(*) as total FROM ").append(GOODS_TABLE);
                break;
            case "price_stats":
                sql.append("SELECT COUNT(*) as count, MIN(retail_price) as min_price, MAX(retail_price) as max_price, AVG(retail_price) as avg_price FROM ").append(GOODS_TABLE);
                break;
            case "stock_stats":
                sql.append("SELECT COUNT(*) as count, SUM(number) as total_stock, AVG(number) as avg_stock FROM ").append(GOODS_TABLE);
                break;
            case "category_stats":
                sql.append("SELECT category_id, COUNT(*) as count FROM ").append(GOODS_TABLE).append(" GROUP BY category_id");
                break;
            default:
                throw new IllegalArgumentException("不支持的统计类型: " + statisticType);
        }
        
        // 添加基础条件（如is_on_sale）
        String whereClause = buildStatisticalWhereClause(conditions, parameters);
        if (whereClause != null && !whereClause.trim().isEmpty()) {
            sql.append(" WHERE ").append(whereClause);
        }
        
        return new Object[]{sql.toString(), parameters};
    }
    
    /**
     * 构建统计查询SQL（保持向后兼容）
     * @param queryIntent 查询意图
     * @return 统计查询SQL
     */
    public String buildStatisticalSQL(QueryIntent queryIntent) {
        if (queryIntent == null || !queryIntent.isValid()) {
            throw new IllegalArgumentException("无效的查询意图");
        }
        
        Map<String, Object> conditions = queryIntent.getConditions();
        if (conditions == null || conditions.isEmpty()) {
            throw new IllegalArgumentException("统计查询需要条件");
        }
        
        String statisticType = (String) conditions.get("statistic_type");
        if (statisticType == null || statisticType.trim().isEmpty()) {
            throw new IllegalArgumentException("统计查询需要指定统计类型");
        }
        
        StringBuilder sql = new StringBuilder();
        
        switch (statisticType) {
            case "total_count":
                sql.append("SELECT COUNT(*) as total FROM ").append(GOODS_TABLE);
                break;
            case "price_stats":
                sql.append("SELECT COUNT(*) as count, MIN(retail_price) as min_price, MAX(retail_price) as max_price, AVG(retail_price) as avg_price FROM ").append(GOODS_TABLE);
                break;
            case "stock_stats":
                sql.append("SELECT COUNT(*) as count, SUM(number) as total_stock, AVG(number) as avg_stock FROM ").append(GOODS_TABLE);
                break;
            case "category_stats":
                sql.append("SELECT category_id, COUNT(*) as count FROM ").append(GOODS_TABLE).append(" GROUP BY category_id");
                break;
            default:
                throw new IllegalArgumentException("不支持的统计类型: " + statisticType);
        }
        
        // 添加基础条件（如is_on_sale）
        List<Object> statisticalParams = new ArrayList<>();
        String whereClause = buildStatisticalWhereClause(conditions, statisticalParams);
        if (whereClause != null && !whereClause.trim().isEmpty()) {
            sql.append(" WHERE ").append(whereClause);
        }
        
        // 注意：统计查询的参数需要通过其他方式传递
        // 这里只返回SQL字符串，参数需要调用方单独处理
        return sql.toString();
    }
    
    /**
     * 构建统计查询的WHERE条件
     * @param conditions 查询条件
     * @param parameters 参数列表（用于预编译SQL）
     * @return WHERE条件子句
     */
    private String buildStatisticalWhereClause(Map<String, Object> conditions, List<Object> parameters) {
        StringBuilder whereClause = new StringBuilder();
        boolean firstCondition = true;
        
        for (Map.Entry<String, Object> entry : conditions.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            // 跳过统计类型条件
            if ("statistic_type".equals(key) || "name".equals(key)) {
                continue;
            }
            
            if (value == null) {
                continue;
            }
            
            if (!firstCondition) {
                whereClause.append(" AND ");
            }
            firstCondition = false;
            
            // 处理通用条件，使用预编译参数
            switch (key) {
                case "min_price":
                    whereClause.append("retail_price >= ?");
                    parameters.add(value);
                    break;
                case "max_price":
                    whereClause.append("retail_price <= ?");
                    parameters.add(value);
                    break;
                case "min_number":
                    whereClause.append("number >= ?");
                    parameters.add(value);
                    break;
                case "max_number":
                    whereClause.append("number <= ?");
                    parameters.add(value);
                    break;
                case "is_on_sale":
                    whereClause.append("is_on_sale = ?");
                    parameters.add(value);
                    break;
                default:
                    whereClause.append(key).append(" = ?");
                    parameters.add(value);
                    break;
            }
        }
        
        return whereClause.toString();
    }
}