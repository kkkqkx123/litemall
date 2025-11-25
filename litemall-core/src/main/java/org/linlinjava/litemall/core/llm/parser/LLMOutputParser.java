package org.linlinjava.litemall.core.llm.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.linlinjava.litemall.core.llm.exception.LLMOutputParseException;
import org.linlinjava.litemall.core.llm.model.QueryIntent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * LLM输出解析器
 * 解析LLM输出为QueryIntent对象
 */
@Component
public class LLMOutputParser {
    
    @Autowired
    private JSONExtractor jsonExtractor;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 解析LLM输出为查询意图
     * @param llmOutput LLM输出文本
     * @return 查询意图对象
     * @throws LLMOutputParseException 当解析失败时抛出此异常
     */
    public QueryIntent parseQueryIntent(String llmOutput) throws LLMOutputParseException {
        if (llmOutput == null || llmOutput.trim().isEmpty()) {
            throw new LLMOutputParseException("LLM输出为空");
        }
        
        try {
            // 提取JSON部分
            String jsonStr = jsonExtractor.extractJSON(llmOutput);
            
            // 转换为QueryIntent对象
            QueryIntent queryIntent = objectMapper.readValue(jsonStr, QueryIntent.class);
            
            // 增强的验证逻辑
            validateQueryIntent(queryIntent);
            
            return queryIntent;
            
        } catch (LLMOutputParseException e) {
            throw e; // 重新抛出已知的解析异常
        } catch (Exception e) {
            throw new LLMOutputParseException("解析LLM输出失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 解析LLM输出为字符串（用于回答生成）
     * @param llmOutput LLM输出文本
     * @return 解析后的字符串
     */
    public String parseString(String llmOutput) {
        if (llmOutput == null) {
            return "";
        }
        
        // 移除Markdown代码块标记
        return llmOutput.replaceAll("```[\\s\\S]*?```", "")
                       .replaceAll("`", "")
                       .trim();
    }
    
    /**
     * 验证查询意图是否有效（增强版）
     * @param queryIntent 查询意图
     * @throws LLMOutputParseException 当验证失败时抛出此异常
     */
    private void validateQueryIntent(QueryIntent queryIntent) throws LLMOutputParseException {
        if (queryIntent == null) {
            throw new LLMOutputParseException("查询意图为空");
        }
        
        // 基本结构验证
        if (!queryIntent.isValid()) {
            throw new LLMOutputParseException("查询意图基本结构无效: " + queryIntent);
        }
        
        // 验证查询类型
        String queryType = queryIntent.getQueryType();
        if (!isValidQueryType(queryType)) {
            throw new LLMOutputParseException("无效的查询类型: " + queryType);
        }
        
        // 验证查询条件
        Map<String, Object> conditions = queryIntent.getConditions();
        if (conditions != null) {
            validateConditions(conditions, queryType);
        }
    }
    
    /**
     * 验证查询类型是否有效
     * @param queryType 查询类型
     * @return true表示有效，false表示无效
     */
    private boolean isValidQueryType(String queryType) {
        if (queryType == null) {
            return false;
        }
        
        // 验证是否为支持的查询类型
        return "price_range".equals(queryType) ||
               "stock_check".equals(queryType) ||
               "category_filter".equals(queryType) ||
               "keyword_search".equals(queryType) ||
               "name_pattern".equals(queryType) ||
               "specific_product".equals(queryType) ||
               "statistical".equals(queryType);
    }
    
    /**
     * 验证查询条件
     * @param conditions 查询条件
     * @param queryType 查询类型
     * @throws LLMOutputParseException 验证失败时抛出此异常
     */
    private void validateConditions(Map<String, Object> conditions, String queryType) throws LLMOutputParseException {
        // 验证名称模式匹配的条件
        if ("name_pattern".equals(queryType)) {
            Object nameCondition = conditions.get("name");
            if (!(nameCondition instanceof Map)) {
                throw new LLMOutputParseException("名称模式匹配查询需要name条件为Map类型");
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> nameMap = (Map<String, Object>) nameCondition;
            String pattern = (String) nameMap.get("pattern");
            String mode = (String) nameMap.getOrDefault("mode", "contains");
            
            if (pattern == null || pattern.trim().isEmpty()) {
                throw new LLMOutputParseException("名称模式匹配的pattern不能为空");
            }
            
            if (!isValidNamePatternMode(mode)) {
                throw new LLMOutputParseException("无效的名称匹配模式: " + mode);
            }
        }
        
        // 验证价格范围查询的条件
        if ("price_range".equals(queryType)) {
            Object minPrice = conditions.get("min_price");
            Object maxPrice = conditions.get("max_price");
            
            if (minPrice == null && maxPrice == null) {
                throw new LLMOutputParseException("价格范围查询至少需要指定min_price或max_price之一");
            }
            
            if (minPrice != null && !(minPrice instanceof Number)) {
                throw new LLMOutputParseException("min_price必须是数字类型");
            }
            
            if (maxPrice != null && !(maxPrice instanceof Number)) {
                throw new LLMOutputParseException("max_price必须是数字类型");
            }
        }
        
        // 验证关键词搜索的条件
        if ("keyword_search".equals(queryType)) {
            String keyword = (String) conditions.get("keyword");
            if (keyword == null || keyword.trim().isEmpty()) {
                throw new LLMOutputParseException("关键词搜索的keyword不能为空");
            }
        }
    }
    
    /**
     * 验证名称匹配模式是否有效
     * @param mode 匹配模式
     * @return true表示有效，false表示无效
     */
    private boolean isValidNamePatternMode(String mode) {
        if (mode == null) {
            return false;
        }
        
        return "exact".equals(mode) ||
               "contains".equals(mode) ||
               "starts_with".equals(mode) ||
               "ends_with".equals(mode) ||
               "regex".equals(mode);
    }
}