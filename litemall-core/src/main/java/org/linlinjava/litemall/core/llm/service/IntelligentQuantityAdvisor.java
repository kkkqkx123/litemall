package org.linlinjava.litemall.core.llm.service;

import org.linlinjava.litemall.core.llm.model.QuantitySuggestion;
import java.util.Map;

/**
 * 智能数量建议器
 * 根据查询类型和问题内容提供合适的商品数量建议
 * 采用启发式规则 + LLM智能覆盖的策略
 */
public class IntelligentQuantityAdvisor {
    
    // 基础数量建议（基于查询类型）
    private static final int SPECIFIC_PRODUCT_COUNT = 5;      // 特定商品查询
    private static final int PRICE_RANGE_COUNT = 20;        // 价格范围查询
    private static final int RECOMMENDATION_COUNT = 30;     // 推荐类查询
    private static final int CATEGORY_BROWSE_COUNT = 50;    // 类目浏览
    private static final int STATISTICAL_COUNT = 100;       // 统计查询
    private static final int KEYWORD_SEARCH_COUNT = 25;     // 关键词搜索
    private static final int DEFAULT_COUNT = 15;           // 默认数量
    
    /**
     * 根据查询类型、问题内容和上下文建议合适的商品数量
     * @param queryType 查询类型
     * @param question 用户问题
     * @param context 上下文信息（包含会话信息等）
     * @param llmService LLM服务，用于智能决策
     * @return 数量建议对象，包含最终数量、原因和是否覆盖启发式建议
     */
    public static QuantitySuggestion suggestQuantity(String queryType, String question, 
                                                   Map<String, Object> context,
                                                   Qwen3Service llmService) {
        if (queryType == null || question == null) {
            return new QuantitySuggestion(DEFAULT_COUNT, "参数为空，使用默认数量", false);
        }
        
        // 1. 启发式基础建议（方案二）
        int heuristicQuantity = getHeuristicQuantity(queryType);
        
        // 2. 根据问题内容进行启发式调整
        int adjustedHeuristicQuantity = adjustQuantityByQuestion(heuristicQuantity, question);
        
        // 3. 如果没有LLM服务，返回启发式建议
        if (llmService == null) {
            return new QuantitySuggestion(adjustedHeuristicQuantity, 
                "基于" + getQueryTypeDescription(queryType) + "的启发式建议", false);
        }
        
        // 4. 构建LLM数量决策提示词
        String prompt = buildQuantityDecisionPrompt(question, adjustedHeuristicQuantity, 
                                                 queryType, context);
        
        // 5. LLM决定是否覆盖启发式建议
        try {
            String llmResponse = llmService.callLLM(prompt);
            LLMQuantityDecision decision = parseLLMQuantityDecision(llmResponse);
            
            if (decision != null && decision.isOverrideHeuristic()) {
                return new QuantitySuggestion(decision.getFinalQuantity(), 
                    decision.getReason(), true);
            }
        } catch (Exception e) {
            // LLM调用失败，使用启发式建议
            return new QuantitySuggestion(adjustedHeuristicQuantity, 
                "LLM决策失败，使用启发式建议: " + e.getMessage(), false);
        }
        
        // 6. 返回启发式建议
        return new QuantitySuggestion(adjustedHeuristicQuantity, 
            "基于" + getQueryTypeDescription(queryType) + "的启发式建议", false);
    }
    
    /**
     * 获取启发式数量建议
     */
    private static int getHeuristicQuantity(String queryType) {
        return getBaseQuantityByType(queryType);
    }
    
    /**
     * 根据查询类型获取基础数量
     */
    private static int getBaseQuantityByType(String queryType) {
        switch (queryType.toLowerCase()) {
            case "specific_product":
                return SPECIFIC_PRODUCT_COUNT;
            case "price_range":
                return PRICE_RANGE_COUNT;
            case "recommendation":
            case "personalized_recommendation":
                return RECOMMENDATION_COUNT;
            case "category_filter":
            case "category_browse":
                return CATEGORY_BROWSE_COUNT;
            case "statistical":
                return STATISTICAL_COUNT;
            case "keyword_search":
                return KEYWORD_SEARCH_COUNT;
            default:
                return DEFAULT_COUNT;
        }
    }
    
