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
     * @param llmOutput LLM输出字符串
     * @return QueryIntent对象
     * @throws JSONParseException 当解析失败时抛出异常
     */
    public QueryIntent parseQueryIntent(String llmOutput) throws JSONParseException {
        if (llmOutput == null || llmOutput.trim().isEmpty()) {
            throw new JSONParseException("LLM输出为空");
        }
        
        // 提取JSON内容
        String jsonContent = jsonExtractor.extractJSON(llmOutput);
        if (jsonContent == null) {
            throw new JSONParseException("无法提取有效的JSON格式");
        }
        
        try {
            // 解析JSON为Map
            @SuppressWarnings("unchecked")
            Map<String, Object> jsonMap = objectMapper.readValue(jsonContent, Map.class);
            
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
            queryIntent.setSort(mapSortField(sort));
            
            // 设置限制数量
            queryIntent.setLimit((Integer) jsonMap.getOrDefault("limit", 10));
            
            logger.debug("解析LLM输出为QueryIntent: {}", queryIntent);
            return queryIntent;
            
        } catch (Exception e) {
            throw new JSONParseException("JSON解析失败: " + e.getMessage());
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

    /**
     * 清理输出文本，移除常见的标记和前缀
     * @param output 原始输出
     * @return 清理后的文本
     */
    public String cleanOutput(String output) {
        if (output == null) {
            return "";
        }
        
        String cleaned = output;
        
        // 移除常见的AI回复前缀
        String[] prefixes = {
            "根据以上信息，",
            "根据您的查询，",
            "以下是查询结果：",
            "查询结果：",
            "结果：",
            "Answer:",
            "回复：",
            "回答：",
            "```json",
            "```"
        };
        
        for (String prefix : prefixes) {
            if (cleaned.startsWith(prefix)) {
                cleaned = cleaned.substring(prefix.length()).trim();
            }
        }
        
        return cleaned.trim();
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
            if (!(nameCondition instanceof Map<?, ?>)) {
                throw new LLMOutputParseException("名称模式匹配查询需要name条件为Map类型");
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> nameMap = (Map<String, Object>) nameCondition;
            Object patternObj = nameMap.get("pattern");
            String pattern = null;
            if (patternObj instanceof String) {
                pattern = (String) patternObj;
            }
            Object modeObj = nameMap.get("mode");
            String mode = modeObj instanceof String ? (String) modeObj : "contains";
            
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
    
    /**
     * 映射排序字段
     * 将LLM生成的字段名映射为数据库中的实际字段名
     * @param sortField 原始排序字段
     * @return 映射后的排序字段
     */
    private String mapSortField(String sortField) {
        if (sortField == null || sortField.trim().isEmpty()) {
            return "retail_price ASC";
        }
        
        String[] parts = sortField.split("\\s+");
        String field = parts[0];
        String direction = parts.length > 1 ? parts[1] : "ASC";
        
        // 字段映射
        switch (field.toLowerCase()) {
            case "price":
                field = "retail_price";
                break;
            case "name":
                field = "name";
                break;
            case "stock":
                field = "number";
                break;
            case "sales":
                field = "sales";
                break;
            default:
                // 如果字段不在映射列表中，使用原字段
                break;
        }
        
        return field + " " + direction;
    }
}