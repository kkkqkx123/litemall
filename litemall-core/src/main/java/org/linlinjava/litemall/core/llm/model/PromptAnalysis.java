package org.linlinjava.litemall.core.llm.model;

import java.util.Map;

/**
 * 提示词分析模型
 * 用于表示LLM对用户问题的分析结果
 */
public class PromptAnalysis {
    
    /**
     * 原始用户问题
     */
    private String originalQuestion;
    
    /**
     * 标准化后的问题
     * 经过预处理和清洗的问题文本
     */
    private String normalizedQuestion;
    
    /**
     * 问题类型
     * - product_inquiry: 商品咨询
     * - price_query: 价格查询
     * - stock_query: 库存查询
     * - recommendation: 商品推荐
     * - comparison: 商品对比
     * - general: 一般性问题
     */
    private String questionType;
    
    /**
     * 提取的关键词
     * 从用户问题中提取的重要词汇
     */
    private String[] keywords;
    
    /**
     * 识别的实体
     * 如商品名称、品牌、类别等
     */
    private Map<String, String> entities;
    
    /**
     * 用户意图
     * 用户想要达到的目的
     */
    private String intent;
    
    /**
     * 情感倾向
     * - positive: 积极
     * - negative: 消极
     * - neutral: 中性
     */
    private String sentiment;
    
    /**
     * 置信度
     * 分析结果的置信度，0-1之间
     */
    private double confidence;
    
    /**
     * 是否需要多轮对话
     * 根据问题的复杂程度判断
     */
    private boolean requiresConversation;
    
    /**
     * 建议的提示词模板
     * 用于生成LLM提示词
     */
    private String suggestedPromptTemplate;
    
    /**
     * 默认构造函数
     */
    public PromptAnalysis() {
        this.confidence = 0.0;
        this.requiresConversation = false;
    }
    
    /**
     * 带参数的构造函数
     * @param originalQuestion 原始问题
     */
    public PromptAnalysis(String originalQuestion) {
        this();
        this.originalQuestion = originalQuestion;
        this.normalizedQuestion = originalQuestion;
    }
    
    /**
     * 判断是否可信
     * @return true表示分析结果可信，false表示不可信
     */
    public boolean isReliable() {
        return confidence >= 0.7; // 置信度阈值设为0.7
    }
    
    /**
     * 获取实体值
     * @param entityType 实体类型
     * @return 实体值，如果不存在返回null
     */
    public String getEntity(String entityType) {
        return entities != null ? entities.get(entityType) : null;
    }
    
    /**
     * 检查是否包含指定实体
     * @param entityType 实体类型
     * @return true表示包含，false表示不包含
     */
    public boolean hasEntity(String entityType) {
        return entities != null && entities.containsKey(entityType);
    }
    
    /**
     * 获取关键词字符串（逗号分隔）
     * @return 关键词字符串
     */
    public String getKeywordsString() {
        if (keywords == null || keywords.length == 0) {
            return "";
        }
        return String.join(", ", keywords);
    }
    
    /**
     * 生成分析摘要
     * @return 分析摘要
     */
    public String getAnalysisSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("问题类型: ").append(questionType != null ? questionType : "未知").append("\n");
        summary.append("用户意图: ").append(intent != null ? intent : "未知").append("\n");
        summary.append("情感倾向: ").append(sentiment != null ? sentiment : "未知").append("\n");
        summary.append("置信度: ").append(String.format("%.2f", confidence)).append("\n");
        summary.append("关键词: ").append(getKeywordsString()).append("\n");
        
        if (entities != null && !entities.isEmpty()) {
            summary.append("识别的实体:\n");
            for (Map.Entry<String, String> entry : entities.entrySet()) {
                summary.append("  - ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
        }
        
        return summary.toString();
    }
    
    // Getters and Setters
    public String getOriginalQuestion() {
        return originalQuestion;
    }
    
    public void setOriginalQuestion(String originalQuestion) {
        this.originalQuestion = originalQuestion;
    }
    
    public String getNormalizedQuestion() {
        return normalizedQuestion;
    }
    
    public void setNormalizedQuestion(String normalizedQuestion) {
        this.normalizedQuestion = normalizedQuestion;
    }
    
    public String getQuestionType() {
        return questionType;
    }
    
    public void setQuestionType(String questionType) {
        this.questionType = questionType;
    }
    
    public String[] getKeywords() {
        return keywords;
    }
    
    public void setKeywords(String[] keywords) {
        this.keywords = keywords;
    }
    
    public Map<String, String> getEntities() {
        return entities;
    }
    
    public void setEntities(Map<String, String> entities) {
        this.entities = entities;
    }
    
    public String getIntent() {
        return intent;
    }
    
    public void setIntent(String intent) {
        this.intent = intent;
    }
    
    public String getSentiment() {
        return sentiment;
    }
    
    public void setSentiment(String sentiment) {
        this.sentiment = sentiment;
    }
    
    public double getConfidence() {
        return confidence;
    }
    
    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }
    
    public boolean isRequiresConversation() {
        return requiresConversation;
    }
    
    public void setRequiresConversation(boolean requiresConversation) {
        this.requiresConversation = requiresConversation;
    }
    
    public String getSuggestedPromptTemplate() {
        return suggestedPromptTemplate;
    }
    
    public void setSuggestedPromptTemplate(String suggestedPromptTemplate) {
        this.suggestedPromptTemplate = suggestedPromptTemplate;
    }
    
    @Override
    public String toString() {
        return "PromptAnalysis{" +
                "originalQuestion='" + originalQuestion + '\'' +
                ", questionType='" + questionType + '\'' +
                ", keywords=" + getKeywordsString() +
                ", intent='" + intent + '\'' +
                ", sentiment='" + sentiment + '\'' +
                ", confidence=" + confidence +
                ", requiresConversation=" + requiresConversation +
                '}';
    }
}