    /**
     * 根据问题内容调整数量
     */
    private static int adjustQuantityByQuestion(int baseQuantity, String question) {
        String lowerQuestion = question.toLowerCase();
        int adjustedQuantity = baseQuantity;
        
        // 减少数量的情况
        if (containsWords(lowerQuestion, new String[]{"最好的", "推荐", "精选", "优质", "高质量"})) {
            adjustedQuantity = Math.min(baseQuantity, 10); // 精选推荐，减少数量
        }
        if (containsWords(lowerQuestion, new String[]{"便宜的", "实惠的", "性价比"})) {
            adjustedQuantity = Math.min(baseQuantity, 15); // 性价比推荐，适中数量
        }
        if (containsWords(lowerQuestion, new String[]{"几个", "一些", "几款"})) {
            adjustedQuantity = Math.min(baseQuantity, 8); // 用户暗示少量选择
        }
        
        // 增加数量的情况
        if (containsWords(lowerQuestion, new String[]{"所有", "全部", "列表", "浏览", "看看"})) {
            adjustedQuantity = Math.max(baseQuantity, 30); // 用户想看更多选择
        }
        if (containsWords(lowerQuestion, new String[]{"多", "很多", "大量", "丰富"})) {
            adjustedQuantity = Math.max(baseQuantity, 40); // 用户暗示需要更多选择
        }
        if (containsWords(lowerQuestion, new String[]{"比较", "对比", "挑选", "选择"})) {
            adjustedQuantity = Math.max(baseQuantity, 20); // 用户需要对比选择
        }
        
        // 特殊关键词处理
        if (containsWords(lowerQuestion, new String[]{"送礼", "礼物", "礼品"})) {
            adjustedQuantity = Math.min(baseQuantity, 12); // 送礼推荐，精选少量
        }
        if (containsWords(lowerQuestion, new String[]{"爆款", "热门", "畅销", "流行"})) {
            adjustedQuantity = Math.min(baseQuantity, 15); // 热门推荐，适中数量
        }
        
        return adjustedQuantity;
    }
    
