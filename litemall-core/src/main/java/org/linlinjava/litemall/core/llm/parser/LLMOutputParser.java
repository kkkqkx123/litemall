package org.linlinjava.litemall.core.llm.parser;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.linlinjava.litemall.core.llm.exception.JSONParseException;
import org.linlinjava.litemall.core.llm.exception.LLMOutputParseException;
import org.linlinjava.litemall.core.llm.model.QueryIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * LLM输出解析器
 * 解析LLM输出为QueryIntent对象
 */
@Component
public class LLMOutputParser {
    
    private static final Logger logger = LoggerFactory.getLogger(LLMOutputParser.class);
    
    @Autowired
    private JSONExtractor jsonExtractor;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 解析LLM输出为QueryIntent对象
     * 仅支持JSON格式：{"type":"product","conditions":{"name":"手机"}}
     * 
     * @param llmOutput LLM输出内容
     * @return QueryIntent对象，如果解析失败则返回null
     */
    public static QueryIntent parseLLMOutput(String llmOutput) {
        return parseLLMOutput(llmOutput, null);
    }
    
    /**
     * 解析LLM输出为QueryIntent对象（支持外部数量建议）
     * 仅支持JSON格式：{"type":"product","conditions":{"name":"手机"}}
     * 
     * @param llmOutput LLM输出内容
     * @param suggestedLimit 外部建议的查询数量，如果为null则由LLM决定
     * @return QueryIntent对象，如果解析失败则返回null
     */
    public static QueryIntent parseLLMOutput(String llmOutput, Integer suggestedLimit) {
        if (llmOutput == null || llmOutput.trim().isEmpty()) {
            logger.warn("LLM输出为空");
            return null;
        }
        
        try {
            String cleanedOutput = cleanOutput(llmOutput);
            
            // 仅尝试JSON解析
            if (isJSONFormat(cleanedOutput)) {
                return parseJSONOutput(cleanedOutput, suggestedLimit);
            }
            
            logger.warn("LLM输出不是有效的JSON格式，解析失败");
            return null;
            
        } catch (Exception e) {
            logger.error("解析LLM输出失败: {}", llmOutput, e);
            return null;
        }
    }
    
    /**
     * 清理LLM输出
     */
    private static String cleanOutput(String output) {
        if (output == null) {
            return "";
        }
        
        // 移除AI回复前缀
        String cleaned = output.replaceAll("^(AI|助手|Assistant)[:：]\\s*", "");
        
        // 移除Markdown代码块标记
        cleaned = cleaned.replaceAll("```[\\s\\S]*?```", "");
        cleaned = cleaned.replaceAll("`", "");
        
        return cleaned.trim();
    }
    
