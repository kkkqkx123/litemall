package org.linlinjava.litemall.core.llm.service;

/**
 * 智能数量建议器
 * 根据查询类型和问题内容提供合适的商品数量建议
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
     * 根据查询类型和问题内容建议合适的商品数量
     * @param queryType 查询类型
     * @param question 用户问题
     * @return 建议的商品数量
     */
    public static int suggestQuantity(String queryType, String question) {
        if (queryType == null || question == null) {
            return DEFAULT_COUNT;
        }
        
        // 基础数量（基于查询类型）
        int baseQuantity = getBaseQuantityByType(queryType);
        
        // 根据问题内容进行调整
        int adjustedQuantity = adjustQuantityByQuestion(baseQuantity, question);
        
        return adjustedQuantity;
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
}