    /**
     * 检查问题中是否包含指定词汇
     */
    private static boolean containsWords(String question, String[] words) {
        for (String word : words) {
            if (question.contains(word)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 获取查询类型的描述
     */
    public static String getQueryTypeDescription(String queryType) {
        switch (queryType.toLowerCase()) {
            case "specific_product":
                return "特定商品查询";
            case "price_range":
                return "价格范围查询";
            case "recommendation":
            case "personalized_recommendation":
                return "个性化推荐";
            case "category_filter":
            case "category_browse":
                return "类目浏览";
            case "statistical":
                return "统计查询";
            case "keyword_search":
                return "关键词搜索";
            default:
                return "通用查询";
        }
    }
    
    /**
     * 获取数量建议的详细说明
     */
    public static String getQuantityExplanation(int quantity, String queryType) {
        StringBuilder explanation = new StringBuilder();
        explanation.append("基于").append(getQueryTypeDescription(queryType));
        explanation.append("，建议显示").append(quantity).append("个商品");
        
        if (quantity <= 5) {
            explanation.append("（精选推荐）");
        } else if (quantity <= 15) {
            explanation.append("（适中选择）");
        } else if (quantity <= 30) {
            explanation.append("（丰富选择）");
        } else {
            explanation.append("（全面展示）");
        }
        
        return explanation.toString();
    }
    
    /**
     * 构建LLM数量决策提示词
     */
    private static String buildQuantityDecisionPrompt(String question, int heuristicQuantity, 
                                                    String queryType, Map<String, Object> context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("基于用户问题和以下信息，决定合适的查询数量：\n\n");
        prompt.append("用户问题：").append(question).append("\n");
        prompt.append("启发式建议数量：").append(heuristicQuantity).append("\n");
        prompt.append("查询类型：").append(getQueryTypeDescription(queryType)).append("\n");
        
        if (context != null && !context.isEmpty()) {
            prompt.append("用户偏好：").append(context).append("\n");
        }
        
        prompt.append("\n请分析：\n");
        prompt.append("1. 启发式建议的数量是否合适？\n");
        prompt.append("2. 用户的具体需求是否需要调整数量？\n");
        prompt.append("3. 基于上下文，应该返回更多还是更少的结果？\n\n");
        prompt.append("输出JSON格式：\n");
        prompt.append("{\n");
        prompt.append("  \"finalQuantity\": 数量,\n");
        prompt.append("  \"overrideHeuristic\": true/false,\n");
        prompt.append("  \"reason\": \"调整原因说明\"\n");
        prompt.append("}\n");
        
        return prompt.toString();
    }
    
    /**
     * 解析LLM数量决策响应
     */
    private static LLMQuantityDecision parseLLMQuantityDecision(String llmResponse) {
        if (llmResponse == null || llmResponse.trim().isEmpty()) {
            return null;
        }
        
        try {
            // 提取JSON内容
            String jsonContent = extractJSONFromResponse(llmResponse);
            if (jsonContent == null) {
                return null;
            }
            
            // 简单解析JSON（这里可以使用更完善的JSON解析器）
            int finalQuantity = extractIntFromJSON(jsonContent, "finalQuantity");
            boolean overrideHeuristic = extractBooleanFromJSON(jsonContent, "overrideHeuristic");
            String reason = extractStringFromJSON(jsonContent, "reason");
            
            if (finalQuantity > 0 && finalQuantity <= 1000) {
                return new LLMQuantityDecision(finalQuantity, overrideHeuristic, reason);
            }
            
        } catch (Exception e) {
            // 解析失败，返回null
        }
        
        return null;
    }
    
    /**
     * 从响应中提取JSON内容
     */
    private static String extractJSONFromResponse(String response) {
        int startIndex = response.indexOf('{');
        int endIndex = response.lastIndexOf('}');
        
        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            return response.substring(startIndex, endIndex + 1);
        }
        
        return null;
    }
    
    /**
     * 从JSON中提取整数值
     */
    private static int extractIntFromJSON(String json, String key) {
        String keyPattern = "\"" + key + "\":";
        int keyIndex = json.indexOf(keyPattern);
        
        if (keyIndex != -1) {
            int valueStart = keyIndex + keyPattern.length();
            int valueEnd = json.indexOf(",", valueStart);
            if (valueEnd == -1) {
                valueEnd = json.indexOf("}", valueStart);
            }
            
            if (valueEnd != -1) {
                try {
                    String valueStr = json.substring(valueStart, valueEnd).trim();
                    return Integer.parseInt(valueStr.replaceAll("[^0-9]", ""));
                } catch (NumberFormatException e) {
                    // 解析失败
                }
            }
        }
        
        return -1;
    }
    
    /**
     * 从JSON中提取布尔值
     */
    private static boolean extractBooleanFromJSON(String json, String key) {
        String keyPattern = "\"" + key + "\":";
        int keyIndex = json.indexOf(keyPattern);
        
        if (keyIndex != -1) {
            int valueStart = keyIndex + keyPattern.length();
            int valueEnd = json.indexOf(",", valueStart);
            if (valueEnd == -1) {
                valueEnd = json.indexOf("}", valueStart);
            }
            
            if (valueEnd != -1) {
                String valueStr = json.substring(valueStart, valueEnd).trim().toLowerCase();
                return "true".equals(valueStr);
            }
        }
        
        return false;
    }
    
    /**
     * 从JSON中提取字符串值
     */
    private static String extractStringFromJSON(String json, String key) {
        String keyPattern = "\"" + key + "\":";
        int keyIndex = json.indexOf(keyPattern);
        
        if (keyIndex != -1) {
            int valueStart = keyIndex + keyPattern.length();
            int quoteStart = json.indexOf("\"", valueStart);
            if (quoteStart != -1) {
                int quoteEnd = json.indexOf("\"", quoteStart + 1);
                if (quoteEnd != -1) {
                    return json.substring(quoteStart + 1, quoteEnd);
                }
            }
        }
        
        return null;
    }
    
    /**
     * LLM数量决策内部类
     */
    private static class LLMQuantityDecision {
        private final int finalQuantity;
        private final boolean overrideHeuristic;
        private final String reason;
        
        public LLMQuantityDecision(int finalQuantity, boolean overrideHeuristic, String reason) {
            this.finalQuantity = finalQuantity;
            this.overrideHeuristic = overrideHeuristic;
            this.reason = reason;
        }
        
        public int getFinalQuantity() {
            return finalQuantity;
        }
        
        public boolean isOverrideHeuristic() {
            return overrideHeuristic;
        }
        
        public String getReason() {
            return reason;
        }
    }
}