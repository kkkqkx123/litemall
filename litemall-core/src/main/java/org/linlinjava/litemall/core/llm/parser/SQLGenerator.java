package org.linlinjava.litemall.core.llm.parser;

import java.util.Map;

import org.linlinjava.litemall.core.llm.exception.SQLGenerationException;
import org.linlinjava.litemall.core.llm.model.QueryIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * SQL生成器
 * 根据QueryIntent生成对应的SQL查询语句
 */
@Component
public class SQLGenerator {
    
    private static final Logger logger = LoggerFactory.getLogger(SQLGenerator.class);
    
    @Autowired
    private SQLBuilder sqlBuilder;
    
    /**
     * 根据查询意图生成SQL语句
     * @param queryIntent 查询意图对象
     * @return 生成的SQL语句
     * @throws SQLGenerationException 当SQL生成失败时抛出异常
     */
    public String generateSQL(QueryIntent queryIntent) throws SQLGenerationException {
        try {
            if (queryIntent == null) {
                throw new SQLGenerationException("查询意图对象为空");
            }
            
            String queryType = queryIntent.getQueryType();
            if (queryType == null || queryType.trim().isEmpty()) {
                throw new SQLGenerationException("查询类型为空", null, "query_type");
            }
            
            logger.debug("开始生成SQL查询 - queryType: {}", queryType);
            
            // 根据查询类型生成相应的SQL
            String sql;
            switch (queryType.toLowerCase()) {
                case "product_search":
                case "price_range":
                case "stock_check":
                case "category_filter":
                    sql = sqlBuilder.buildQuerySQL(queryIntent);
                    break;
                    
                case "statistical_query":
                case "total_count":
                case "price_stats":
                case "stock_stats":
                case "category_stats":
                    sql = sqlBuilder.buildStatisticalSQL(queryIntent);
                    break;
                    
                default:
                    throw new SQLGenerationException("不支持的查询类型: " + queryType, queryType, null);
            }
            
            if (sql == null || sql.trim().isEmpty()) {
                throw new SQLGenerationException("生成的SQL语句为空", queryType, null);
            }
            
            logger.debug("SQL生成成功 - queryType: {}, sql: {}", queryType, sql);
            return sql;
            
        } catch (SQLGenerationException e) {
            // 重新抛出已知的SQL生成异常
            throw e;
        } catch (Exception e) {
            String errorMsg = "生成SQL时发生未知错误";
            logger.error("{}: {}", errorMsg, e.getMessage(), e);
            throw new SQLGenerationException(errorMsg, e);
        }
    }
    
    /**
     * 验证查询意图是否有效
     * @param queryIntent 查询意图对象
     * @return true如果有效，false如果无效
     */
    public boolean validateQueryIntent(QueryIntent queryIntent) {
        try {
            if (queryIntent == null) {
                logger.warn("查询意图验证失败：对象为null");
                return false;
            }
            
            String queryType = queryIntent.getQueryType();
            if (queryType == null || queryType.trim().isEmpty()) {
                logger.warn("查询意图验证失败：查询类型为空");
                return false;
            }
            
            // 验证查询类型是否支持
            switch (queryType.toLowerCase()) {
                case "product_search":
                case "price_range":
                case "stock_check":
                case "category_filter":
                case "statistical_query":
                case "total_count":
                case "price_stats":
                case "stock_stats":
                case "category_stats":
                    break;
                default:
                    logger.warn("查询意图验证失败：不支持的查询类型 - {}", queryType);
                    return false;
            }
            
            // 验证条件字段
            Map<String, Object> conditions = queryIntent.getConditions();
            if (conditions != null) {
                for (Map.Entry<String, Object> entry : conditions.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    
                    if (key == null || key.trim().isEmpty()) {
                        logger.warn("查询意图验证失败：条件字段名为空");
                        return false;
                    }
                    
                    if (value == null) {
                        logger.warn("查询意图验证失败：条件字段值为空 - key: {}", key);
                        return false;
                    }
                }
            }
            
            // 验证限制数量
            Integer limit = queryIntent.getLimit();
            if (limit != null && limit <= 0) {
                logger.warn("查询意图验证失败：限制数量必须为正数 - limit: {}", limit);
                return false;
            }
            
            // 验证置信度
            Double confidence = queryIntent.getConfidence();
            if (confidence != null && (confidence < 0.0 || confidence > 1.0)) {
                logger.warn("查询意图验证失败：置信度必须在0-1之间 - confidence: {}", confidence);
                return false;
            }
            
            logger.debug("查询意图验证成功 - queryType: {}", queryType);
            return true;
            
        } catch (Exception e) {
            logger.error("查询意图验证失败：发生异常", e);
            return false;
        }
    }
}