    /**
     * 解析JSON格式的LLM输出（支持外部数量建议）
     * 示例格式：{"type":"product","conditions":{"name":"手机"},"limit":20}
     */
    private static QueryIntent parseJSONOutput(String jsonOutput, Integer suggestedLimit) {
        try {
            // 创建ObjectMapper实例
            ObjectMapper mapper = new ObjectMapper();
            // 解析JSON为Map
            @SuppressWarnings("unchecked")
            Map<String, Object> jsonMap = mapper.readValue(jsonOutput, Map.class);

            // 创建QueryIntent对象
            QueryIntent queryIntent = new QueryIntent();

            // 设置查询类型
            queryIntent.setQueryType((String) jsonMap.getOrDefault("query_type", "keyword_search"));

            // 设置查询条件
            Object conditionsObj = jsonMap.get("conditions");
            if (conditionsObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> conditions = (Map<String, Object>) conditionsObj;
                queryIntent.setConditions(conditions);
            }

            // 设置排序（进行字段映射）
            String sort = (String) jsonMap.getOrDefault("sort", "retail_price ASC");
            queryIntent.setSort("retail_price ASC");

            // 设置查询数量（优先使用外部建议，否则使用LLM决策）
            if (suggestedLimit != null && suggestedLimit > 0) {
                queryIntent.setLimit(suggestedLimit);
            } else {
                Integer limit = (Integer) jsonMap.get("limit");
                if (limit != null && limit > 0) {
                    queryIntent.setLimit(limit);
                }
            }

            logger.debug("解析LLM输出为QueryIntent: {}", queryIntent);
            return queryIntent;

        } catch (Exception e) {
            logger.error("JSON解析失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 检查是否为JSON格式
     */
    private static boolean isJSONFormat(String output) {
        if (output == null || output.trim().isEmpty()) {
            return false;
        }
        
        String trimmed = output.trim();
        return (trimmed.startsWith("{") && trimmed.endsWith("}")) || 
               (trimmed.startsWith("[") && trimmed.endsWith("]"));
    }

    /**
     * 验证查询意图的基本结构
     */
    public boolean validateQueryIntent(QueryIntent queryIntent) {
        if (queryIntent == null) {
            logger.warn("查询意图为空");
            return false;
        }
        
        // 验证查询类型
        if (!isValidQueryType(queryIntent.getQueryType())) {
            logger.warn("无效的查询类型: {}", queryIntent.getQueryType());
            return false;
        }
        
        // 验证查询条件
        if (!validateConditions(queryIntent)) {
            logger.warn("查询条件验证失败");
            return false;
        }
        
        return true;
    }

    /**
     * 验证查询类型
     */
    private boolean isValidQueryType(String queryType) {
        if (queryType == null || queryType.trim().isEmpty()) {
            return false;
        }
        
        String[] validTypes = {
            "keyword_search", "category_filter", "brand_filter", 
            "price_range", "recommendation", "similar_products"
        };
        
        for (String validType : validTypes) {
            if (validType.equals(queryType)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * 验证查询条件
     */
    private boolean validateConditions(QueryIntent queryIntent) {
        Map<String, Object> conditions = queryIntent.getConditions();
        String queryType = queryIntent.getQueryType();
        
        if (conditions == null || conditions.isEmpty()) {
            logger.warn("查询条件为空");
            return false;
        }
        
        // 根据查询类型验证特定条件
        switch (queryType) {
            case "keyword_search":
                return conditions.containsKey("keyword") && 
                       conditions.get("keyword") != null &&
                       !conditions.get("keyword").toString().trim().isEmpty();
                       
            case "category_filter":
                return conditions.containsKey("category") &&
                       conditions.get("category") != null;
                       
            case "brand_filter":
                return conditions.containsKey("brand") &&
                       conditions.get("brand") != null;
                       
            case "price_range":
                return conditions.containsKey("minPrice") &&
                       conditions.containsKey("maxPrice") &&
                       conditions.get("minPrice") != null &&
                       conditions.get("maxPrice") != null;
                       
            default:
                return true; // 其他类型不强制验证
        }
    }

    /**
     * 验证名称匹配模式
     */
    public boolean isValidNamePatternMode(String mode) {
        if (mode == null || mode.trim().isEmpty()) {
            return false;
        }
        
        String[] validModes = {"exact", "like", "prefix", "suffix", "fuzzy"};
        for (String validMode : validModes) {
            if (validMode.equals(mode)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * 将LLM生成的排序字段映射为数据库实际字段名
     * @param llmSortField LLM生成的排序字段
     * @return 数据库实际字段名
     */
    public String mapSortField(String llmSortField) {
        if (llmSortField == null || llmSortField.trim().isEmpty()) {
            return "retail_price ASC";
        }
        
        // 移除多余空格并转换为大写
        llmSortField = llmSortField.trim().toUpperCase();
        
        // 分离字段名和排序方向
        String[] parts = llmSortField.split("\\s+");
        String field = parts[0];
        String direction = parts.length > 1 ? parts[1] : "ASC";
        
        // 字段映射
        String mappedField;
        switch (field) {
            case "PRICE":
            case "价格":
                mappedField = "retail_price";
                break;
            case "NAME":
            case "名称":
                mappedField = "name";
                break;
            case "SALES":
            case "销量":
                mappedField = "number_sold";
                break;
            case "ADD_TIME":
            case "添加时间":
                mappedField = "add_time";
                break;
            case "UPDATE_TIME":
            case "更新时间":
                mappedField = "update_time";
                break;
            default:
                mappedField = "retail_price"; // 默认按价格排序
        }
        
        // 确保排序方向有效
        if (!"DESC".equals(direction)) {
            direction = "ASC";
        }
        
        return mappedField + " " + direction;
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
     * 从LLM输出中提取JSON内容
     * @param output LLM输出字符串
     * @return 提取的JSON内容
     */
    public String extractJSONContent(String output) {
        if (output == null || output.trim().isEmpty()) {
            return "";
        }
        
        logger.debug("开始从输出中提取JSON：{}", output);
        
        try {
            // 使用jsonExtractor提取JSON
            String extractedContent = jsonExtractor.extractJSON(output);
            logger.debug("提取结果：{}", extractedContent);
            return extractedContent;
            
        } catch (Exception e) {
            logger.warn("从输出中提取JSON失败，原样返回输出：{}", e.getMessage());
            return output.trim();
        }
    }

    /**
     * 提取JSON内容（重载方法，支持默认实现）
     * @param output LLM输出字符串
     * @param defaultValue 默认值
     * @return 提取的JSON内容，如果提取失败返回默认值
     */
    public String extractJSONContent(String output, String defaultValue) {
        try {
            String content = extractJSONContent(output);
            if (content == null || content.trim().isEmpty()) {
                return defaultValue;
            }
            return content;
        } catch (Exception e) {
            logger.warn("提取JSON失败，使用默认值：{}", e.getMessage());
            return defaultValue;
        }
    }

    /**
     * 从输出中提取第一个有效的JSON对象
     * @param output LLM输出字符串
     * @return JSON字符串，如果没有找到返回null
     */
    public String extractFirstJSONObject(String output) {
        if (output == null) {
            return null;
        }
        
        // 查找第一个{和对应的}
        int startIndex = output.indexOf('{');
        if (startIndex == -1) {
            return null;
        }
        
        int braceCount = 0;
        int endIndex = -1;
        
        for (int i = startIndex; i < output.length(); i++) {
            if (output.charAt(i) == '{') {
                braceCount++;
            } else if (output.charAt(i) == '}') {
                braceCount--;
                if (braceCount == 0) {
                    endIndex = i;
                    break;
                }
            }
        }
        
        if (endIndex != -1) {
            return output.substring(startIndex, endIndex + 1);
        }
        
        return null;
    }
}